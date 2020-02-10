package net.sf.l2j.gameserver.engine.events;

import net.sf.l2j.commons.concurrent.ThreadPool;

import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.engine.EventTeam;
import net.sf.l2j.gameserver.enums.SayType;
import net.sf.l2j.gameserver.model.actor.Creature;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.itemcontainer.Inventory;
import net.sf.l2j.gameserver.model.spawn.Spawn;
import net.sf.l2j.gameserver.network.serverpackets.CreatureSay;
import net.sf.l2j.gameserver.network.serverpackets.InventoryUpdate;
import net.sf.l2j.gameserver.network.serverpackets.ItemList;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.network.serverpackets.SocialAction;

public class CTF extends Event
{
	public EventState eventState;
	public Player playerWithRedFlag;
	public Player playerWithBlueFlag;
	private Core task = new Core();
	private Spawn redFlagNpc;
	private Spawn blueFlagNpc;
	private Spawn redHolderNpc;
	private Spawn blueHolderNpc;
	private int redFlagStatus;
	private int blueFlagStatus;
	
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
						spawnFlagsAndHolders();
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
						
						unspawnFlagsAndHolders();
						if (playerWithRedFlag != null)
							unequipFlag(playerWithRedFlag);
						if (playerWithBlueFlag != null)
							unequipFlag(playerWithBlueFlag);
						setStatus(EventState.INACTIVE);
						
