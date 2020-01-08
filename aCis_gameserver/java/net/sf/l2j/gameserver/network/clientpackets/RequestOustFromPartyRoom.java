package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExClosePartyRoom;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchList;

public final class RequestOustFromPartyRoom extends L2GameClientPacket
{
	private int _targetId;
	
	@Override
	protected void readImpl()
	{
		_targetId = readD();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getClient().getPlayer();
		if (player == null)
			return;
		
		final Player target = World.getInstance().getPlayer(_targetId);
		if (target == null)
			return;
		
		final PartyMatchRoom room = PartyMatchRoomList.getInstance().getPlayerRoom(target);
		if (room == null)
			return;
		
		if (room.getOwner() != player)
			return;
		
		if (player.isInParty() && target.isInParty() && player.getParty().getLeaderObjectId() == target.getParty().getLeaderObjectId())
			player.sendPacket(SystemMessageId.CANNOT_DISMISS_PARTY_MEMBER);
		else
		{
			room.deleteMember(target);
			target.setPartyRoom(0);
			
			// Close the PartyRoom window
			target.sendPacket(ExClosePartyRoom.STATIC_PACKET);
			
			// Add player back on waiting list
			PartyMatchWaitingList.getInstance().addPlayer(target);
			
			// Send Room list
			target.sendPacket(new PartyMatchList(target, 0, MapRegionData.getInstance().getClosestLocation(target.getX(), target.getY()), target.getLevel()));
			
			// Clean player's LFP title
			target.broadcastUserInfo();
			
			target.sendPacket(SystemMessageId.OUSTED_FROM_PARTY_ROOM);
		}
	}
}