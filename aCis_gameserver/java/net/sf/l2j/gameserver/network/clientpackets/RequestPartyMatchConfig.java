package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ActionFailed;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyRoomMember;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchDetail;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchList;

public final class RequestPartyMatchConfig extends L2GameClientPacket
{
	private int _auto, _loc, _lvl;
	
	@Override
	protected void readImpl()
	{
		_auto = readD();
		_loc = readD();
		_lvl = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		if (!player.isInPartyMatchRoom() && player.getParty() != null && player.getParty().getLeader() != player)
		{
			player.sendPacket(SystemMessageId.CANT_VIEW_PARTY_ROOMS);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		if (player.isInPartyMatchRoom())
		{
			// If Player is in Room show him room, not list
			final PartyMatchRoomList list = PartyMatchRoomList.getInstance();
			if (list == null)
				return;
			
			final PartyMatchRoom room = list.getPlayerRoom(player);
			if (room == null)
				return;
			
			player.sendPacket(new PartyMatchDetail(room));
			player.sendPacket(new ExPartyRoomMember(room, 2));
			
			player.setPartyRoom(room.getId());
			player.broadcastUserInfo();
		}
		else
		{
			// Add to waiting list
			PartyMatchWaitingList.getInstance().addPlayer(player);
			
			// Send Room list
			player.sendPacket(new PartyMatchList(player, _auto, _loc, _lvl));
		}
	}
}