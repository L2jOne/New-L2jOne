package net.sf.l2j.gameserver.engine.events;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.engine.EventStats;
import net.sf.l2j.gameserver.engine.EventTeam;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillLaunched;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Bomb extends Event
{
	public Map<Spawn, Player> bombs = new ConcurrentHashMap<>();
	public EventState eventState;
	private Core task = new Core();
	
	private enum EventState
	{
		START,
		FIGHT,
		END,
		TELEPORT,
		INACTIVE
	}
	
	public class Bomber implements Runnable
	{
		private Spawn _bomb;
		
		public Bomber(Spawn bomb)
		{
			_bomb = bomb;
		}
		
		@Override
		public void run()
		{
			explode(_bomb);
			bombs.remove(_bomb);
		}
	}
	
	public class Core implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				switch (eventState)
				{
					case START:
						divideIntoTeams(2);
						preparePlayers();
						teleportToTeamPos();
						createPartyOfTeam(1);
						createPartyOfTeam(2);
						forceSitAll();
						unequip();
						giveSkill();
						setStatus(EventState.FIGHT);
						schedule(20000);
						break;
					
					case FIGHT:
						forceStandAll();
						sendMsg();
						setStatus(EventState.END);
						clock.startClock(getInt("matchTime"));
						break;
					
					case END:
						clock.setTime(0);
						if (winnerTeam == 0)
							winnerTeam = getWinnerTeam();
						
						removeSkill();
						setStatus(EventState.INACTIVE);
						
						if (winnerTeam == 0)
							EventManager.getInstance().end("The event ended in a tie! both teams had " + teams.get(1).getScore() + " kills!");
						else
						{
							giveReward(getPlayersOfTeam(winnerTeam), getInt("rewardId"), getInt("rewardAmmount"));
							EventManager.getInstance().end("Congratulation! The " + teams.get(winnerTeam).getName() + " team won the event with " + teams.get(winnerTeam).getScore() + " kills!");
						}
						break;
				}
			}
			catch (Throwable e)
			{
				e.printStackTrace();
				EventManager.getInstance().end("Error! Event ended.");
			}
		}
	}
	
	public Bomb()
	{
		super();
		eventId = 12;
		createNewTeam(1, "Blue", getColor("Blue"), getPosition("Blue", 1));
		createNewTeam(2, "Red", getColor("Red"), getPosition("Red", 1));
	}
	
	@Override
	public void dropBomb(Player player)
	{
		Spawn bomb = spawnNPC(player.getX(), player.getY(), player.getZ(), getInt("bombNpcId"));
		bombs.put(bomb, player);
		
		bomb.getNpc().setTitle((getTeam(player) == 1 ? "Blue" : "Red"));
		bomb.getNpc().broadcastStatusUpdate();
		
		for (Player p : getPlayerList())
			p.sendPacket(new NpcInfo(bomb.getNpc(), p));
		
		ThreadPool.schedule(new Bomber(bomb), 3000);
	}
	
	@Override
	public void endEvent()
	{
		winnerTeam = getWinnerTeam();
		
		setStatus(EventState.END);
		clock.setTime(0);
	}
	
	public void explode(Spawn bomb)
	{
		List<Creature> victims = new CopyOnWriteArrayList<>();
		
		for (Player player : getPlayerList())
		{
			if (player == null)
				continue;
			
			if (player.isInvul())
				continue;
			
			if (getTeam(bombs.get(bomb)) != getTeam(player) && Math.sqrt(player.getPlanDistanceSq(bomb.getNpc().getX(), bomb.getNpc().getY())) <= getInt("bombRadius"))
			{
				player.doDie(bomb.getNpc());
				increasePlayersScore(bombs.get(bomb));
				EventStats.getInstance().tempTable.get(player.getObjectId())[2] = EventStats.getInstance().tempTable.get(player.getObjectId())[2] + 1;
				addToResurrector(player);
				
				victims.add(player);
				
				if (getTeam(player) == 1)
					teams.get(2).increaseScore();
				if (getTeam(player) == 2)
					teams.get(1).increaseScore();
			}
			if (victims.size() != 0)
			{
				bomb.getNpc().broadcastPacket(new MagicSkillUse(bomb.getNpc(), victims.get(0), 18, 1, 0, 0));
				bomb.getNpc().broadcastPacket(new MagicSkillLaunched(bomb.getNpc(), 18, 1, victims.toArray(new WorldObject[victims.size()])));
				victims.clear();
			}
		}
		unspawnNPC(bomb);
	}
	
	@Override
	public int getWinnerTeam()
	{
		if (teams.get(1).getScore() > teams.get(2).getScore())
			return 1;
		if (teams.get(2).getScore() > teams.get(1).getScore())
			return 2;
		return 0;
	}
	
	public void giveSkill()
	{
		for (Player player : getPlayerList())
			player.addSkill(SkillTable.getInstance().getInfo(getInt("bombSkillId"), 1), false);
	}
	
	@Override
	public void onLogout(Player player)
	{
		player.removeSkill(getInt("bombSkillId"), false);
	}
	
	@Override
	public boolean onUseMagic(L2Skill skill)
	{
		if (skill.getId() == getInt("bombSkillId"))
			return true;
		
		return false;
	}
	
	public void removeSkill()
	{
		for (Player player : getPlayerList())
			player.removeSkill(getInt("bombSkillId"), false);
	}
	
	@Override
	public void schedule(int time)
	{
		ThreadPool.schedule(task, time);
	}
	
	public void setStatus(EventState s)
	{
		eventState = s;
	}
	
	@Override
	public void showHtml(Player player, int obj)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(obj);
		StringBuilder sb = new StringBuilder();
		sb.append("<html><body><table width=270><tr><td width=200>Event Engine </td><td><a action=\"bypass -h eventstats 1\">Statistics</a></td></tr></table><br><center><table width=270 bgcolor=5A5A5A><tr><td width=70>Running</td><td width=130><center>" + getString("eventName") + "</td><td width=70>Time: " + clock.getTime() + "</td></tr></table><table width=270><tr><td><center><font color=" + teams.get(1).getHexaColor() + ">" + teams.get(1).getScore() + "</font> - <font color=" + teams.get(2).getHexaColor() + ">" + teams.get(2).getScore() + "</font></td></tr></table><table width=270><tr><td><center><font color=" + teams.get(1).getHexaColor() + ">" + teams.get(1).getScore() + "</font> - <font color=" + teams.get(2).getHexaColor() + ">" + teams.get(2).getScore() + "</font></td></tr></table><br><table width=270>");
		
		int i = 0;
		for (EventTeam team : teams.values())
		{
			i++;
			sb.append("<tr><td><font color=" + team.getHexaColor() + ">" + team.getName() + "</font> team</td><td></td><td></td><td></td></tr>");
			for (Player p : getPlayersOfTeam(i))
				sb.append("<tr><td>" + p.getName() + "</td><td>lvl " + p.getLevel() + "</td><td>" + p.getTemplate().getClassName() + "</td><td>" + getScore(p) + "</td></tr>");
		}
		
		sb.append("</table></body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public void start()
	{
		setStatus(EventState.START);
		schedule(1);
	}
	
	@Override
	public boolean onUseItem(Player player, ItemInstance item)
	{
		return false;
	}
	
	@Override
	public String getStartingMsg()
	{
		return "Kill your enemies by using Bomb skill near them!";
	}
	
	@Override
	public String getScorebar()
	{
		return teams.get(1).getName() + ": " + teams.get(1).getScore() + "  " + teams.get(2).getName() + ": " + teams.get(2).getScore() + "  Time: " + clock.getTime();
	}
}