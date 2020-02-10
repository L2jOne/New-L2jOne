package net.sf.l2j.gameserver.engine.events;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class Simon extends Event
{
	public EventState eventState;
	public int round;
	public String say;
	public Player winner;
	private Core task = new Core();
	private Npc npc;
	private Spawn spawn;
	private CreatureSay message;
	
	private enum EventState
	{
		START,
		SAY,
		CHECK,
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
						setStatus(EventState.SAY);
						schedule(30000);
						break;
					
					case SAY:
						if (round == 0)
							sendMsg();
						
						round++;
						say = createNewRandomString(getInt("lengthOfFirstWord") + getInt("increasePerRound") * round);
						sendToPlayers(say.toUpperCase());
						setStatus(EventState.CHECK);
						schedule(getInt("roundTime") * 1000);
						break;
					
					case CHECK:
						if (removeAfkers())
						{
							setAllToFalse();
							setStatus(EventState.SAY);
							schedule(getInt("roundTime") * 1000);
						}
						break;
					
					case END:
						setStatus(EventState.INACTIVE);
						forceStandAll();
						
						if (winner != null)
						{
							giveReward(winner, getInt("rewardId"), getInt("rewardAmmount"));
							EventManager.getInstance().end("Congratulation! " + winner.getName() + " won the event!");
						}
						else
							EventManager.getInstance().end("The event ended in a tie!");
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
	
	public Simon()
	{
		super();
		eventId = 6;
		createNewTeam(1, "All", getColor("All"), getPosition("All", 1));
	}
	
	public static String createNewRandomString(int size)
	{
		String str = "";
		
		for (int i = 0; i < size; i++)
			str += (char) (Rnd.get(26) + 97);
		
		return str;
	}
	
	@Override
	public void endEvent()
	{
		winner = getPlayerWithMaxScore();
		setStatus(EventState.END);
		schedule(1);
	}
	
	@Override
	public boolean onSay(SayType type, Player player, String text)
	{
		if (eventState == EventState.CHECK && getStatus(player) != -1)
		{
			if (text.equalsIgnoreCase(say))
			{
				setStatus(player, 1);
				player.sendMessage("Correct!");
				increasePlayersScore(player);
				player.getAppearance().setNameColor(0, 255, 0);
				player.broadcastUserInfo();
			}
			else
			{
				setStatus(player, -1);
				player.sendMessage("Wrong!");
				player.getAppearance().setNameColor(255, 0, 0);
				player.broadcastUserInfo();
			}
			
			int falses = 0;
			Player falsed = null;
			for (Player p : getPlayerList())
			{
				if (getStatus(p) == 0)
				{
					falses++;
					falsed = p;
				}
			}
			
			if (falses == 1)
			{
				int count = 0;
				for (Player pla : getPlayerList())
					if (getStatus(pla) == 1)
						count++;
				
				if (count >= 1 && falsed != null)
				{
					falsed.sendMessage("Last one!");
					falsed.getAppearance().setNameColor(255, 0, 0);
					falsed.broadcastUserInfo();
					setStatus(falsed, -1);
				}
				
				if (count == 0)
				{
					winner = getPlayerWithMaxScore();
					setStatus(EventState.END);
					schedule(1);
				}
			}
			
			if (countOfPositiveStatus() == 1)
			{
				winner = getPlayerWithMaxScore();
				setStatus(EventState.END);
				schedule(1);
			}
			
			return true;
		}
		
		return false;
	}
	
	public boolean removeAfkers()
	{
		for (Player player : getPlayerList())
		{
			if (getStatus(player) == 0)
			{
				player.sendMessage("Timeout!");
				player.getAppearance().setNameColor(255, 0, 0);
				player.broadcastUserInfo();
				setStatus(player, -1);
			}
			
			if (countOfPositiveStatus() == 1)
			{
				if (getPlayersWithStatus(1).size() == 1)
					winner = getPlayerWithMaxScore();
				else
					winner = null;
				
				setStatus(EventState.END);
				schedule(1);
				return false;
			}
		}
		return true;
	}
	
	@Override
	public void reset()
	{
		super.reset();
		round = 0;
		players.clear();
		say = "";
		npc.deleteMe();
		spawn.setRespawnState(false);
		SpawnTable.getInstance().deleteSpawn(spawn, true);
		npc = null;
		spawn = null;
	}
	
	@Override
	public void schedule(int time)
	{
		ThreadPool.schedule(task, time);
	}
	
	public void sendToPlayers(String text)
	{
		message = new CreatureSay(npc.getObjectId(), SayType.HERO_VOICE, "Simon", text);
		for (Player player : getPlayerList())
			player.sendPacket(message);
	}
	
	public void setAllToFalse()
	{
		for (Player player : getPlayerList())
		{
			if (getStatus(player) != -1)
			{
				setStatus(player, 0);
				player.getAppearance().setNameColor(255, 255, 255);
				player.broadcastUserInfo();
			}
		}
	}
	
	private int getActivePlayerCount()
	{
		int c = 0;
		for (Player player : getPlayerList())
			if (getStatus(player) != -1)
				c++;
		
		return c;
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
		sb.append("<html><body><table width=270><tr><td width=200>Event Engine </td><td><a action=\"bypass -h eventstats 1\">Statistics</a></td></tr></table><br><center><table width=270 bgcolor=5A5A5A><tr><td width=70>Running</td><td width=130><center>" + getString("eventName") + "</td><td width=70>Time: ?</td></tr></table><table width=270><tr><td><center>Players left: " + getPlayersWithStatus(0).size() + "</td></tr></table><br><table width=270>");
		
		for (Player p : getPlayersOfTeam(1))
			sb.append("<tr><td>" + p.getName() + "</td><td>lvl " + p.getLevel() + "</td><td>" + p.getTemplate().getClassName() + "</td><td>" + getScore(p) + "</td></tr>");
		
		sb.append("</table></body></html>");
		html.setHtml(sb.toString());
		player.sendPacket(html);
	}
	
	@Override
	public void start()
	{
		int[] npcpos = getPosition("Simon", 1);
		spawn = spawnNPC(npcpos[0], npcpos[1], npcpos[2], getInt("simonNpcId"));
		npc = spawn.getNpc();
		setStatus(EventState.START);
		schedule(1);
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
	
	@Override
	public String getStartingMsg()
	{
		return "Say excactly as NPC says as fast as possible!";
	}
	
	@Override
	public String getScorebar()
	{
		return "Players: " + getActivePlayerCount();
	}
}