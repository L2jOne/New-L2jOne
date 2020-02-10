package net.sf.l2j.gameserver.scripting.scripts.events;

import java.util.LinkedList;
import java.util.List;

import net.sf.l2j.commons.random.Rnd;

import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.World;
import net.sf.l2j.gameserver.model.actor.Npc;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.model.location.SpawnLocation;
import net.sf.l2j.gameserver.scripting.QuestState;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;

/**
 * @author Hasha
 *
 */
public class HeavyMedal extends ScheduledQuest
{
	private static final String qn = "HeavyMedal";
	
	private static final int ROY_THE_CAT = 31228;
	private static final int WINNIE_THE_CAT = 31229;
	
	private static final int EVENT_MEDAL = 6392;
	private static final int GLITTERING_MEDAL = 6393;
	
	private static final int CHANCE_WIN = 50;
	private static final int CHANCE_DROP_EVENT_MEDAL = 50000; // 5% (1.000.000 is 100%)
	private static final int CHANCE_DROP_GLITTERING_MEDAL = 5000; // 0,5% (1.000.000 is 100%)
	private final DropCategory _category;
	private final List<Npc> _spawns;
	
	private static final String MONSTER = "Monster";
	
	private static final int[] GLITTERING_MEDAL_COUNT =
	{
		5,
		10,
		20,
		40
	};
	
	private static final int[] BADGES =
	{
		6399,
		6400,
		6401,
		6402
	};
	
