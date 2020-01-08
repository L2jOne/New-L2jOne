package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;
import net.sf.l2j.gameserver.network.SystemMessageId;
import net.sf.l2j.gameserver.network.serverpackets.ExManagePartyRoomMember;
import net.sf.l2j.gameserver.network.serverpackets.ExPartyRoomMember;
import net.sf.l2j.gameserver.network.serverpackets.PartyMatchDetail;
import net.sf.l2j.gameserver.network.serverpackets.SystemMessage;

public final class RequestPartyMatchDetail extends L2GameClientPacket
{
	private int _roomId;
	@SuppressWarnings("unused")
	private int _unk1;
	@SuppressWarnings("unused")
	private int _unk2;
	@SuppressWarnings("unused")
	private int _unk3;
	
	@Override
	protected void readImpl()
	{
		_roomId = readD();
		/*
		 * IF player click on Room all unk are 0 IF player click AutoJoin values are -1 1 1
		 */
		_unk1 = readD();
		_unk2 = readD();
		_unk3 = readD();
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
		
		if ((player.getLevel() >= room.getMinLvl()) && (player.getLevel() <= room.getMaxLvl()))
		{
			// Remove from waiting list
			PartyMatchWaitingList.getInstance().removePlayer(player);
			
			player.setPartyRoom(_roomId);
			
			player.sendPacket(new PartyMatchDetail(room));
			player.sendPacket(new ExPartyRoomMember(room, 0));
			
			for (Player member : room.getPartyMembers())
			{
				member.sendPacket(new ExManagePartyRoomMember(player, room, 0));
				member.sendPacket(SystemMessage.getSystemMessage(SystemMessageId.S1_ENTERED_PARTY_ROOM).addCharName(player));
			}
			room.addMember(player);
			
			// Info Broadcast
			player.broadcastUserInfo();
		}
		else
			player.sendPacket(SystemMessageId.CANT_ENTER_PARTY_ROOM);
	}
}