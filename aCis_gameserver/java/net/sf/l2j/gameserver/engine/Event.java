package net.sf.l2j.gameserver.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.sf.l2j.commons.concurrent.ThreadPool;
import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.sql.SpawnTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.engine.events.Raid;
import net.sf.l2j.gameserver.enums.LootRule;
import net.sf.l2j.gameserver.enums.MessageType;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.enums.items.EtcItemType;
import net.sf.l2j.gameserver.enums.skills.AbnormalEffect;
import net.sf.l2j.gameserver.enums.skills.L2SkillType;
import net.sf.l2j.gameserver.model.L2Effect;
import net.sf.l2j.gameserver.model.L2Skill;
import net.sf.l2j.gameserver.model.WorldObject;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.group.Party;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.ExShowScreenMessage;

public abstract class Event
{
	public static final int[] ITEMS =
	{
		6707,
		6709,
		6708,
		6710,
		6704,
		6701,
		6702,
		6703,
		6706,
		6705,
		6713,
		6714,
		6712,
		6711,
		6697,
		6688,
		6696,
		6691,
		7579,
		6695,
		6694,
		6689,
		6693,
		6690
	};
	public int eventId;
	public EventConfig config = EventConfig.getInstance();
	public Map<Integer, EventTeam> teams;
	public ResurrectorTask resurrectorTask;
	public Clock clock;
	public String scorebartext;
	public int time;
	public int winnerTeam;
	public int loserTeam;
	private Map<Player, ArrayList<L2Skill>> summons;
	
	// TEAM-STATUS-SCORE
	public Map<Player, int[]> players;
	
	public class Clock implements Runnable
	{
		private int totalTime;
		
		public String getTime()
		{
			String mins = "" + time / 60;
			String secs = (time % 60 < 10 ? "0" + time % 60 : "" + time % 60);
			return mins + ":" + secs + "";
		}
		
		@Override
		public void run()
		{
			clockTick();
			
			if (time < totalTime)
			{
				scorebartext = getScorebar();
				if (scorebartext != "")
				{
					for (Player player : getPlayerList())
						player.sendPacket(new ExShowScreenMessage(1, -1, 3, false, 1, 0, 0, false, 2000, false, scorebartext));
				}
			}
			
			if (time <= 0)
				schedule(1);
			else
			{
				time--;
				ThreadPool.schedule(clock, 1000);
			}
		}
		
		public void setTime(int t)
		{
			time = t;
		}
		
		public void startClock(int mt)
		{
			totalTime = mt - 2;
			time = mt;
			ThreadPool.schedule(clock, 1);
		}
	}
	
	public class ResurrectorTask implements Runnable
	{
		private Player player;
		
		public ResurrectorTask(Player p)
		{
			player = p;
			ThreadPool.schedule(this, 7000);
		}
		
		@Override
		public void run()
		{
			if (EventManager.getInstance().isRegistered(player))
			{
				player.doRevive();
				
				player.setCurrentCp(player.getMaxCp());
				player.setCurrentHp(player.getMaxHp());
				player.setCurrentMp(player.getMaxMp());
				teleportToTeamPos(player);
			}
		}
	}
	
	public Event()
	{
		teams = new ConcurrentHashMap<>();
		clock = new Clock();
		players = new ConcurrentHashMap<>();
		summons = new ConcurrentHashMap<>();
		time = 0;
	}
	
	public void clockTick()
	{
		
	}
	
	public void dropBomb(Player player)
	{
		
	}
	
	public void onHit(Player actor, Player target)
	{
		
	}
	
	public void useCapture(Player player, Npc base)
	{
		
	}
	
	public void addToResurrector(Player player)
	{
		new ResurrectorTask(player);
	}
	
	public void announce(Set<Player> list, String text)
	{
		for (Player player : list)
			player.sendPacket(new CreatureSay(0, SayType.HERO_VOICE, "", "[Event] " + text));
	}
	
	public boolean canAttack(Player player, WorldObject target)
	{
		return true;
	}
	
	public int countOfPositiveStatus()
	{
		int count = 0;
		for (Player player : getPlayerList())
			if (getStatus(player) >= 0)
				count++;
		
		return count;
	}
	
	public void createNewTeam(int id, String name, int[] color, int[] startPos)
	{
		teams.put(id, new EventTeam(id, name, color, startPos));
	}
	
	public void createPartyOfTeam(int teamId)
	{
		int count = 0;
		Party party = null;
		
		List<Player> list = new CopyOnWriteArrayList<>();
		
		for (Player p : players.keySet())
			if (getTeam(p) == teamId)
				list.add(p);
		
		for (Player player : list)
		{
			if (count % 9 == 0 && list.size() - count != 1)
				party = new Party(player, player, LootRule.ITEM_LOOTER);
			if (party != null && count % 9 < 9)
				party.addPartyMember(player);
			
			count++;
		}
	}
	