	private static final SpawnLocation[] WINNIE_SPAWN =
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
		new SpawnLocation(147285, -56461, -2776, 33000),
		new SpawnLocation(44176, -48688, -800, 33000),
		new SpawnLocation(44294, -47642, -792, 50000)
	};
	
	private static final SpawnLocation[] ROY_SPAWN =
	{
		new SpawnLocation(-44337, -113669, -224, 0),
		new SpawnLocation(-44628, -115409, -240, 22500),
		new SpawnLocation(-13073, 122801, -3117, 0),
		new SpawnLocation(-13949, 121934, -2988, 32768),
		new SpawnLocation(-14786, 123686, -3117, 8192),
		new SpawnLocation(11281, 15652, -4584, 25000),
		new SpawnLocation(11303, 17732, -4574, 57344),
		new SpawnLocation(18178, 145149, -3054, 7400),
		new SpawnLocation(19208, 144380, -3097, 32768),
		new SpawnLocation(19508, 145775, -3086, 48000),
		new SpawnLocation(17396, 170259, -3507, 36000),
		new SpawnLocation(47151, 49436, -3059, 32000),
		new SpawnLocation(44122, 50784, -3059, 57344),
		new SpawnLocation(79806, 55570, -1560, 0),
		new SpawnLocation(83328, 55824, -1525, 32768),
		new SpawnLocation(80986, 54504, -1525, 32768),
		new SpawnLocation(83332, 149160, -3405, 49152),
		new SpawnLocation(82277, 148598, -3467, 0),
		new SpawnLocation(81621, 148725, -3467, 32768),
		new SpawnLocation(81680, 145656, -3467, 32768),
		new SpawnLocation(114733, -178691, -821, 0),
		new SpawnLocation(115708, -182362, -1449, 0),
		new SpawnLocation(-80789, 151073, -3043, 28672),
		new SpawnLocation(-84049, 150176, -3129, 4096),
		new SpawnLocation(-82623, 151666, -3129, 49152),
		new SpawnLocation(117498, 76630, -2695, 38000),
		new SpawnLocation(115914, 76449, -2711, 59000),
		new SpawnLocation(119536, 76988, -2275, 40960),
		new SpawnLocation(-84516, 242971, -3730, 34000),
		new SpawnLocation(-86003, 243205, -3730, 60000),
		new SpawnLocation(147184, 27405, -2192, 17000),
		new SpawnLocation(147920, 25664, -2000, 16384),
		new SpawnLocation(111776, 221104, -3543, 16384),
		new SpawnLocation(107904, 218096, -3675, 0),
		new SpawnLocation(114920, 220020, -3632, 32768),
		new SpawnLocation(147888, -58048, -2979, 49000),
		new SpawnLocation(147262, -56450, -2776, 33000),
		new SpawnLocation(44176, -48732, -800, 33000),
		new SpawnLocation(44319, -47640, -792, 50000)
	};
	
	public HeavyMedal()
	{
		super(-1, "events");
		
		addFirstTalkId(ROY_THE_CAT, WINNIE_THE_CAT);
		addStartNpc(ROY_THE_CAT, WINNIE_THE_CAT);
		addTalkId(ROY_THE_CAT, WINNIE_THE_CAT);
		
		// create drop category with unique ID (event medal)
		_category = new DropCategory(EVENT_MEDAL);
		
		// add Event Medal drop
		DropData data = new DropData(EVENT_MEDAL, 1, 1, CHANCE_DROP_EVENT_MEDAL);
		_category.addDropData(data, false);
		
		// add Glittering Medal drop
		data = new DropData(GLITTERING_MEDAL, 1, 1, CHANCE_DROP_GLITTERING_MEDAL);
		_category.addDropData(data, false);
		
		_spawns = new LinkedList<>();
	}
	
	@Override
	public void onStart()
	{
		LOGGER.info("HeavyMedal event has started.");
		World.announceToOnlinePlayers("Heavy medal event has started.");
		
		// spawn Winnie the Cat
		for (SpawnLocation loc : WINNIE_SPAWN)
			_spawns.add(addSpawn(WINNIE_THE_CAT, loc, false, 0, false));
			
		// spawn Roy the Cat
		for (SpawnLocation loc : ROY_SPAWN)
			_spawns.add(addSpawn(ROY_THE_CAT, loc, false, 0, false));
		
		// add medal drop to every monster
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
		LOGGER.info("HeavyMedal event has ended.");
		World.announceToOnlinePlayers("Heavy medal event has ended.");
		
		// despawn Winnie the Cat and Roy the Cat
		for (Npc npc : _spawns)
			npc.deleteMe();
		
		_spawns.clear();
		
		// remove medal drop from every monster
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
			
		return npc.getNpcId() + ".htm";
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		QuestState st = player.getQuestState(qn);
		int level = checkLevel(st);
		
		if (event.equalsIgnoreCase("game"))
		{
			if (st.getQuestItemsCount(GLITTERING_MEDAL) < GLITTERING_MEDAL_COUNT[level])
				return "31229-no.htm";
				
			return "31229-game.htm";
		}
		else if (event.equalsIgnoreCase("heads") || event.equalsIgnoreCase("tails"))
		{
			if (st.getQuestItemsCount(GLITTERING_MEDAL) < GLITTERING_MEDAL_COUNT[level])
				return "31229-" + event.toLowerCase() + "-10.htm";
				
			if (level < 4)
			{
				st.takeItems(GLITTERING_MEDAL, GLITTERING_MEDAL_COUNT[level]);
				
				if (Rnd.get(100) < CHANCE_WIN)
				{
					if (level > 0)
						st.takeItems(BADGES[level - 1], -1);
					
					st.giveItems(BADGES[level], 1);
					st.playSound(QuestState.SOUND_ITEMGET);
					level++;
				}
				else
					level = 0;
					
				return "31229-" + event + "-" + level + ".htm";
			}
		}
		else if (event.equalsIgnoreCase("talk"))
		{
			return npc.getNpcId() + "-lvl-" + level + ".htm";
		}
		return event;
	}
	
	private static final int checkLevel(QuestState st)
	{
		if (st == null)
			return 0;
		
		if (st.getQuestItemsCount(6402) > 0)
			return 4;
		
		if (st.getQuestItemsCount(6401) > 0)
			return 3;
		
		if (st.getQuestItemsCount(6400) > 0)
			return 2;
		
		if (st.getQuestItemsCount(6399) > 0)
			return 1;
		
		return 0;
	}
}