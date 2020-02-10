package net.sf.l2j.gameserver.scripting.scripts.events;

import java.util.LinkedList;
import java.util.List;

import net.sf.l2j.gameserver.data.SkillTable;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.network.serverpackets.MagicSkillUse;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;

/**
 * @author Rootware
 */
public class DeckTheHalls extends ScheduledQuest
{
	private static final String qn = "DeckTheHalls";
	
	private static final int EVENT_ID = 998;
	
	private static final int SANTA_TRAINEE_1 = 31863;
	private static final int SANTA_TRAINEE_2 = 31864;
	private static final int CHRISTMAS_TREE = 13006;
	
	private static final int HOLIDAY_WIND_WALK = 4262;
	private static final int HOLIDAY_HASTE = 4263;
	private static final int HOLIDAY_EMPOWER = 4264;
	private static final int HOLIDAY_MIGHT = 4265;
	private static final int HOLIDAY_SHIELD = 4266;
	
	private static final int STAR_ORNAMENT = 5556;
	private static final int BEAD_ORNAMENT = 5557;
	private static final int FIR_TREE_BRANCH = 5558;
	private static final int FLOWER_POT = 5559;
	private static final int CHRISTMAS_TREE_ITEM = 5560;
	private static final int SPECIAL_CHRISTMAS_TREE_ITEM = 5560;
	private static final int SANTAS_HAT = 7836;
	
	private static final int DROP_CHANCE_STAR_ORNAMENT = 40000;
	private static final int DROP_CHANCE_BEAD_ORNAMENT = 40000;
	private static final int DROP_CHANCE_FIR_TREE_BRANCH = 50000;
	private static final int DROP_CHANCE_FLOWER_POT = 20000;
	
	private final DropCategory _category;
	private final List<Npc> _spawns;
	
	private static final String MONSTER = "Monster";
	
	private static final SpawnLocation[] SANTA_TRAINEE_1_SPAWN =
	{
		new SpawnLocation(-44337, -113669, -224, 0),
		new SpawnLocation(-44628, -115409, -240, 22500),
		new SpawnLocation(11281, 15652, -4584, 25000),
		new SpawnLocation(11303, 17732, -4574, 57344),
		new SpawnLocation(47151, 49436, -3059, 32000),
		new SpawnLocation(44122, 50784, -3059, 57344),
		new SpawnLocation(114733, -178691, -821, 0),
		new SpawnLocation(115708, -182362, -1449, 0),
		new SpawnLocation(-84516, 242971, -3730, 34000),
		new SpawnLocation(-86003, 243205, -3730, 60000)
	};
	
	private static final SpawnLocation[] SANTA_TRAINEE_2_SPAWN =
	{
		new SpawnLocation(-13073, 122801, -3117, 0),
		new SpawnLocation(-13949, 121934, -2988, 32768),
		new SpawnLocation(-14786, 123686, -3117, 8192),
		new SpawnLocation(18178, 145149, -3054, 7400),
		new SpawnLocation(19208, 144380, -3097, 32768),
		new SpawnLocation(19508, 145775, -3086, 48000),
		new SpawnLocation(17396, 170259, -3507, 36000),
		new SpawnLocation(79806, 55570, -1560, 0),
		new SpawnLocation(83328, 55824, -1525, 32768),
		new SpawnLocation(80986, 54504, -1525, 32768),
		new SpawnLocation(83327, 149141, -3405, 32500),
		new SpawnLocation(82277, 148598, -3467, 0),
		new SpawnLocation(81621, 148725, -3467, 32768),
		new SpawnLocation(81680, 145656, -3467, 32768),
		new SpawnLocation(-80789, 151073, -3043, 28672),
		new SpawnLocation(-84049, 150176, -3129, 4096),
		new SpawnLocation(-82623, 151666, -3129, 49152),
		new SpawnLocation(117498, 76630, -2695, 38000),
		new SpawnLocation(115914, 76449, -2711, 59000),
		new SpawnLocation(119536, 76988, -2275, 40960),
		new SpawnLocation(147184, 27405, -2192, 17000),
		new SpawnLocation(147920, 25664, -2000, 16384),
		new SpawnLocation(111776, 221104, -3543, 16384),
		new SpawnLocation(107904, 218096, -3675, 0),
		new SpawnLocation(114920, 220020, -3632, 32768),
		new SpawnLocation(147888, -58048, -2979, 49000),
		new SpawnLocation(147262, -56450, -2776, 33000),
		new SpawnLocation(44176, -48732, -800, 33000),
		new SpawnLocation(44319, -47640, -792, 50000),
		new SpawnLocation(89730, -140810, -1536, 32768),
		new SpawnLocation(88740, -142740, -1336, 16384),
		new SpawnLocation(86060, -142670, -1336, 16384),
		new SpawnLocation(85310, -141360, -1536, 32768)
	};
	
