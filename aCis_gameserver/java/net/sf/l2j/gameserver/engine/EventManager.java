package net.sf.l2j.gameserver.engine;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.manager.FenceManager;
import net.sf.l2j.gameserver.data.xml.DoorData;
import net.sf.l2j.gameserver.engine.events.Battlefield;
import net.sf.l2j.gameserver.engine.events.Bomb;
import net.sf.l2j.gameserver.engine.events.CTF;
import net.sf.l2j.gameserver.engine.events.DM;
import net.sf.l2j.gameserver.engine.events.Domination;
import net.sf.l2j.gameserver.engine.events.DoubleDomination;
import net.sf.l2j.gameserver.engine.events.HG;
import net.sf.l2j.gameserver.engine.events.Korean;
import net.sf.l2j.gameserver.engine.events.LMS;
import net.sf.l2j.gameserver.engine.events.Lucky;
import net.sf.l2j.gameserver.engine.events.Mutant;
import net.sf.l2j.gameserver.engine.events.Raid;
import net.sf.l2j.gameserver.engine.events.Russian;
import net.sf.l2j.gameserver.engine.events.Simon;
import net.sf.l2j.gameserver.engine.events.Treasure;
import net.sf.l2j.gameserver.engine.events.TvT;
import net.sf.l2j.gameserver.engine.events.VIPTvT;
import net.sf.l2j.gameserver.engine.events.Zombie;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.PolyType;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public final class EventManager
{
	private EventConfig config;
	public Map<Integer, Event> disabled;
	public Map<Integer, Event> events;
	public List<Player> players;
	public Map<String, Integer> afkers;
	private Event current;
	protected Map<Player, Integer> colors;
	protected Map<Player, String> titles;
	protected Map<Player, int[]> positions;
	protected Map<Player, Integer> votes;
	protected State status;
	protected int counter;
	protected Countdown cdtask;
	private Scheduler task;
	protected Random rnd = new Random();
	protected List<Integer> eventIds;
	
	protected enum State
	{
		REGISTERING,
		VOTING,
		RUNNING,
		END
	}
	
	protected class Countdown implements Runnable
	{
		protected String getTime()
		{
			String mins = "" + counter / 60;
			String secs = (counter % 60 < 10 ? "0" + counter % 60 : "" + counter % 60);
			return mins + ":" + secs;
		}
		
		@Override
		public void run()
		{
			if (status == State.REGISTERING)
			{
				switch (counter)
				{
					case 300:
					case 240:
					case 180:
					case 120:
					case 60:
						announce(counter / 60 + " min(s) left to register, " + getCurrentEvent().getString("eventName"));
						break;
					case 30:
					case 10:
						announce(counter + " seconds left to register!");
						break;
				}
			}
			
			if (status == State.VOTING && counter == getInt("showVotePopupAt") && getBoolean("votePopupEnabled"))
			{
				NpcHtmlMessage html = new NpcHtmlMessage(0);
				StringBuilder sb = new StringBuilder();
				int i = 0, roll1, roll2, roll3, count = 0;
				
				roll1 = Rnd.get(events.size()) + 1;
				do
				{
					roll2 = Rnd.get(events.size()) + 1;
				}
				while (roll2 == roll1);
				do
				{
					roll3 = Rnd.get(events.size()) + 1;
				}
				while (roll3 == roll2 || roll3 == roll1);
				
				sb.append("<html><body><center><table width=270><tr><td width=270><center>Event Engine - Vote for your favourite event!</center></td></tr></table></center><br>");
				
				for (Map.Entry<Integer, Event> event : events.entrySet())
				{
					i++;
					if (i == roll1 || i == roll2 || i == roll3)
					{
						count++;
						sb.append("<center><table width=270 " + (count % 2 == 1 ? "" : "bgcolor=5A5A5A") + "><tr><td width=240>" + event.getValue().getString("eventName") + "</td><td width=30><a action=\"bypass -h eventvote " + event.getKey() + "\">Vote</a></td></tr></table></center>");
					}
					if (count == 3)
						break;
				}
				
				sb.append("</body></html>");
				html.setHtml(sb.toString());
				
				for (Player player : World.getInstance().getPlayers())
				{
					if (votes.containsKey(player) || player.getLevel() < 40)
						continue;
					
					player.sendPacket(html);
				}
			}
			
			if (counter == 0)
				schedule(1);
			else
			{
				counter--;
				ThreadPool.schedule(cdtask, 1000);
			}
		}
	}
	
	private class AntiAfk implements Runnable
	{
		private Player _player;
		
		public AntiAfk(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (getCurrentEvent() == null)
				return;
			
			if (_player.getAntiAfk() == 0)
			{
				_player.sendMessage("You were AFK during the event and therefore you are kicked.");
				
				if (_player.getPolyType() != PolyType.DEFAULT)
					_player.unpolymorph();
				
				if (_player.isDead())
					_player.doRevive();
				
				_player.teleportTo(positions.get(_player)[0], positions.get(_player)[1], positions.get(_player)[2], 0);
				_player.getAppearance().setNameColor(colors.get(_player));
				_player.setTitle(titles.get(_player));
				
				if (_player.getParty() != null)
					_player.getParty().removePartyMember(_player, MessageType.LEFT);
				
				_player.broadcastUserInfo();
				
				// pretend a logout
				getCurrentEvent().onLogout(_player);
				
				if (afkers.containsKey(_player.getAccountName()))
				{
					afkers.put(_player.getAccountName(), afkers.get(_player.getAccountName()) + 1);
					
					if (afkers.get(_player.getAccountName()) == getInt("antiAfkDisallowAfter"))
						ThreadPool.schedule(new AntiAfkDisallow(_player), getInt("antiAfkDisallowTime"));
				}
				else
					afkers.put(_player.getAccountName(), 1);
			}
			else if (_player.isOnline())
			{
				if (!_player.isDead())
					_player.setAntiAfk(_player.getAntiAfk() - 1);
				
				ThreadPool.schedule(this, 1000);
			}
		}
	}
	
	private class AntiAfkDisallow implements Runnable
	{
		private Player _player;
		
		public AntiAfkDisallow(Player player)
		{
			_player = player;
		}
		
		@SuppressWarnings("unlikely-arg-type")
		@Override
		public void run()
		{
			afkers.remove(_player);
		}
	}
	
	protected class Scheduler implements Runnable
	{
		@Override
		public void run()
		{
			switch (status)
			{
				case VOTING:
					if (votes.size() > 0)
						setCurrentEvent(getVoteWinner());
					else
						setCurrentEvent(eventIds.get(rnd.nextInt(eventIds.size())));
					
					announce("The next event will be: " + getCurrentEvent().getString("eventName"));
					announce("Registering phase started! You have " + getInt("registerTime") / 60 + " minutes to register!");
					announce("To register use .register or visit the event manager.");
					setStatus(State.REGISTERING);
					counter = getInt("registerTime") - 1;
					ThreadPool.schedule(cdtask, 1);
					break;
				
				case REGISTERING:
					announce("Registering phase ended!");
					if (players.size() < getCurrentEvent().getInt("minPlayers"))
					{
						announce("There are not enough participants! Next event in " + getInt("betweenEventsTime") / 60 + "mins!");
						setCurrentEvent(0);
						players.clear();
						colors.clear();
						positions.clear();
						setStatus(State.VOTING);
						counter = getInt("betweenEventsTime") - 1;
						ThreadPool.schedule(cdtask, 1);
					}
					else
					{
						announce("Event started!");
						setStatus(State.RUNNING);
						msgToAll("You'll be teleported to the event in 10 seconds.");
						schedule(10000);
					}
					break;
				
				case RUNNING:
					if (getCurrentEvent() instanceof Korean)
					{
						if (players.size() < 15) // 1vs1 and 2vs2
						{
							if (players.size() % 2 != 0)
								players.remove(Rnd.get(players.size()));
						}
						else
						// 3vs3
						{
							while (players.size() % 3 != 0)
								players.remove(Rnd.get(players.size()));
						}
					}
					
					getCurrentEvent().start();
					
					for (Player player : players)
					{
						EventStats.getInstance().tempTable.put(player.getObjectId(), new int[]
						{
							0,
							0,
							0,
							0
						});
						
						if (getInt("antiAfkTime") > 0)
						{
							player.setAntiAfk(getInt("antiAfkTime"));
							ThreadPool.schedule(new AntiAfk(player), 30000);
						}
					}
					break;
				
				case END:
					teleBackEveryone();
					if (getBoolean("statTrackingEnabled"))
					{
						EventStats.getInstance().applyChanges();
						EventStats.getInstance().tempTable.clear();
						EventStats.getInstance().updateSQL(getCurrentEvent().getPlayerList(), getCurrentEvent().eventId);
					}
					getCurrentEvent().reset();
					setCurrentEvent(0);
					players.clear();
					colors.clear();
					positions.clear();
					titles.clear();
					announce("Event ended! Next event in " + getInt("betweenEventsTime") / 60 + "mins!");
					setStatus(State.VOTING);
					counter = getInt("betweenEventsTime") - 1;
					ThreadPool.schedule(cdtask, 1);
					break;
			}
		}
	}
	
	private static class SingletonHolder
	{
		protected static final EventManager _instance = new EventManager();
	}
	
	public static EventManager getInstance()
	{
		return SingletonHolder._instance;
	}
	
	public EventManager()
	{
		config = EventConfig.getInstance();
		
		events = new ConcurrentHashMap<>();
		disabled = new ConcurrentHashMap<>();
		players = new CopyOnWriteArrayList<>();
		afkers = new ConcurrentHashMap<>();
		votes = new ConcurrentHashMap<>();
		titles = new ConcurrentHashMap<>();
		colors = new ConcurrentHashMap<>();
		positions = new ConcurrentHashMap<>();
		eventIds = new CopyOnWriteArrayList<>();
		status = State.VOTING;
		task = new Scheduler();
		cdtask = new Countdown();
		counter = 0;
		
		List<Integer> disabledEvents = getRestriction("disabledEvents");
		
		// Add the events to the list
		if (!disabledEvents.contains(1))
			events.put(1, new DM());
		else
			disabled.put(1, new DM());
		
		if (!disabledEvents.contains(2))
			events.put(2, new Domination());
		else
			disabled.put(2, new Domination());
		
		if (!disabledEvents.contains(3))
			events.put(3, new DoubleDomination());
		else
			disabled.put(3, new DoubleDomination());
		
		if (!disabledEvents.contains(4))
			events.put(4, new LMS());
		else
			disabled.put(4, new LMS());
		
		if (!disabledEvents.contains(5))
			events.put(5, new Lucky());
		else
			disabled.put(5, new Lucky());
		
		if (!disabledEvents.contains(6))
			events.put(6, new Simon());
		else
			disabled.put(6, new Simon());
		
		if (!disabledEvents.contains(7))
			events.put(7, new TvT());
		else
			disabled.put(7, new TvT());
		
		if (!disabledEvents.contains(8))
			events.put(8, new VIPTvT());
		else
			disabled.put(8, new VIPTvT());
		
		if (!disabledEvents.contains(9))
			events.put(9, new Zombie());
		else
			disabled.put(9, new Zombie());
		
		if (!disabledEvents.contains(10))
			events.put(10, new CTF());
		else
			disabled.put(10, new CTF());
		
		if (!disabledEvents.contains(11))
			events.put(11, new Russian());
		else
			disabled.put(11, new Russian());
		
		if (!disabledEvents.contains(12))
			events.put(12, new Bomb());
		else
			disabled.put(12, new Bomb());
		
		if (!disabledEvents.contains(13))
			events.put(13, new Mutant());
		else
			disabled.put(13, new Mutant());
		
		if (!disabledEvents.contains(14))
			events.put(14, new Battlefield());
		else
			disabled.put(14, new Battlefield());
		
		if (!disabledEvents.contains(15))
			events.put(15, new HG());
		else
			disabled.put(15, new HG());
		
		if (!disabledEvents.contains(16))
			events.put(16, new Raid());
		else
			disabled.put(16, new Raid());
		
		if (!disabledEvents.contains(17))
			events.put(17, new Korean());
		else
			disabled.put(17, new Korean());
		
		if (!disabledEvents.contains(18))
			events.put(18, new Treasure());
		else
			disabled.put(18, new Treasure());
		
		for (int eventId : events.keySet())
			eventIds.add(eventId);
		
		// Start the scheduler
		counter = getInt("firstAfterStartTime") - 1;
		ThreadPool.schedule(cdtask, 1);
		
		setZombiesEvent();
		
		System.out.println("Event Engine Started");
	}
	
	public boolean addVote(Player player, int eventId)
	{
		if (getStatus() != State.VOTING)
		{
			player.sendMessage("You can't vote now!");
			return false;
		}
		if (votes.containsKey(player))
		{
			player.sendMessage("You have already voted for an event!");
			return false;
		}
		if (player.getLevel() < 40)
		{
			player.sendMessage("Your level is too low to vote for events!");
			return false;
		}
		
		player.sendMessage("You have succesfully voted for the event");
		votes.put(player, eventId);
		return true;
	}
	
	protected static void announce(String text)
	{
		World.toAllOnlinePlayers(new CreatureSay(0, SayType.HERO_VOICE, "", "[Event] " + text));
	}
	
	private boolean canRegister(Player player)
	{
		if (players.contains(player))
		{
			player.sendMessage("You are already registered to the event!");
			return false;
		}
		if (player.isInJail())
		{
			player.sendMessage("You can't register from the jail.");
			return false;
		}
		if (player.isInOlympiadMode())
		{
			player.sendMessage("You can't register while you are in the olympiad.");
			return false;
		}
		if (player.getLevel() > getCurrentEvent().getInt("maxLvl"))
		{
			player.sendMessage("You are greater than the maximum allowed lvl.");
			return false;
		}
		if (player.getLevel() < getCurrentEvent().getInt("minLvl"))
		{
			player.sendMessage("You are lower than the minimum allowed lvl.");
			return false;
		}
		if (player.getKarma() > 0)
		{
			player.sendMessage("You can't register if you have karma.");
			return false;
		}
		if (player.isCursedWeaponEquipped())
		{
			player.sendMessage("You can't register with a cursed weapon.");
			return false;
		}
		if (player.isDead())
		{
			player.sendMessage("You can't register while you are dead.");
			return false;
		}
		if (afkers.containsKey(player.getAccountName()) && afkers.get(player.getAccountName()) == getInt("antiAfkDisallowAfter"))
		{
			player.sendMessage("You can't register because you were AFK inside events.");
			return false;
		}
		if (!getBoolean("dualboxAllowed"))
		{
			String ip = player.getClient().getConnection().getInetAddress().getHostAddress();
			for (Player p : players)
			{
				if (p.getClient().getConnection().getInetAddress().getHostAddress().equalsIgnoreCase(ip))
				{
					player.sendMessage("You have already joined the event with another character.");
					return false;
				}
			}
		}
		
		return true;
	}
	
	public boolean canTargetPlayer(Player target, Player self)
	{
		if (getStatus() == State.RUNNING)
		{
			if ((isRegistered(target) && isRegistered(self)) || (!isRegistered(target) && !isRegistered(self)))
				return true;
			
			return false;
		}
		
		return true;
	}
	
	public void end(String text)
	{
		announce(text);
		status = State.END;
		schedule(1);
	}
	
	public boolean getBoolean(String propName)
	{
		return config.getBoolean(0, propName);
	}
	
	public Event getCurrentEvent()
	{
		return current;
	}
	
	public List<String> getEventNames()
	{
		List<String> map = new CopyOnWriteArrayList<>();
		for (Event event : events.values())
			map.add(event.getString("eventName"));
		
		return map;
	}
	
	public Map<Integer, String> getEventMap()
	{
		Map<Integer, String> map = new ConcurrentHashMap<>();
		for (Event event : disabled.values())
			map.put(event.getInt("ids"), event.getString("eventName"));
		for (Event event : events.values())
			map.put(event.getInt("ids"), event.getString("eventName"));
		
		return map;
	}
	
	public Event getEvent(int id)
	{
		for (Event event : events.values())
		{
			if (event.getInt("ids") == id)
				return event;
		}
		for (Event event : disabled.values())
		{
			if (event.getInt("ids") == id)
				return event;
		}
		
		return null;
	}
	
	public void enableEvent(int id, int enable)
	{
		if (enable == 1)
		{
			if (disabled.containsKey(id))
				events.put(id, disabled.remove(id));
		}
		else
		{
			if (events.containsKey(id))
				disabled.put(id, events.remove(id));
		}
	}
	
	public boolean isEnabled(int id)
	{
		if (events.containsKey(id))
			return true;
		
		return false;
	}
	
	public int getInt(String propName)
	{
		return config.getInt(0, propName);
	}
	
	protected int[] getPosition(String owner, int num)
	{
		return config.getPosition(0, owner, num);
	}
	
	public List<Integer> getRestriction(String type)
	{
		return config.getRestriction(0, type);
	}
	
	public int getInt(int eventId, String propName)
	{
		return config.getInt(eventId, propName);
	}
	
	public boolean getBoolean(int eventId, String propName)
	{
		return config.getBoolean(eventId, propName);
	}
	
	public String getString(int eventId, String propName)
	{
		return config.getString(eventId, propName);
	}
	
	private State getStatus()
	{
		return status;
	}
	
	public String getString(String propName)
	{
		return config.getString(0, propName);
	}
	
	private int getVoteCount(int event)
	{
		int count = 0;
		for (int e : votes.values())
			if (e == event)
				count++;
		
		return count;
	}
	
	protected int getVoteWinner()
	{
		int eventId = 0;
		int maxVotes = -1;
		Map<Integer, Integer> temp = new ConcurrentHashMap<>();
		for (int vote : votes.values())
		{
			if (!temp.containsKey(vote))
			{
				temp.put(vote, 1);
			}
			else
			{
				temp.put(vote, temp.get(vote) + 1);
			}
		}
		for (Entry<Integer, Integer> v : temp.entrySet())
		{
			if (v.getValue() > maxVotes)
			{
				eventId = v.getKey();
				maxVotes = v.getValue();
			}
		}
		
		votes.clear();
		return eventId;
	}
	
	public boolean isRegistered(Player player)
	{
		if (getCurrentEvent() != null)
			return getCurrentEvent().players.containsKey(player);
		
		return false;
	}
	
	public boolean isRegistered(Creature player)
	{
		if (getCurrentEvent() != null)
			return getCurrentEvent().players.containsKey(player);
		
		return false;
	}
	
	public boolean isRunning()
	{
		if (getStatus() == State.RUNNING)
			return true;
		
		return false;
	}
	
	protected void msgToAll(String text)
	{
		for (Player player : players)
			player.sendMessage(text);
	}
	
	public void onLogout(Player player)
	{
		if (votes.containsKey(player))
			votes.remove(player);
		if (players.contains(player))
		{
			players.remove(player);
			colors.remove(player);
			titles.remove(player);
			positions.remove(player);
		}
	}
	
	public boolean registerPlayer(Player player)
	{
		if (getStatus() != State.REGISTERING)
		{
			player.sendMessage("You can't register now!");
			return false;
		}
		if (getBoolean("eventBufferEnabled"))
		{
			if (!EventBuffer.getInstance().playerHaveTemplate(player))
			{
				player.sendMessage("You have to set a buff template first!");
				EventBuffer.getInstance().showHtml(player);
				return false;
			}
		}
		if (canRegister(player))
		{
			player.sendMessage("You have succesfully registered to the event!");
			players.add(player);
			titles.put(player, player.getTitle());
			colors.put(player, player.getAppearance().getNameColor());
			positions.put(player, new int[]
			{
				player.getX(),
				player.getY(),
				player.getZ()
			});
			return true;
		}
		
		player.sendMessage("You have failed registering to the event!");
		return false;
	}
	
	protected void schedule(int time)
	{
		ThreadPool.schedule(task, time);
	}
	
	protected void setCurrentEvent(int eventId)
	{
		current = eventId == 0 ? null : events.get(eventId);
	}
	
	protected void setStatus(State s)
	{
		status = s;
	}
	
	public void showFirstHtml(Player player, int obj)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(obj);
		StringBuilder sb = new StringBuilder();
		int count = 0;
		
		sb.append("<html><body><center><table width=270><tr><td width=145>Event Engine</td><td width=75>" + (getBoolean("eventBufferEnabled") ? "<a action=\"bypass -h eventbuffershow\">Buffer</a>" : "") + "</td><td width=50><a action=\"bypass -h eventstats 1\">Statistics</a></td></tr></table></center><br>");
		
		if (getStatus() == State.VOTING)
		{
			sb.append("<center><table width=270 bgcolor=5A5A5A><tr><td width=90>Events</td><td width=140><center>Time left: " + cdtask.getTime() + "</center></td><td width=40><center>Votes</center></td></tr></table></center><br>");
			
			for (Map.Entry<Integer, Event> event : events.entrySet())
			{
				count++;
				sb.append("<center><table width=270 " + (count % 2 == 1 ? "" : "bgcolor=5A5A5A") + "><tr><td width=180>" + event.getValue().getString("eventName") + "</td><td width=30><a action=\"bypass -h eventinfo " + event.getKey() + "\">Info</a></td><td width=30><center>" + getVoteCount(event.getKey()) + "</td></tr></table></center>");
			}
			
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else if (getStatus() == State.REGISTERING)
		{
			sb.append("<center><table width=270 bgcolor=5A5A5A><tr><td width=70>");
			
			if (players.contains(player))
				sb.append("<a action=\"bypass -h unregister\">Unregister</a>");
			else
				sb.append("<a action=\"bypass -h register\">Register</a>");
			
			sb.append("</td><td width=130><center><a action=\"bypass -h eventinfo " + getCurrentEvent().getInt("ids") + "\">" + getCurrentEvent().getString("eventName") + "</a></td><td width=70>Time: " + cdtask.getTime() + "</td></tr></table><br>");
			
			for (Player p : EventManager.getInstance().players)
			{
				count++;
				sb.append("<center><table width=270 " + (count % 2 == 1 ? "" : "bgcolor=5A5A5A") + "><tr><td width=120>" + p.getName() + "</td><td width=40>lvl " + p.getLevel() + "</td><td width=110>" + p.getTemplate().getClassName() + "</td></tr></table>");
			}
			
			sb.append("</body></html>");
			html.setHtml(sb.toString());
			player.sendPacket(html);
		}
		else if (getStatus() == State.RUNNING)
			getCurrentEvent().showHtml(player, obj);
	}
	
	protected void teleBackEveryone()
	{
		for (Player player : getCurrentEvent().getPlayerList())
		{
			if (player.getPolyType() != PolyType.DEFAULT)
				player.unpolymorph();
			
			if (player.isDead())
				player.doRevive();
			
			player.teleportTo(positions.get(player)[0], positions.get(player)[1], positions.get(player)[2], 0);
			player.getAppearance().setNameColor(colors.get(player));
			player.setTitle(titles.get(player));
			
			if (player.getParty() != null)
				player.getParty().removePartyMember(player, MessageType.LEFT);
			
			player.broadcastUserInfo();
		}
	}
	
	public boolean unregisterPlayer(Player player)
	{
		if (!players.contains(player))
		{
			player.sendMessage("You are not registered to the event!");
			return false;
		}
		else if (getStatus() != State.REGISTERING)
		{
			player.sendMessage("You can't unregister now!");
			return false;
		}
		
		player.sendMessage("You have succesfully unregistered from the event!");
		players.remove(player);
		colors.remove(player);
		positions.remove(player);
		return true;
	}
	
	public boolean areTeammates(Player player, Player target)
	{
		if (getCurrentEvent() == null)
			return false;
		
		if (getCurrentEvent().numberOfTeams() < 2)
			return false;
		
		if (getCurrentEvent().getTeam(player) == getCurrentEvent().getTeam(target))
			return true;
		
		return false;
	}
	
	public void manualStart(int eventId)
	{
		setCurrentEvent(eventId);
		announce("The next event will be: " + getCurrentEvent().getString("eventName"));
		announce("Registering phase started! You have " + getInt("registerTime") / 60 + " minutes to register!");
		announce("To register use .register or visit the event manager.");
		setStatus(State.REGISTERING);
		counter = getInt("registerTime") - 1;
	}
	
	public void manualStop()
	{
		announce("The event has been aborted by a GM.");
		if (getStatus() == State.REGISTERING)
		{
			setCurrentEvent(0);
			players.clear();
			colors.clear();
			positions.clear();
			setStatus(State.VOTING);
			counter = getInt("betweenEventsTime") - 1;
		}
		else if (getStatus() == State.RUNNING)
			getCurrentEvent().endEvent();
	}
	
	public boolean isSpecialEvent()
	{
		return getCurrentEvent() != null && (getCurrentEvent() instanceof LMS || getCurrentEvent() instanceof DM);
	}
	
	public static void setZombiesEvent()
	{
		FenceManager.getInstance().addFence(57227, -29805, 574, 2, 500, 0, 3);
		FenceManager.getInstance().addFence(57399, -29611, 574, 2, 0, 500, 3);
		FenceManager.getInstance().addFence(58591, -29566, 574, 2, 400, 0, 3);
		FenceManager.getInstance().addFence(58720, -29789, 574, 2, 0, 400, 3);
		FenceManager.getInstance().addFence(57956, -29362, 574, 2, 300, 0, 3);
		
		DoorData.getInstance().getDoor(21170006).openMe();
		DoorData.getInstance().getDoor(21170005).openMe();
		DoorData.getInstance().getDoor(21170004).openMe();
		DoorData.getInstance().getDoor(21170003).openMe();
	}
}