						if (winnerTeam == 0)
							EventManager.getInstance().end("The event ended in a tie! both teams had " + teams.get(1).getScore() + " flags taken!");
						else
						{
							giveReward(getPlayersOfTeam(winnerTeam), getInt("rewardId"), getInt("rewardAmmount"));
							EventManager.getInstance().end("Congratulation! The " + teams.get(winnerTeam).getName() + " team won the event with " + teams.get(winnerTeam).getScore() + " flags taken!");
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
	
	public CTF()
	{
		super();
		eventId = 10;
		createNewTeam(1, "Blue", getColor("Blue"), getPosition("Blue", 1));
		createNewTeam(2, "Red", getColor("Red"), getPosition("Red", 1));
	}
	
	@Override
	public void endEvent()
	{
		winnerTeam = getWinnerTeam();
		
		setStatus(EventState.END);
		clock.setTime(0);
	}
	
	private void equipFlag(Player player, int flag)
	{
		ItemInstance wpn = player.getActiveWeaponInstance();
		if (wpn != null)
			player.useEquippableItem(wpn, false);
		
		wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		if (wpn != null)
			player.useEquippableItem(wpn, false);
		
		player.addItem("", 6718, 1, player, false);
		player.getInventory().equipItem(player.getInventory().getItemByItemId(6718));
		player.broadcastPacket(new SocialAction(player, 16));
		
		switch (flag)
		{
			case 1:
				playerWithBlueFlag = player;
				announce(getPlayerList(), player.getName() + " took the Blue flag!");
				unspawnNPC(blueFlagNpc);
				break;
			case 2:
				playerWithRedFlag = player;
				announce(getPlayerList(), player.getName() + " took the Red flag!");
				unspawnNPC(redFlagNpc);
				break;
		}
		
		player.broadcastUserInfo();
		CreatureSay cs = new CreatureSay(player.getObjectId(), SayType.HERO_VOICE, ":", "You got it! Run back! ::");
		player.sendPacket(cs);
	}
	
	@Override
	public void onDie(Player victim, Creature killer)
	{
		super.onDie(victim, killer);
		
		if (playerWithRedFlag == victim)
		{
			announce(getPlayerList(), victim.getName() + " dropped the Red flag!");
			redFlagStatus = 2;
			unequipFlag(victim);
			redFlagNpc = spawnNPC(victim.getX(), victim.getY(), victim.getZ(), getInt("redFlagId"));
		}
		
		if (playerWithBlueFlag == victim)
		{
			announce(getPlayerList(), victim.getName() + " dropped the Blue flag!");
			blueFlagStatus = 2;
			unequipFlag(victim);
			blueFlagNpc = spawnNPC(victim.getX(), victim.getY(), victim.getZ(), getInt("blueFlagId"));
		}
		
		addToResurrector(victim);
	}
	
	@Override
	public void onLogout(Player player)
	{
		super.onLogout(player);
		
		if (playerWithRedFlag == player)
		{
			announce(getPlayerList(), player.getName() + " dropped the Red flag!");
			redFlagStatus = 2;
			unequipFlag(player);
			redFlagNpc = spawnNPC(player.getX(), player.getY(), player.getZ(), getInt("redFlagId"));
		}
		
		if (playerWithBlueFlag == player)
		{
			announce(getPlayerList(), player.getName() + " dropped the Blue flag!");
			blueFlagStatus = 2;
			unequipFlag(player);
			blueFlagNpc = spawnNPC(player.getX(), player.getY(), player.getZ(), getInt("blueFlagId"));
		}
	}
	
	@Override
	public boolean onTalkNpc(Npc npc, Player player)
	{
		if (npc.getNpcId() != getInt("blueFlagId") && npc.getNpcId() != getInt("blueFlagHolderId") && npc.getNpcId() != getInt("redFlagId") && npc.getNpcId() != getInt("redFlagHolderId"))
			return false;
		
		// Blue holder
		if (npc.getNpcId() == getInt("blueFlagHolderId"))
		{
			if (player == playerWithRedFlag)
			{
				if (blueFlagStatus == 0)
				{
					announce(getPlayerList(), "The Blue team scored!");
					teams.get(getTeam(player)).increaseScore();
					increasePlayersScore(player);
					returnFlag(2);
				}
				else
					player.sendMessage("Your team must kill enemy flag owner and return the flag in order to score!");
			}
		}
		// Red holder
		else if (npc.getNpcId() == getInt("redFlagHolderId"))
		{
			if (player == playerWithBlueFlag)
			{
				if (redFlagStatus == 0)
				{
					announce(getPlayerList(), "The Red team scored!");
					teams.get(getTeam(player)).increaseScore();
					increasePlayersScore(player);
					returnFlag(1);
				}
				else
					player.sendMessage("Your team must kill enemy flag owner and return the flag in order to score!");
			}
		}
		// Blue flag
		else if (npc.getNpcId() == getInt("blueFlagId"))
		{
			if (blueFlagStatus == 2)
			{
				// blue player
				if (getTeam(player) == 1)
					returnFlag(1);
				
				// red player
				if (getTeam(player) == 2)
					equipFlag(player, 1);
			}
			if (blueFlagStatus == 0)
			{
				if (getTeam(player) == 2)
				{
					equipFlag(player, 1);
					unspawnNPC(blueFlagNpc);
					blueFlagStatus = 1;
				}
			}
		}
		// Red flag
		else
		{
			if (redFlagStatus == 2)
			{
				// red player
				if (getTeam(player) == 2)
					returnFlag(2);
				
				// blue player
				if (getTeam(player) == 1)
					equipFlag(player, 2);
			}
			if (redFlagStatus == 0)
			{
				if (getTeam(player) == 1)
				{
					equipFlag(player, 2);
					unspawnNPC(redFlagNpc);
					redFlagStatus = 1;
				}
			}
		}
		
		return true;
	}
	
	@Override
	public boolean onUseItem(Player player, ItemInstance item)
	{
		if (playerWithRedFlag == player || playerWithBlueFlag == player)
			return false;
		
		return super.onUseItem(player, item);
	}
	
	private void returnFlag(int flag)
	{
		int[] pos;
		
		switch (flag)
		{
			case 1:
				if (playerWithBlueFlag != null)
					unequipFlag(playerWithBlueFlag);
				if (blueFlagStatus == 2)
					unspawnNPC(blueFlagNpc);
				
				pos = getPosition("BlueFlag", 1);
				blueFlagNpc = spawnNPC(pos[0], pos[1], pos[2], getInt("blueFlagId"));
				blueFlagStatus = 0;
				announce(getPlayerList(), "The Blue flag returned!");
				break;
			
			case 2:
				if (playerWithRedFlag != null)
					unequipFlag(playerWithRedFlag);
				if (redFlagStatus == 2)
					unspawnNPC(redFlagNpc);
				
				pos = getPosition("RedFlag", 1);
				redFlagNpc = spawnNPC(pos[0], pos[1], pos[2], getInt("redFlagId"));
				redFlagStatus = 0;
				announce(getPlayerList(), "The Red flag returned!");
				break;
		}
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
		sb.append("<html><body><table width=270><tr><td width=200>Event Engine </td><td><a action=\"bypass -h eventstats 1\">Statistics</a></td></tr></table><br><center><table width=270 bgcolor=5A5A5A><tr><td width=70>Running</td><td width=130><center>" + getString("eventName") + "</td><td width=70>Time: " + clock.getTime() + "</td></tr></table><center><table width=270><tr><td><center><font color=" + teams.get(1).getHexaColor() + ">" + teams.get(1).getScore() + "</font> - <font color=" + teams.get(2).getHexaColor() + ">" + teams.get(2).getScore() + "</font></td></tr></table><br><table width=270>");
		
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
	
	public void spawnFlagsAndHolders()
	{
		int[] pos = getPosition("BlueFlag", 1);
		blueFlagNpc = spawnNPC(pos[0], pos[1], pos[2], getInt("blueFlagId"));
		blueHolderNpc = spawnNPC(pos[0] + 50, pos[1], pos[2], getInt("blueFlagHolderId"));
		
		pos = getPosition("RedFlag", 1);
		redFlagNpc = spawnNPC(pos[0], pos[1], pos[2], getInt("redFlagId"));
		redHolderNpc = spawnNPC(pos[0] + 50, pos[1], pos[2], getInt("redFlagHolderId"));
	}
	
	@Override
	public void start()
	{
		setStatus(EventState.START);
		schedule(1);
	}
	
	@Override
	public String getStartingMsg()
	{
		return "Steal the enemy flag while keeping yours safe!";
	}
	
	@Override
	public String getScorebar()
	{
		return teams.get(1).getName() + ": " + teams.get(1).getScore() + "  " + teams.get(2).getName() + ": " + teams.get(2).getScore() + "  Time: " + clock.getTime();
	}
	
	public void unequipFlag(Player player)
	{
		ItemInstance wpn = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (wpn != null)
		{
			ItemInstance[] unequiped = player.getInventory().unEquipItemInBodySlotAndRecord(wpn.getItem().getBodyPart());
			player.getInventory().destroyItemByItemId("", 6718, 1, player, null);
			InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance element : unequiped)
				iu.addModifiedItem(element);
			player.sendPacket(iu);
			player.sendPacket(new ItemList(player, true));
			player.broadcastUserInfo();
		}
		
		if (player == playerWithRedFlag)
			playerWithRedFlag = null;
		if (player == playerWithBlueFlag)
			playerWithBlueFlag = null;
	}
	
	public void unspawnFlagsAndHolders()
	{
		unspawnNPC(blueFlagNpc);
		unspawnNPC(blueHolderNpc);
		unspawnNPC(redFlagNpc);
		unspawnNPC(redHolderNpc);
	}
}