	private static final SpawnLocation[] CHRISTMAS_TREE_SPAWN =
	{
		new SpawnLocation(-44342, -113726, -240, 0),
		new SpawnLocation(-44671, -115437, -240, 22500),
		new SpawnLocation(-13073, 122841, -3117, 0),
		new SpawnLocation(-13972, 121893, -2988, 32768),
		new SpawnLocation(-14843, 123710, -3117, 8192),
		new SpawnLocation(11327, 15682, -4584, 25000),
		new SpawnLocation(11243, 17712, -4574, 57344),
		new SpawnLocation(18154, 145192, -3054, 7400),
		new SpawnLocation(19214, 144327, -3097, 32768),
		new SpawnLocation(19459, 145775, -3086, 48000),
		new SpawnLocation(17418, 170217, -3507, 36000),
		new SpawnLocation(47146, 49382, -3059, 32000),
		new SpawnLocation(44157, 50827, -3059, 57344),
		new SpawnLocation(79798, 55629, -1560, 0),
		new SpawnLocation(83328, 55769, -1525, 32768),
		new SpawnLocation(80986, 54452, -1525, 32768),
		new SpawnLocation(83329, 149095, -3405, 49152),
		new SpawnLocation(82277, 148564, -3467, 0),
		new SpawnLocation(81620, 148689, -3464, 32768),
		new SpawnLocation(81691, 145610, -3467, 32768),
		new SpawnLocation(114719, -178742, -821, 0),
		new SpawnLocation(115708, -182422, -1449, 0),
		new SpawnLocation(-80731, 151152, -3043, 28672),
		new SpawnLocation(-84097, 150171, -3129, 4096),
		new SpawnLocation(-82678, 151666, -3129, 49152),
		new SpawnLocation(117459, 76664, -2695, 38000),
		new SpawnLocation(115936, 76488, -2711, 59000),
		new SpawnLocation(119576, 76940, -2275, 40960),
		new SpawnLocation(-84516, 243015, -3730, 34000),
		new SpawnLocation(-86031, 243153, -3730, 60000),
		new SpawnLocation(147124, 27401, -2192, 40960),
		new SpawnLocation(147985, 25664, -2000, 16384),
		new SpawnLocation(111724, 221111, -3543, 16384),
		new SpawnLocation(107899, 218149, -3675, 0),
		new SpawnLocation(114920, 220080, -3632, 32768),
		new SpawnLocation(147924, -58052, -2979, 49000),
		new SpawnLocation(147285, -56461, -2776, 11500),
		new SpawnLocation(44176, -48688, -800, 33000),
		new SpawnLocation(44294, -47642, -792, 50000),
		new SpawnLocation(89730, -140760, -1536, 32768),
		new SpawnLocation(88690, -142740, -1336, 16384),
		new SpawnLocation(86010, -142670, -1336, 16384),
		new SpawnLocation(85310, -141300, -1536, 32768)
	};
	
	public DeckTheHalls()
	{
		super(-1, "events");
		
		addFirstTalkId(SANTA_TRAINEE_1, SANTA_TRAINEE_2);
		addStartNpc(SANTA_TRAINEE_1, SANTA_TRAINEE_2);
		addTalkId(SANTA_TRAINEE_1, SANTA_TRAINEE_2);
		
		_category = new DropCategory(EVENT_ID);
		
		DropData data = new DropData(STAR_ORNAMENT, 1, 1, DROP_CHANCE_STAR_ORNAMENT);
		
		// drop bead ornament
		data = new DropData(BEAD_ORNAMENT, 1, 1, DROP_CHANCE_BEAD_ORNAMENT);
		
		// drop fir tre
		data = new DropData(FIR_TREE_BRANCH, 1, 1, DROP_CHANCE_FIR_TREE_BRANCH);
		
		// drop flower
		data = new DropData(FLOWER_POT, 1, 1, DROP_CHANCE_FLOWER_POT);
		
		_category.addDropData(data, false);
		_spawns = new LinkedList<>();
	}
	
