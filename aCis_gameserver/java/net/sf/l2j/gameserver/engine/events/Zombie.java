package net.sf.l2j.gameserver.engine.events;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.enums.PolyType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Zombie extends Event
{
	public EventState eventState;
	private Core task = new Core();
	private int counter;
	
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
						teleportToRandom();
						InvisAll();
						giveBows();
						setStatus(EventState.FIGHT);
						schedule(20000);
						break;
					
					case FIGHT:
						unInvisAll();
						sendMsg();
						transform(getRandomPlayer());
						transform(getRandomPlayer());
						setStatus(EventState.END);
						clock.startClock(getInt("matchTime"));
						break;
					
					case END:
						setStatus(EventState.INACTIVE);
						clock.setTime(0);
						
						removeMisc();
						
						if (getPlayersWithStatus(0).size() != 1)
							EventManager.getInstance().end("The event ended in a tie! there are " + getPlayersWithStatus(0).size() + " humans still standing!");
						else
						{
							Player winner = getPlayerWithMaxScore();
							giveReward(winner, getInt("rewardId"), getInt("rewardAmmount"));
							EventManager.getInstance().end("Congratulation! " + winner.getName() + " won the event!");
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
	
	public Zombie()
	{
		super();
		eventId = 9;
		createNewTeam(1, "All", getColor("All"), getPosition("All", 1));
	}
	
	@Override
	public boolean canAttack(Player player, WorldObject target)
	{
		if (target instanceof Player)
			if (getStatus(player) == 1 && getStatus((Player) target) == 0 || getStatus(player) == 0 && getStatus((Player) target) == 1)
				return true;
		
		return false;
	}
	
	@Override
	public void endEvent()
	{
		setStatus(EventState.END);
		clock.setTime(0);
	}
	
	@Override
	public void onHit(Player actor, Player target)
	{
		if (eventState == EventState.END)
		{
			if (getStatus(actor) == 1 && getStatus(target) == 0)
			{
				transform(target);
				increasePlayersScore(actor);
				actor.addItem("Event", getInt("rewardId"), 1, actor, true);
				
				if (getPlayersWithStatus(0).size() == 1)
					schedule(1);
			}
			else if (getStatus(actor) == 0 && getStatus(target) == 1)
			{
				target.doDie(actor);
				increasePlayersScore(actor);
				addToResurrector(target);
			}
		}
	}
	
	@Override
	public void onLogout(Player player)
	{
		if (getStatus(player) == 1)
		{
			player.removeSkill(9008, false);
			super.onLogout(player);
			
			if (getPlayersWithStatus(1).size() == 0)
				transform(getRandomPlayer());
		}
		else
		{
			player.destroyItem("Zombies", player.getInventory().getItemByItemId(9999), player, false);
			player.destroyItem("Zombies", player.getInventory().getItemByItemId(17), player, false);
			super.onLogout(player);
		}
		
		if (getPlayersWithStatus(0).size() == 1)
			schedule(1);
	}
	
	@Override
	public boolean onUseItem(Player player, ItemInstance item)
	{
		return false;
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
		sb.append("<html><body><table width=270><tr><td width=200>Event Engine </td><td><a action=\"bypass -h eventstats 1\">Statistics</a></td></tr></table><br><center><table width=270 bgcolor=5A5A5A><tr><td width=70>Running</td><td width=130><center>" + getString("eventName") + "</td><td width=70>Time: " + clock.getTime() + "</td></tr></table><table width=270><tr><td><center>Players left: " + getPlayersWithStatus(0).size() + "</td></tr></table><br><table width=270>");
		
		for (Player p : getPlayersOfTeam(1))
			sb.append("<tr><td>" + p.getName() + "</td><td>lvl " + p.getLevel() + "</td><td>" + p.getTemplate().getClassName() + "</td><td>" + (getStatus(p) == 1 ? "Zombie" : "Human") + "</td></tr>");
		
		sb.append("</table></body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public void start()
	{
		counter = 0;
		setStatus(EventState.START);
		schedule(1);
	}
	
	@Override
	public String getStartingMsg()
	{
		return "";
	}
	
	@Override
	public String getScorebar()
	{
		if (counter == 0)
			return "Humans: " + getPlayersWithStatus(0).size() + "  Time: " + clock.getTime();
		
		counter--;
		return "";
	}
	
	public void giveBows()
	{
		for (Player player : players.keySet())
		{
			try
			{
				player.addItem("Zombies", 9999, 1, player, false);
				player.addItem("Zombies", 17, 50, player, false);
				player.getInventory().equipItem(player.getInventory().getItemByItemId(9999));
			}
			catch (Exception e)
			{
				// player disconnected
			}
		}
	}
	
	public void removeMisc()
	{
		for (Player player : getPlayersWithStatus(1))
			player.removeSkill(9008, false);
		for (Player player : getPlayersWithStatus(0))
		{
			try
			{
				player.destroyItem("Zombies", player.getInventory().getItemByItemId(9999), player, false);
				player.destroyItem("Zombies", player.getInventory().getItemByItemId(17), player, false);
			}
			catch (Exception e)
			{
				
			}
		}
	}
	
	public void teleportToRandom()
	{
		for (Player player : players.keySet())
		{
			int[] loc = getPosition("All", 0);
			player.teleportTo(loc[0], loc[1], loc[2], 0);
		}
	}
	
	public void transform(Player player)
	{
		if (player.getPolyType() != PolyType.DEFAULT)
			return;
		
		counter = 3;
		for (Player p : players.keySet())
			p.sendPacket(new ExShowScreenMessage(1, -1, 8, false, 0, 0, 0, false, 3000, true, player.getName() + " has transformed into a zombie!"));
		player.destroyItem("Zombies", player.getInventory().getItemByItemId(9999), player, false);
		player.destroyItem("Zombies", player.getInventory().getItemByItemId(17), player, false);
		setStatus(player, 1);
		player.addSkill(SkillTable.getInstance().getInfo(9008, 1), false);
		player.getAppearance().setNameColor(255, 0, 0);
		player.broadcastUserInfo();
		player.polymorph(PolyType.NPC, 25375);
	}
}