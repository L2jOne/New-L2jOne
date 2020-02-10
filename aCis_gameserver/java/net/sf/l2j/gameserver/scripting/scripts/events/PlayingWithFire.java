package net.sf.l2j.gameserver.scripting.scripts.events;

import java.util.LinkedList;
import java.util.List;

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
 * @author Williams
 *
 */
public class PlayingWithFire extends ScheduledQuest
{
	private static final String qn = "PlayingWithFire";
	
	private static final int EVENT_ID = 995;
	
	private static final int BUZZ_THE_CAT = 31227;
	
	private static final int ELVEN_FIRECRACKER = 6403;
	private static final int GUNPOWRED = 6404;
	private static final int MAGNESIUM = 6405;
	private static final int FIREWORK = 6406;
	private static final int LARGE_FIREWORK = 6407;
	
	private static final int CHANCE_DROP_EVENT_ELVEN_FIRECRACKER = 50000;
	private static final int CHANCE_DROP_EVENT_GUNPOWRED = 30000;
	private static final int CHANCE_DROP_EVENT_MAGNESIUM = 10000;
	
	private final DropCategory _category;
	private final List<Npc> _spawns;
	
	private static final String MONSTER = "Monster";
	
	private static final SpawnLocation[] BUZZ_SPAWN =
	{
		new SpawnLocation(-44162, -112238, -240, 41000),
		new SpawnLocation(-14796, 123305, -3112, 0),
		new SpawnLocation(-14295, 122438, -3104, 16000),
		new SpawnLocation(-13065, 122916, -3112, 49000),
		new SpawnLocation(11658, 16027, -4568, 22000),
		new SpawnLocation(19421, 145801, -3080, 48000),
		new SpawnLocation(15849, 142855, -2696, 15500),
		new SpawnLocation(17240, 170540, -3496, 54000),
		new SpawnLocation(43051, 50108, -2992, 63500),
		new SpawnLocation(83185, 53480, -1448, 32000),
		new SpawnLocation(80225, 55052, -1552, 0),
		new SpawnLocation(83362, 55473, -1520, 32000),
		new SpawnLocation(83691, 148386, -3400, 32000),
		new SpawnLocation(80239, 146832, -3528, 0),
		new SpawnLocation(81923, 148916, -3464, 16000),
		new SpawnLocation(114891, -178149, -832, 0),
		new SpawnLocation(-81435, 151717, -3120, 48000),
		new SpawnLocation(-83118, 150925, -3120, 0),
		new SpawnLocation(117368, 76729, -2688, 44000),
		new SpawnLocation(115626, 75730, -2592, 5000),
		new SpawnLocation(-84019, 242875, -3728, 59000),
		new SpawnLocation(147450, 27763, -2264, 16000),
		new SpawnLocation(149779, 25431, -2136, 0),
		new SpawnLocation(147231, 29931, -2456, 16000),
		new SpawnLocation(111262, 219619, -3664, 16000),
		new SpawnLocation(111591, 223109, -3672, 48000),
		new SpawnLocation(147888, -58048, -2979, 49000),
		new SpawnLocation(147285, -56461, -2776, 11500),
		new SpawnLocation(44176, -48732, -800, 33000),
		new SpawnLocation(44294, -47642, -792, 50000)
	};
	
	public PlayingWithFire()
	{
		super(-1, "events");
		
		addFirstTalkId(BUZZ_THE_CAT);
		addStartNpc(BUZZ_THE_CAT);
		addTalkId(BUZZ_THE_CAT);
		
		_category = new DropCategory(EVENT_ID);
		
		// drop elven
		DropData data = new DropData(ELVEN_FIRECRACKER, 1, 1, CHANCE_DROP_EVENT_ELVEN_FIRECRACKER);
		//drop gunpowred
		data = new DropData(GUNPOWRED, 1, 1, CHANCE_DROP_EVENT_GUNPOWRED);
		//drop magnesium
		data = new DropData(MAGNESIUM, 1, 1, CHANCE_DROP_EVENT_MAGNESIUM);
		
		_category.addDropData(data, false);
		_spawns = new LinkedList<>();
	}
	
	@Override
	public void onStart()
	{
		LOGGER.info("PlayingWithFire event has started.");
		World.announceToOnlinePlayers("Playing with Fire event has started.");
		
		for (SpawnLocation loc : BUZZ_SPAWN)
			_spawns.add(addSpawn(BUZZ_THE_CAT, loc, false, 0, false));
		
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
		LOGGER.info("PlayingWithFire event has ended.");
		World.announceToOnlinePlayers("Playing with Fire event has ended.");
		
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
		
		return "31227-01.htm";
	}
	
	@Override
	public String onAdvEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		QuestState st = player.getQuestState(qn);
		if (st == null)
			return null;
		
		if (event.equalsIgnoreCase("regular"))
		{
			if (st.getQuestItemsCount(ELVEN_FIRECRACKER) > 1 && st.getQuestItemsCount(GUNPOWRED) > 1)
			{
				st.takeItems(ELVEN_FIRECRACKER, 2);
				st.takeItems(GUNPOWRED, 2);
				st.giveItems(FIREWORK, 1);
				htmltext = "31227-04.htm";
			}
			else
				htmltext = "31227-06.htm";
		}
		else if (event.equalsIgnoreCase("large"))
		{
			if (st.getQuestItemsCount(ELVEN_FIRECRACKER) > 3 && st.getQuestItemsCount(GUNPOWRED) > 3 && st.hasQuestItems(MAGNESIUM))
			{
				st.takeItems(ELVEN_FIRECRACKER, 4);
				st.takeItems(GUNPOWRED, 4);
				st.takeItems(MAGNESIUM, 1);
				st.giveItems(LARGE_FIREWORK, 1);
				htmltext = "31227-05.htm";
			}
			else
				htmltext = "31227-06.htm";
		}
		
		return htmltext;
	}
}