	@Override
	public void onStart()
	{
		LOGGER.info("DeckTheHalls event has started.");
		World.announceToOnlinePlayers("Deck The Halls event has started.");
		
		for (SpawnLocation loc : SANTA_TRAINEE_1_SPAWN)
			_spawns.add(addSpawn(SANTA_TRAINEE_1, loc, false, 0, false));
		
		for (SpawnLocation loc : SANTA_TRAINEE_2_SPAWN)
			_spawns.add(addSpawn(SANTA_TRAINEE_2, loc, false, 0, false));
		
		for (SpawnLocation loc : CHRISTMAS_TREE_SPAWN)
			_spawns.add(addSpawn(CHRISTMAS_TREE, loc, false, 0, false));
		
		for (NpcTemplate template : NpcData.getInstance().getAllNpcs())
		{
			if (!template.isType(MONSTER))
				continue;
			
			template.addDropCategory(_category);
		}
	}
	
	@Override
	public void onEnd()
	{
		LOGGER.info("DeckTheHalls event has ended.");
		World.announceToOnlinePlayers("Deck The Halls event has ended.");
		
		for (Npc npc : _spawns)
			npc.deleteMe();
		
		_spawns.clear();
		
		for (NpcTemplate template : NpcData.getInstance().getAllNpcs())
		{
			if (!template.isType(MONSTER))
				continue;
			
			template.removeDropCategory(_category);
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		QuestState st = player.getQuestState(qn);
		if (st == null)
			st = newQuestState(player);
		
		return npc.getNpcId() + "-01.htm";
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;
		
		final int npcId = npc.getNpcId();
		if (event.equalsIgnoreCase("regular"))
		{
			if (st.getQuestItemsCount(STAR_ORNAMENT) > 3 && st.getQuestItemsCount(BEAD_ORNAMENT) > 3 && st.getQuestItemsCount(FIR_TREE_BRANCH) > 9 && st.hasQuestItems(FLOWER_POT))
			{
				st.takeItems(STAR_ORNAMENT, 4);
				st.takeItems(BEAD_ORNAMENT, 4);
				st.takeItems(FIR_TREE_BRANCH, 10);
				st.takeItems(FLOWER_POT, 1);
				st.giveItems(CHRISTMAS_TREE_ITEM, 1);
				htmltext = npcId + "-03.htm";
			}
			else
				htmltext = npcId + "-03a.htm";
		}
		else if (event.equalsIgnoreCase("special"))
		{
			if (st.getQuestItemsCount(CHRISTMAS_TREE_ITEM) > 9)
			{
				st.takeItems(CHRISTMAS_TREE_ITEM, 10);
				st.giveItems(SPECIAL_CHRISTMAS_TREE_ITEM, 1);
				htmltext = npcId + "-04.htm";
			}
			else
				htmltext = npcId + "-04a.htm";
		}
		else if (event.equalsIgnoreCase("windwalk"))
		{
			getBuff(npc, player, HOLIDAY_WIND_WALK);
			htmltext = npcId + "-06.htm";
		}
		else if (event.equalsIgnoreCase("haste"))
		{
			getBuff(npc, player, HOLIDAY_HASTE);
			htmltext = npcId + "-07.htm";
		}
		else if (event.equalsIgnoreCase("empower"))
		{
			getBuff(npc, player, HOLIDAY_EMPOWER);
			htmltext = npcId + "-08.htm";
		}
		else if (event.equalsIgnoreCase("might"))
		{
			getBuff(npc, player, HOLIDAY_MIGHT);
			htmltext = npcId + "-09.htm";
		}
		else if (event.equalsIgnoreCase("shield"))
		{
			getBuff(npc, player, HOLIDAY_SHIELD);
			htmltext = npcId + "-10.htm";
		}
		else if (event.equalsIgnoreCase("hat"))
		{
			if (st.getQuestItemsCount(CHRISTMAS_TREE_ITEM) > 9)
			{
				st.takeItems(CHRISTMAS_TREE_ITEM, 10);
				st.giveItems(SANTAS_HAT, 1);
				htmltext = npcId + "-12.htm";
			}
			else
				htmltext = npcId + "-13.htm";
		}
		
		return htmltext;
	}
	
	private static void getBuff(Npc npc, Player player, int skillId)
	{
		SkillTable.getInstance().getInfo(skillId, SkillTable.getInstance().getMaxLevel(skillId)).getEffects(npc, player);
		player.broadcastPacket(new MagicSkillUse(player, player, skillId, 1, 0, 0));
	}
}