	public void divideIntoTeams(int number)
	{
		int i = 0;
		
		while (EventManager.getInstance().players.size() != 0)
		{
			i++;
			Player player = EventManager.getInstance().players.get(Rnd.get(EventManager.getInstance().players.size()));
			
			// skip healers
			if (player.getClassId().getId() == 16 || player.getClassId().getId() == 97)
				continue;
			
			players.put(player, new int[]
			{
				i,
				0,
				0
			});
			EventManager.getInstance().players.remove(player);
			if (i == number)
				i = 0;
		}
		
		i = getPlayersOfTeam(1).size() > getPlayersOfTeam(2).size() ? 1 : 0;
		
		// healers here
		while (EventManager.getInstance().players.size() != 0)
		{
			i++;
			Player player = EventManager.getInstance().players.get(Rnd.get(EventManager.getInstance().players.size()));
			
			players.put(player, new int[]
			{
				i,
				0,
				0
			});
			EventManager.getInstance().players.remove(player);
			if (i == number)
				i = 0;
		}
	}
	
	public void forceSitAll()
	{
		for (Player player : players.keySet())
		{
			player.abortAttack();
			player.abortCast();
			player.setIsParalyzed(true);
			player.setIsInvul(true);
			player.startAbnormalEffect(AbnormalEffect.HOLD_2);
		}
	}
	
	public void forceStandAll()
	{
		for (Player player : players.keySet())
		{
			player.stopAbnormalEffect(AbnormalEffect.HOLD_2);
			player.setIsInvul(false);
			player.setIsParalyzed(false);
		}
	}
	
	public void InvisAll()
	{
		for (Player player : players.keySet())
		{
			player.abortAttack();
			player.abortCast();
			player.getAppearance().setVisible(true);
		}
	}
	
	public void unInvisAll()
	{
		for (Player player : players.keySet())
		{
			player.getAppearance().setVisible(false);
			player.broadcastCharInfo();
		}
	}
	
	public boolean getBoolean(String propName)
	{
		return config.getBoolean(eventId, propName);
	}
	
	public int[] getColor(String owner)
	{
		return config.getColor(eventId, owner);
	}
	
	public int getInt(String propName)
	{
		return config.getInt(eventId, propName);
	}
	
	public Set<Player> getPlayerList()
	{
		return players.keySet();
	}
	
	public List<Player> getPlayersOfTeam(int team)
	{
		List<Player> list = new CopyOnWriteArrayList<>();
		getPlayerList().stream().filter(x -> getTeam(x) == team).forEach(x -> list.add(x));
		return list;
	}
	
	public EventTeam getPlayersTeam(Player player)
	{
		return teams.get(players.get(player)[0]);
	}
	
	public List<Player> getPlayersWithStatus(int status)
	{
		List<Player> list = new CopyOnWriteArrayList<>();
		
		for (Player player : getPlayerList())
			if (getStatus(player) == status)
				list.add(player);
		
		return list;
	}
	
	public List<Player> getTopPlayers(int limit)
	{
		List<Player> temp = new CopyOnWriteArrayList<>();
		List<Player> copy = new CopyOnWriteArrayList<>(players.keySet());
		for (int i = 0; i < limit; i++)
		{
			Player max = null;
			for (Player ps : copy)
			{
				if ((max == null) || (players.get(ps)[2] > players.get(max)[2]))
				{
					max = ps;
				}
			}
			if (max == null)
			{
				break;
			}
			
			temp.add(max);
			copy.remove(max);
		}
		
		return temp;
	}
	
	public Player getPlayerWithMaxScore()
	{
		List<Player> players = getTopPlayers(1);
		if (players.isEmpty())
		{
			return null;
		}
		
		return players.get(0);
	}
	
	public void unequip()
	{
		for (Player player : players.keySet())
		{
			player.getInventory().unEquipItemInSlot(7);
			player.getInventory().unEquipItemInSlot(8);
		}
	}
	
	public int[] getPosition(String owner, int num)
	{
		return config.getPosition(eventId, owner, num);
	}
	
	public Player getRandomPlayer()
	{
		List<Player> temp = new CopyOnWriteArrayList<>();
		for (Player player : players.keySet())
			temp.add(player);
		
		return temp.get(Rnd.get(temp.size()));
	}
	
	public Player getRandomPlayerFromTeam(int team)
	{
		List<Player> temp = new CopyOnWriteArrayList<>();
		for (Player player : players.keySet())
			if (getTeam(player) == team)
				temp.add(player);
		
		return temp.get(Rnd.get(temp.size()));
	}
	
