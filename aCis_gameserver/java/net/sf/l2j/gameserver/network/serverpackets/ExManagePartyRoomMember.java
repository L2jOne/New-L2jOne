package net.sf.l2j.gameserver.network.serverpackets;

import net.sf.l2j.gameserver.data.xml.MapRegionData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.partymatching.PartyMatchRoom;

public class ExManagePartyRoomMember extends L2GameServerPacket
{
	private final Player _player;
	private final PartyMatchRoom _room;
	private final int _mode;
	
	public ExManagePartyRoomMember(Player player, PartyMatchRoom room, int mode)
	{
		_player = player;
		_room = room;
		_mode = mode;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x10);
		
		writeD(_mode);
		writeD(_player.getObjectId());
		writeS(_player.getName());
		writeD(_player.getActiveClass());
		writeD(_player.getLevel());
		writeD(MapRegionData.getInstance().getClosestLocation(_player.getX(), _player.getY()));
		
		if (_room.getOwner().equals(_player))
			writeD(1);
		else
		{
			if ((_room.getOwner().isInParty() && _player.isInParty()) && (_room.getOwner().getParty().getLeaderObjectId() == _player.getParty().getLeaderObjectId()))
				writeD(2);
			else
				writeD(0);
		}
	}
}