package net.sf.l2j.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoomList;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchWaitingList;

public class ExListPartyMatchingWaitingRoom extends L2GameServerPacket
{
	private final Player _player;
	@SuppressWarnings("unused")
	private final int _page;
	private final int _minlvl;
	private final int _maxlvl;
	private final int _mode;
	private final List<Player> _members;
	
	public ExListPartyMatchingWaitingRoom(Player player, int page, int minlvl, int maxlvl, int mode)
	{
		_player = player;
		_page = page;
		_minlvl = minlvl;
		_maxlvl = maxlvl;
		_mode = mode;
		_members = new ArrayList<>();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x35);
		
		// If the mode is 0 and the player isn't the PartyRoom leader, return an empty list.
		if (_mode == 0)
		{
			// Retrieve the player PartyMatchRoom
			final PartyMatchRoom room = PartyMatchRoomList.getInstance().getRoom(_player.getPartyRoom());
			if (room == null || !room.getOwner().equals(_player))
			{
				writeD(0);
				writeD(0);
				return;
			}
		}
		
		for (Player member : PartyMatchWaitingList.getInstance().getPlayers())
		{
			// Don't add yourself in the list
			if (member == null || member == _player)
				continue;
			
			if (member.getLevel() < _minlvl || member.getLevel() > _maxlvl)
				continue;
			
			_members.add(member);
		}
		
		writeD(1);
		writeD(_members.size());
		for (Player member : _members)
		{
			writeS(member.getName());
			writeD(member.getActiveClass());
			writeD(member.getLevel());
		}
	}
}