	public List<Player> getPlayersFromTeamWithStatus(int team, int status)
	{
		List<Player> players = getPlayersWithStatus(status);
		List<Player> temp = new CopyOnWriteArrayList<>();
		
		for (Player player : players)
			if (getTeam(player) == team)
				temp.add(player);
		
		return temp;
	}
	
	public Player getRandomPlayerFromTeamWithStatus(int team, int status)
	{
		List<Player> temp = getPlayersFromTeamWithStatus(team, status);
		return temp.get(Rnd.get(temp.size()));
	}
	
	public List<Integer> getRestriction(String type)
	{
		return config.getRestriction(eventId, type);
	}
	
	public int getScore(Player player)
	{
		return players.get(player)[2];
	}
	
	public int getStatus(Player player)
	{
		return players.get(player)[1];
	}
	
	public String getString(String propName)
	{
		return config.getString(eventId, propName);
	}
	
	public int getTeam(Player player)
	{
		return players.get(player)[0];
	}
	
	public int getWinnerTeam()
	{
		int maxScore = 0;
		int teamNum = 0;
		for (EventTeam team : teams.values())
		{
			if (team.getScore() > maxScore)
			{
				maxScore = team.getScore();
				teamNum = team.getId();
			}
		}
		
		return teamNum;
	}
	
	public void giveReward(List<Player> players, int id, int ammount)
	{
		for (Player player : players)
		{
			if (player == null)
				continue;
			
			player.addItem("Event", id, ammount, player, true);
			EventStats.getInstance().tempTable.get(player.getObjectId())[0] = 1;
		}
	}
	
	public void giveReward(Player player, int id, int ammount)
	{
		EventStats.getInstance().tempTable.get(player.getObjectId())[0] = 1;
		player.addItem("Event", id, ammount, player, true);
	}
	
	public void increasePlayersScore(Player player)
	{
		int old = getScore(player);
		setScore(player, old + 1);
		EventStats.getInstance().tempTable.get(player.getObjectId())[3] = EventStats.getInstance().tempTable.get(player.getObjectId())[3] + 1;
	}
	
	public void msgToAll(String text)
	{
		for (Player player : players.keySet())
			player.sendMessage(text);
	}
	
	public void onDie(Player victim, Creature killer)
	{
		EventStats.getInstance().tempTable.get(victim.getObjectId())[2] = EventStats.getInstance().tempTable.get(victim.getObjectId())[2] + 1;
	}
	
	public void onKill(Creature victim, Player killer)
	{
		EventStats.getInstance().tempTable.get(killer.getObjectId())[1] = EventStats.getInstance().tempTable.get(killer.getObjectId())[1] + 1;
	}
	
	public void onLogout(Player player)
	{
		if (players.containsKey(player))
			removePlayer(player);
		
		player.setXYZ(EventManager.getInstance().positions.get(player)[0], EventManager.getInstance().positions.get(player)[1], EventManager.getInstance().positions.get(player)[2]);
		player.setTitle(EventManager.getInstance().titles.get(player));
		
		if (teams.size() == 1)
		{
			if (getPlayerList().size() == 1)
				endEvent();
		}
		else
		{
			int t = players.values().iterator().next()[0];
			for (Player p : getPlayerList())
				if (getTeam(p) != t)
					return;
			
			endEvent();
		}
	}
	
	public boolean onSay(SayType type, Player player, String text)
	{
		return true;
	}
	
	public boolean onTalkNpc(Npc npc, Player player)
	{
		return false;
	}
	
	public boolean onUseItem(Player player, ItemInstance item)
	{
		if (EventManager.getInstance().getRestriction("item").contains(item.getItemId()) || getRestriction("item").contains(item.getItemId()))
			return false;
		
		if (item.getItemType() == EtcItemType.POTION && !getBoolean("allowPotions"))
			return false;
		
		if (item.getItemType() == EtcItemType.SCROLL)
			return false;
		
		if (item.getItemType() == EtcItemType.PET_COLLAR)
			return false;
		
		return true;
	}
	
	public boolean onUseMagic(L2Skill skill)
	{
		if (EventManager.getInstance().getRestriction("skill").contains(skill.getId()) || getRestriction("skill").contains(skill.getId()))
			return false;
		
		if (skill.getSkillType() == L2SkillType.RESURRECT && !(this instanceof Raid))
			return false;
		
		if (skill.getSkillType() == L2SkillType.SUMMON_FRIEND)
			return false;
		
		if (skill.getSkillType() == L2SkillType.RECALL)
			return false;
		
		if (skill.getSkillType() == L2SkillType.FAKE_DEATH)
			return false;
		
		return true;
	}
	
