package net.sf.l2j.gameserver.engine.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Treasure extends Event
{
	public EventState eventState;
	public List<Spawn> chests = new CopyOnWriteArrayList<>();
	public Player finder;
	private Core task = new Core();
	private String current;
	private int number;
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
						teleportToTeamPos();
						forceSitAll();
						unequip();
						setStatus(EventState.FIGHT);
						schedule(30000);
						break;
					
					case FIGHT:
						forceStandAll();
						sendMsg();
						spawnAtRandom();
						setStatus(EventState.END);
						clock.startClock(getInt("matchTime"));
						break;
					
					case END:
						clock.setTime(0);
						unSpawnChests();
						
						if (finder == null)
							EventManager.getInstance().end("Event ended in a tie, nobody has found any treasure.");
						else
						{
							giveReward(finder, getInt("rewardId"), getInt("rewardAmmount"));
							setStatus(EventState.INACTIVE);
							EventManager.getInstance().end("Congratulation! " + finder.getName() + " was the first to find the treasure!");
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
	
	public Treasure()
	{
		super();
		eventId = 18;
		createNewTeam(1, "All", getColor("All"), getPosition("All", 1));
	}
	
	@Override
	public void endEvent()
	{
		setStatus(EventState.END);
		clock.setTime(0);
	}
	
	@Override
	public boolean onTalkNpc(Npc npc, Player player)
	{
		if (npc.getNpcId() != getInt("chestNpcId"))
			return false;
		
		npc.doDie(player);
		increasePlayersScore(player);
		player.addItem("Treasure", getInt("rewardId"), 1, player, true);
		if (getBoolean("rewardOnOpen"))
		{
			int rnd = Rnd.get(100);
			if (rnd < ITEMS.length - 1)
				player.addItem("Treasure", ITEMS[rnd], 1, player, true);
		}
		
		npc.deleteMe();
		npc.getSpawn().setRespawnState(false);
		SpawnTable.getInstance().deleteSpawn(npc.getSpawn(), true);
		chests.remove(npc.getSpawn());
		current = player.getName();
		
		if (finder == null)
			finder = player;
		
		number++;
		counter = 3;
		sendMsg();
		
		if (chests.size() == 0)
			clock.setTime(0);
		
		return true;
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
	public boolean onUseMagic(L2Skill skill)
	{
		return false;
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
		number = 0;
		counter = 0;
		current = null;
		finder = null;
		setStatus(EventState.START);
		schedule(1);
	}
	
	public void unSpawnChests()
	{
		for (Spawn s : chests)
		{
			if (s == null)
			{
				chests.remove(s);
				continue;
			}
			
			s.getNpc().deleteMe();
			s.setRespawnState(false);
			SpawnTable.getInstance().deleteSpawn(s, true);
			chests.remove(s);
		}
	}
	
	@Override
	public boolean onUseItem(Player player, ItemInstance item)
	{
		return false;
	}
	
	@Override
	public boolean canAttack(Player player, WorldObject target)
	{
		return false;
	}
	
	public void spawnAtRandom()
	{
		for (int i = 0; i < getInt("numberOfChests"); i++)
		{
			int[] coor = getPosition("Chests", 0);
			chests.add(spawnNPC(coor[0], coor[1], coor[2], getInt("chestNpcId")));
		}
	}
	
	@Override
	public String getStartingMsg()
	{
		if (eventState == EventState.FIGHT)
			return "Go find the treasures!";
		return current + " has found the treasure number " + number + "!";
	}
	
	@Override
	public String getScorebar()
	{
		if (counter == 0)
			return "Time: " + clock.getTime() + "";
		
		counter--;
		return "";
	}
}