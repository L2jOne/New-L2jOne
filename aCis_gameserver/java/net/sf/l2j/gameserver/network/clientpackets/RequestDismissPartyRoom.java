package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;

public class RequestDismissPartyRoom extends L2GameClientPacket
{
	private int _roomId;
	
	@Override
	protected void readImpl()
	{
		_roomId = readD();
		readD(); // Not used.
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
		
		PartyMatchRoomList.getInstance().deleteRoom(_roomId);
	}
}