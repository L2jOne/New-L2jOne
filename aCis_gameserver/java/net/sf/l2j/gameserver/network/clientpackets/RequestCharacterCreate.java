package net.sf.l2j.gameserver.network.clientpackets;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.sql.PlayerInfoTable;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.data.xml.PlayerData;
import net.sf.l2j.gameserver.data.xml.PlayerLevelData;
import net.sf.l2j.gameserver.data.xml.ScriptData;
import net.sf.l2j.gameserver.enums.ShortcutType;
import net.sf.l2j.gameserver.enums.actors.Sex;
import net.sf.l2j.gameserver.handler.admincommandhandlers.AdminVip;
import net.sf.l2j.gameserver.idfactory.IdFactory;
import net.sf.l2j.gameserver.model.Shortcut;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.PlayerTemplate;
import net.sf.l2j.gameserver.model.holder.ItemTemplateHolder;
import net.sf.l2j.gameserver.model.holder.skillnode.GeneralSkillNode;
import net.sf.l2j.gameserver.model.item.instance.ItemInstance;
import net.sf.l2j.gameserver.model.item.kind.Item;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateFail;
import net.sf.l2j.gameserver.network.serverpackets.CharCreateOk;
import net.sf.l2j.gameserver.network.serverpackets.CharSelectInfo;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;
import net.sf.l2j.gameserver.scripting.Quest;

@SuppressWarnings("unused")
public final class RequestCharacterCreate extends L2GameClientPacket
{
	private static final String ACTIVED = "<font color=00FF00>ON</font>";
	private static final String DESATIVED = "<font color=FF0000>OFF</font>";
	
	private String _name;
	private int _race;
	private byte _sex;
	private int _classId;
	private int _int;
	private int _str;
	private int _con;
	private int _men;
	private int _dex;
	private int _wit;
	private byte _hairStyle;
	private byte _hairColor;
	private byte _face;
	
	@Override
	protected void readImpl()
	{
		_name = readS();
		_race = readD();
		_sex = (byte) readD();
		_classId = readD();
		_int = readD();
		_str = readD();
		_con = readD();
		_men = readD();
		_dex = readD();
		_wit = readD();
		_hairStyle = (byte) readD();
		_hairColor = (byte) readD();
		_face = (byte) readD();
	}
	
