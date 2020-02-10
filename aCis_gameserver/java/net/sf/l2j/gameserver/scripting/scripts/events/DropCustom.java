package net.sf.l2j.gameserver.scripting.scripts.events;

import net.sf.l2j.gameserver.data.xml.DropDataCustom;
import net.sf.l2j.gameserver.data.xml.DropDataCustom.DropSystem;
import net.sf.l2j.gameserver.data.xml.NpcData;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.item.DropCategory;
import net.sf.l2j.gameserver.model.item.DropData;
import net.sf.l2j.gameserver.scripting.ScheduledQuest;

/**
 * @author Williams
 *
 */
public class DropCustom extends ScheduledQuest
{
	private DropCategory _category;
	
	public DropCustom()
	{
		super(-1, "custom");
		
		for (DropSystem drop : DropDataCustom.getInstance().getDrop())	
		{

			// create drop category with unique ID (event medal)
			_category = new DropCategory(drop.getItem());
			
			// add Event Medal drop
			DropData data = new DropData(drop.getItem(), drop.getMin(), drop.getMax(), drop.getChance());
			_category.addDropData(data, false);
		}
	}
	
	@Override
	protected void onStart()
	{
		for (NpcTemplate template : NpcData.getInstance().getAllNpcs())
		{
			if (!template.isType("Monster"))
				continue;
			
			template.addDropCategory(_category);
		}
	}
	
	@Override
	protected void onEnd()
	{
		for (NpcTemplate template : NpcData.getInstance().getAllNpcs())
		{
			if (!template.isType("Monster"))
				continue;
			
			template.removeDropCategory(_category);
		}
	}	
}