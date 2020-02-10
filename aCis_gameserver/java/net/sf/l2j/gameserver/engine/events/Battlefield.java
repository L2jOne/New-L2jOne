package net.sf.l2j.gameserver.engine.events;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.engine.EventTeam;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.AbstractNpcInfo.NpcInfo;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Battlefield extends Event
{
	public EventState eventState;
	public int winnerTeam;
	private Core task = new Core();
	private Map<Integer, Spawn> bases = new ConcurrentHashMap<>();
	private Map<Integer, Integer> owners = new ConcurrentHashMap<>();
	
	private enum EventState
	{
		START,
		FIGHT,
		END,
		TELEPORT,
		INACTIVE
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
						spawnBases();
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
						unspawnBases();
						setStatus(EventState.INACTIVE);
						
						if (winnerTeam == 0)
							EventManager.getInstance().end("The event ended in a tie! both teams had " + teams.get(1).getScore() + " points!");
						else
						{
							giveReward(getPlayersOfTeam(winnerTeam), getInt("rewardId"), getInt("rewardAmmount"));
							EventManager.getInstance().end("Congratulation! The " + teams.get(winnerTeam).getName() + " team won the event with " + teams.get(winnerTeam).getScore() + " points!");
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
	
	public Battlefield()
	{
		super();
		eventId = 14;
		createNewTeam(1, "Blue", getColor("Blue"), getPosition("Blue", 1));
		createNewTeam(2, "Red", getColor("Red"), getPosition("Red", 1));
	}
	
	@Override
	public void endEvent()
	{
		winnerTeam = getWinnerTeam();
		
		setStatus(EventState.END);
		schedule(1);
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
	
	@Override
	public void onDie(Player victim, Creature killer)
	{
		super.onDie(victim, killer);
		addToResurrector(victim);
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
		sb.append("<html><body><table width=300><tr><td><center>Event phase</td></tr><tr><td><center>" + getString("eventName") + " - " + clock.getTime() + "</td></tr><tr><td><center><font color=" + teams.get(1).getHexaColor() + ">" + teams.get(1).getScore() + "</font> - <font color=" + teams.get(2).getHexaColor() + ">" + teams.get(2).getScore() + "</font></td></tr></table><br><table width=300>");
		
		int i = 0;
		for (EventTeam team : teams.values())
		{
			i++;
			sb.append("<tr><td><font color=" + team.getHexaColor() + ">" + team.getName() + "</font> team</td><td></td><td></td><td></td></tr>");
			for (Player p : getPlayersOfTeam(i))
				sb.append("<tr><td>" + p.getName() + "</td><td>lvl " + p.getLevel() + "</td><td>" + p.getTemplate().getClassName() + "</td><td>" + getScore(p) + "</td></tr>");
		}
		
		sb.append("</table><br></body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public void start()
	{
		setStatus(EventState.START);
		schedule(1);
	}
	
	public void spawnBases()
	{
		for (int i = 1; i <= getInt("numOfBases"); i++)
		{
			bases.put(i, spawnNPC(getPosition("Base", i)[0], getPosition("Base", i)[1], getPosition("Base", i)[2], getInt("baseNpcId")));
			bases.get(i).getNpc().setTitle("- Neutral -");
			owners.put(i, 0);
		}
	}
	
	public void unspawnBases()
	{
		for (Spawn base : bases.values())
			unspawnNPC(base);
	}
	
	@Override
	public void reset()
	{
		super.reset();
		bases.clear();
		owners.clear();
	}
	
	@Override
	public void clockTick()
	{
		for (int owner : owners.values())
			if (owner != 0)
				teams.get(owner).increaseScore(1);
	}
	
	@Override
	public void useCapture(Player player, Npc base)
	{
		if (base.getNpcId() != getInt("baseNpcId"))
			return;
		
		for (Map.Entry<Integer, Spawn> baseSpawn : bases.entrySet())
		{
			if (baseSpawn.getValue().getNpc().getObjectId() == base.getObjectId())
			{
				if (owners.get(baseSpawn.getKey()) == getTeam(player))
					return;
				
				owners.put(baseSpawn.getKey(), baseSpawn.getKey());
				baseSpawn.getValue().getNpc().setTitle("- " + teams.get(getTeam(player)).getName() + " -");
				for (Player p : getPlayerList())
					p.sendPacket(new NpcInfo(baseSpawn.getValue().getNpc(), p));
				
				announce(getPlayerList(), "The " + teams.get(getTeam(player)).getName() + " team captured a base!");
				increasePlayersScore(player);
			}
		}
	}
	
	@Override
	public String getStartingMsg()
	{
		return "Capture the flags by using the Capture skill on them!";
	}
	
	@Override
	public String getScorebar()
	{
		return teams.get(1).getName() + ": " + teams.get(1).getScore() + "  " + teams.get(2).getName() + ": " + teams.get(2).getScore() + "  Time: " + clock.getTime();
	}
	
	public void removeSkill()
	{
		for (Player player : getPlayerList())
			player.removeSkill(getInt("captureSkillId"), false);
	}
	
	public void giveSkill()
	{
		for (Player player : getPlayerList())
		{
			player.addSkill(SkillTable.getInstance().getInfo(getInt("captureSkillId"), 1), false);
			player.sendSkillList();
		}
	}
}