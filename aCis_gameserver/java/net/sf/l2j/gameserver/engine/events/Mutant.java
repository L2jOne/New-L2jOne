package net.sf.l2j.gameserver.engine.events;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.enums.PolyType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Mutant extends Event
{
	public EventState eventState;
	private Core task = new Core();
	private Player mutant;
	
	private enum EventState
	{
		START,
		FIGHT,
		END,
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
						divideIntoTeams(1);
						preparePlayers();
						teleportToTeamPos();
						InvisAll();
						setStatus(EventState.FIGHT);
						schedule(20000);
						break;
					
					case FIGHT:
						unInvisAll();
						sendMsg();
						transformMutant(getRandomPlayer());
						setStatus(EventState.END);
						clock.startClock(getInt("matchTime"));
						break;
					
					case END:
						clock.setTime(0);
						untransformMutant();
						Player winner = getPlayerWithMaxScore();
						giveReward(winner, getInt("rewardId"), getInt("rewardAmmount"));
						setStatus(EventState.INACTIVE);
						EventManager.getInstance().end("Congratulation! " + winner.getName() + " won the event with " + getScore(winner) + " kills!");
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
	
	public Mutant()
	{
		super();
		eventId = 13;
		createNewTeam(1, "All", getColor("All"), getPosition("All", 1));
	}
	
	@Override
	public void endEvent()
	{
		setStatus(EventState.END);
		clock.setTime(0);
	}
	
	@Override
	public void onDie(Player victim, Creature killer)
	{
		super.onDie(victim, killer);
		addToResurrector(victim);
	}
	
	@Override
	public void onKill(Creature victim, Player killer)
	{
		super.onKill(victim, killer);
		if (getStatus(killer) == 1)
			increasePlayersScore(killer);
		if (getStatus(killer) == 0 && getStatus((Player) victim) == 1)
		{
			killer.addItem("Event", getInt("rewardId"), 1, killer, true);
			transformMutant(killer);
		}
	}
	
	@Override
	public void onLogout(Player player)
	{
		super.onLogout(player);
		
		if (mutant == player)
			transformMutant(getRandomPlayer());
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
		if (players.size() > 0)
		{
			NpcHtmlMessage html = new NpcHtmlMessage(obj);
			StringBuilder sb = new StringBuilder();
			sb.append("<html><body><table width=270><tr><td width=200>Event Engine </td><td><a action=\"bypass -h eventstats 1\">Statistics</a></td></tr></table><br><center><table width=270 bgcolor=5A5A5A><tr><td width=70>Running</td><td width=130><center>" + getString("eventName") + "</td><td width=70>Time: " + clock.getTime() + "</td></tr></table><table width=270><tr><td><center>" + getPlayerWithMaxScore().getName() + " - " + getScore(getPlayerWithMaxScore()) + "</td></tr></table><br><table width=270>");
			
			for (Player p : getPlayersOfTeam(1))
				sb.append("<tr><td>" + p.getName() + "</td><td>lvl " + p.getLevel() + "</td><td>" + p.getTemplate().getClassName() + "</td><td>" + getScore(p) + "</td></tr>");
			
			sb.append("</table></body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
	}
	
	@Override
	public void start()
	{
		setStatus(EventState.START);
		schedule(1);
	}
	
	public void transformMutant(Player player)
	{
		setStatus(player, 1);
		untransformMutant();
		player.addSkill(SkillTable.getInstance().getInfo(getInt("mutantBuffId"), 1), false);
		player.getAppearance().setNameColor(255, 0, 0);
		player.broadcastUserInfo();
		player.polymorph(PolyType.NPC, 25286);
		mutant = player;
	}
	
	public void untransformMutant()
	{
		if (mutant != null)
		{
			setStatus(mutant, 0);
			mutant.removeSkill(getInt("mutantBuffId"), false);
			mutant.getAppearance().setNameColor(getColor("All")[0], getColor("All")[1], getColor("All")[2]);
			mutant.broadcastUserInfo();
			mutant.unpolymorph();
			mutant = null;
		}
	}
	
	@Override
	public boolean canAttack(Player player, WorldObject target)
	{
		if (target instanceof Player)
		{
			if (getStatus(player) == 0 && getStatus((Player) target) == 0)
				return false;
			
			return true;
		}
		
		return false;
	}
	
	@Override
	public String getStartingMsg()
	{
		return "Kill the Mutant!";
	}
	
	@Override
	public String getScorebar()
	{
		return "Max: " + getScore(getPlayerWithMaxScore()) + "  Time: " + clock.getTime() + "";
	}
}