	@Override
	protected void runImpl()
	{
		// Invalid race.
		if (_race > 4 || _race < 0)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid face.
		if (_face > 2 || _face < 0)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid hair style.
		if (_hairStyle < 0 || (_sex == 0 && _hairStyle > 4) || (_sex != 0 && _hairStyle > 6))
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid hair color.
		if (_hairColor > 3 || _hairColor < 0)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Invalid name typo.
		if (!StringUtil.isValidString(_name, "^[A-Za-z0-9]{1,16}$"))
		{
			sendPacket(CharCreateFail.REASON_INCORRECT_NAME);
			return;
		}
		
		// Your name is already taken by a NPC.
		if (NpcData.getInstance().getTemplateByName(_name) != null)
		{
			sendPacket(CharCreateFail.REASON_INCORRECT_NAME);
			return;
		}
		
		// You already have the maximum amount of characters for this account.
		if (PlayerInfoTable.getInstance().getCharactersInAcc(getClient().getAccountName()) >= 7)
		{
			sendPacket(CharCreateFail.REASON_TOO_MANY_CHARACTERS);
			return;
		}
		
		// The name already exists.
		if (PlayerInfoTable.getInstance().getPlayerObjectId(_name) > 0)
		{
			sendPacket(CharCreateFail.REASON_NAME_ALREADY_EXISTS);
			return;
		}
		
		// The class id related to this template is post-newbie.
		final PlayerTemplate template = PlayerData.getInstance().getTemplate(_classId);
		if (template == null || template.getClassBaseLevel() > 1)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Create the player Object.
		final Player player = Player.create(IdFactory.getInstance().getNextId(), template, getClient().getAccountName(), _name, _hairStyle, _hairColor, _face, Sex.values()[_sex]);
		if (player == null)
		{
			sendPacket(CharCreateFail.REASON_CREATION_FAILED);
			return;
		}
		
		// Set default values.
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		
		// send acknowledgement
		sendPacket(CharCreateOk.STATIC_PACKET);
		
		World.getInstance().addObject(player);
		
		// Start Spawn location
		player.getPosition().set(template.getRandomSpawn());
		
		// Start Title
		player.setTitle(template.getTitle());

		// Start Lvl and Sp
		player.getStat().addExpAndSp(PlayerLevelData.getInstance().getPlayerLevel(template.getLevel()).getRequiredExpToLevelUp(), template.getSp());

		// Start buffs
		for (int buffId : template.getBuffIds())
		{
			if (template.isBuffIds())
				SkillTable.getInstance().getInfo(buffId, SkillTable.getInstance().getMaxLevel(buffId)).getEffects(player, player);
		}

		// Add vip
		if (template.getVip() > 0)
			AdminVip.doVip(player, template.getVip());

		// show info
		if (template.isShow())
		{	
			final NpcHtmlMessage html = new NpcHtmlMessage(0);
			final StringBuilder sbItems = new StringBuilder();
			
			html.setFile("data/html/newchar.htm");
			html.replace("%name%", player.getName());
			html.replace("%vip%", player.getTemplate().getVip());
			html.replace("%level%", player.getTemplate().getLevel());
			html.replace("%sp%", player.getTemplate().getSp());
			html.replace("%buff%", player.getTemplate().isBuffIds() ? ACTIVED : DESATIVED);
			
			for (ItemTemplateHolder holder : player.getTemplate().getItems())
			{
				final Item item = ItemData.getInstance().getTemplate(holder.getId());
				sbItems.append("<table width=280 bgcolor=000000><tr>");
				sbItems.append("<td width=44 height=41 align=center><table bgcolor=FFFFFF cellpadding=6 cellspacing=\"-5\"><tr><td><button width=32 height=32 back=" + item.getIcon() + " fore=" + item.getIcon() + "></td></tr></table></td>");
				sbItems.append("<td width=236>" + item.getName() + "<br1><font color=B09878>Item Amount : " + holder.getValue() + "</font><br1><font color=B09878>Enchant Lvel : " + holder.getEnchant() + "</font></td>");
				sbItems.append("</tr></table><img src=L2UI.SquareGray width=280 height=1>");
			}
			
			html.replace("%items%", sbItems.toString());
			getClient().sendPacket(html);
		}
		
		// Register shortcuts.
		player.getShortcutList().addShortcut(new Shortcut(0, 0, ShortcutType.ACTION, 2, -1, 1)); // attack shortcut
		player.getShortcutList().addShortcut(new Shortcut(3, 0, ShortcutType.ACTION, 5, -1, 1)); // take shortcut
		player.getShortcutList().addShortcut(new Shortcut(10, 0, ShortcutType.ACTION, 0, -1, 1)); // sit shortcut
		
		// Equip or add items, based on template.
		for (ItemTemplateHolder holder : template.getItems())
		{
			final ItemInstance item = player.getInventory().addItem("Init", holder.getId(), holder.getValue(), player, null);
			
			// Tutorial book shortcut.
			if (holder.getId() == 5588)
				player.getShortcutList().addShortcut(new Shortcut(11, 0, ShortcutType.ITEM, item.getObjectId(), -1, 1));

			if (item.isEquipable() && holder.isEquipped() && holder.getEnchant() > 0)
			{
				player.getInventory().equipItemAndRecord(item);
				item.setEnchantLevel(holder.getEnchant());
			}
		}
		
		// Add skills.
		for (GeneralSkillNode skill : player.getAvailableAutoGetSkills())
		{
			if (skill.getId() == 1001 || skill.getId() == 1177)
				player.getShortcutList().addShortcut(new Shortcut(1, 0, ShortcutType.SKILL, skill.getId(), 1, 1), false);
			
			if (skill.getId() == 1216)
				player.getShortcutList().addShortcut(new Shortcut(9, 0, ShortcutType.SKILL, skill.getId(), 1, 1), false);
		}
		
		// Tutorial runs here.
		if (!Config.DISABLE_TUTORIAL)
		{
			if (player.getQuestState("Tutorial") == null)
			{
				final Quest quest = ScriptData.getInstance().getQuest("Tutorial");
				if (quest != null)
					quest.newQuestState(player).setState(Quest.STATE_STARTED);
			}
		}
		
		player.setOnlineStatus(true, false);
		player.deleteMe();
		
		final CharSelectInfo csi = new CharSelectInfo(getClient().getAccountName(), getClient().getSessionId().playOkID1);
		sendPacket(csi);
		getClient().setCharSelectSlot(csi.getCharacterSlots());
	}
}