	public void prepare(Player player)
	{
		if (player.isDead())
			player.doRevive();
		
		if (player.isCastingNow())
			player.abortCast();
		
		player.getAppearance().setVisible(false);
		
		if (player.hasPet())
			player.getSummon().unSummon(player);
		
		if (player.isMounted())
			player.dismount();
		
		if (getBoolean("removeBuffs"))
		{
			player.stopAllEffects();
			if (player.hasServitor())
				player.getSummon().unSummon(player);
		}
		else
		{
			for (L2Effect e : player.getAllEffects())
				if (e.getStackType().equals("hero_buff"))
					e.exit();
			
			if (player.hasServitor())
			{
				ArrayList<L2Skill> summonBuffs = new ArrayList<>();
				
				for (L2Effect e : player.getSummon().getAllEffects())
				{
					if (e.getStackType().equals("hero_buff"))
						e.exit();
					else
						summonBuffs.add(e.getSkill());
				}
				
				summons.put(player, summonBuffs);
			}
		}
		
		ItemInstance wpn = player.getActiveWeaponInstance();
		if (wpn != null && wpn.isHeroItem())
			player.useEquippableItem(wpn, false);
		
		if (player.getParty() != null)
		{
			Party party = player.getParty();
			party.removePartyMember(player, MessageType.LEFT);
		}
		
		int[] nameColor = getPlayersTeam(player).getTeamColor();
		player.getAppearance().setNameColor(nameColor[0], nameColor[1], nameColor[2]);
		player.setTitle("<- 0 ->");
		
		if (EventManager.getInstance().getBoolean("eventBufferEnabled"))
			EventBuffer.getInstance().buffPlayer(player);
		
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
		
		player.broadcastUserInfo();
	}
	
	public ArrayList<L2Skill> getSummonBuffs(Player player)
	{
		return summons.get(player);
	}
	
	public void preparePlayers()
	{
		for (Player player : players.keySet())
			prepare(player);
	}
	
	public void removePlayer(Player player)
	{
		players.remove(player);
	}
	
	public void reset()
	{
		players.clear();
		summons.clear();
		winnerTeam = 0;
		
		for (EventTeam team : teams.values())
			team.setScore(0);
	}
	
	public void selectPlayers(int teamId, int playerCount)
	{
		for (int i = 0; i < playerCount; i++)
		{
			Player player = EventManager.getInstance().players.get(Rnd.get(EventManager.getInstance().players.size()));
			players.put(player, new int[]
			{
				teamId,
				0,
				0
			});
			EventManager.getInstance().players.remove(player);
		}
	}
	
	public void setScore(Player player, int score)
	{
		players.get(player)[2] = score;
		player.setTitle("<- " + score + " ->");
		player.broadcastUserInfo();
	}
	
	public void setStatus(Player player, int status)
	{
		if (players.containsKey(player))
			players.get(player)[1] = status;
	}
	
	public void setTeam(Player player, int team)
	{
		players.get(player)[0] = team;
	}
	
	public Spawn spawnNPC(int xPos, int yPos, int zPos, int npcId)
	{
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		
		try
		{
			final Spawn spawn = new Spawn(template);
			spawn.setLoc(xPos, yPos, zPos, 0);
			spawn.setRespawnDelay(1);
			SpawnTable.getInstance().addSpawn(spawn, false);
			spawn.setRespawnState(true);
			spawn.doSpawn(false);
			return spawn;
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public void teleportPlayer(Player player, int[] coordinates)
	{
		player.teleportTo(coordinates[0] + (Rnd.get(coordinates[3] * 2) - coordinates[3]), coordinates[1] + (Rnd.get(coordinates[3] * 2) - coordinates[3]), coordinates[2], 0);
	}
	
	public void teleportToTeamPos()
	{
		for (Player player : players.keySet())
			teleportToTeamPos(player);
	}
	
	public void teleportToTeamPos(Player player)
	{
		int[] pos = getPosition(teams.get(getTeam(player)).getName(), 0);
		teleportPlayer(player, pos);
	}
	
	public void unspawnNPC(Spawn npcSpawn)
	{
		if (npcSpawn == null)
			return;
		
		npcSpawn.getNpc().deleteMe();
		npcSpawn.setRespawnState(false);
		SpawnTable.getInstance().deleteSpawn(npcSpawn, true);
	}
	
	public int numberOfTeams()
	{
		return teams.size();
	}
	
	public void sendMsg()
	{
		for (Player player : getPlayerList())
			player.sendPacket(new ExShowScreenMessage(1, -1, 2, false, 0, 0, 0, false, 3000, false, getStartingMsg()));
	}
	
	public abstract void endEvent();
	
	public abstract String getStartingMsg();
	
	public abstract String getScorebar();
	
	public abstract void start();
	
	public abstract void showHtml(Player player, int obj);
	
	public abstract void schedule(int time);
}