package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExClosePartyRoom;

public final class RequestWithdrawPartyRoom extends L2GameClientPacket
{
	private int _roomId;
	@SuppressWarnings("unused")
	private int _unk1;
	
	@Override
	protected void readImpl()
	{
		_roomId = readD();
		_unk1 = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_roomId);
		if (room == null)
			return;
		
		if (player.isInParty() && room.getOwner().isInParty() && player.getParty().getLeaderObjectId() == room.getOwner().getParty().getLeaderObjectId())
		{
			// If user is in party with Room Owner is not removed from Room
		}
		else
		{
			room.deleteMember(player);
			player.setPartyRoom(0);
			player.broadcastUserInfo();
			
			player.sendPacket(ExClosePartyRoom.STATIC_PACKET);
			player.sendPacket(SystemMessageId.PARTY_ROOM_EXITED);
		}
	}
}