package net.sf.l2j.gameserver.data.xml;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import net.sf.l2j.commons.data.xml.IXmlReader;
import net.sf.l2j.commons.util.StatsSet;

import org.w3c.dom.Document;

/**
 * @author Williams
 *
 */
public class DropDataCustom implements IXmlReader
{
	private final List<DropSystem> _drops = new ArrayList<>();
	
	@Override
	public void load()
	{
		parseFile("./data/xml/dropSystem.xml");
		LOGGER.info("Loaded {} Drop Custom.", _drops.size());
	}

	public DropDataCustom()
	{
		load();
	}
	
	public void reload()
	{
		_drops.clear();
		load();
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{	
		
		forEach(doc, "list", listNode -> forEach(listNode, "drops", node ->
		{
			final StatsSet set = parseAttributes(node);
			_drops.add(new DropSystem(set));
		}));
	}

	public List<DropSystem> getDrop()
	{
		return _drops;
	}

	public class DropSystem
	{
		private int _itemId;
		private int _max;
		private int _min;
		private int _chance;
		private int _category;
		
		public DropSystem(StatsSet set)
		{
			_category = set.getInteger("category");
			_itemId = set.getInteger("itemid");
			_max = set.getInteger("min");
			_min = set.getInteger("max");
			_chance = set.getInteger("chance");
		}
		
		public int getCategory()
		{
			return _category;
		}
		
		public int getItem()
		{
			return _itemId;
		}
		
		public int getMax()
		{
			return _max;
		}

		public int getMin()
		{
			return _min;
		}

		public int getChance()
		{
			return _chance;
		}
	}

	public static DropDataCustom getInstance()
	{
		return SingletonHolder._instance;
	}
	
	private static class SingletonHolder
	{
		protected static final DropDataCustom _instance = new DropDataCustom();
	}
}