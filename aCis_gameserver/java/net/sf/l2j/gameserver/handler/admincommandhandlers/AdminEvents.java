package net.sf.l2j.gameserver.handler.admincommandhandlers;

import java.util.Map;
import java.util.StringTokenizer;

import net.sf.l2j.gameserver.engine.Event;
import net.sf.l2j.gameserver.engine.EventConfig;
import net.sf.l2j.gameserver.engine.EventManager;
import net.sf.l2j.gameserver.engine.events.Simon;
import net.sf.l2j.gameserver.handler.IAdminCommandHandler;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

public class AdminEvents implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_events",
		"admin_enableEvent",
		"admin_configureEvent",
		"admin_event_set",
		"admin_event_start",
		"admin_event_finish"
	};
	
	@Override
	public boolean useAdminCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_events"))
			listEvents(activeChar);
		else if (command.startsWith("admin_enableEvent"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				int eid = Integer.parseInt(st.nextToken());
				if (EventManager.getInstance().getCurrentEvent() != null && EventManager.getInstance().getCurrentEvent().getInt("ids") == eid)
					activeChar.sendMessage("Cannot disable a running event");
				else
				{
					EventManager.getInstance().enableEvent(eid, Integer.parseInt(st.nextToken()));
					listEvents(activeChar);
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Wrong parameters given. syntax is enableEvent [eventId] [0 to disable or 1 to enable]");
			}
		}
		else if (command.startsWith("admin_configureEvent"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				showMainPage(activeChar, Integer.parseInt(st.nextToken()));
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Wrong parameters given. syntax is admin_configureEvent [eventId]");
			}
		}
		else if (command.startsWith("admin_event_set"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			try
			{
				st.nextToken();
				int eid = Integer.parseInt(st.nextToken());
				EventConfig.getInstance().addProperty(eid, st.nextToken(), st.nextToken());
				showMainPage(activeChar, eid);
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Wrong parameters given.");
			}
		}
		else if (command.startsWith("admin_event_start"))
		{
			if (EventManager.getInstance().getCurrentEvent() != null)
				activeChar.sendMessage("An event is already running.");
			else
			{
				StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				EventManager.getInstance().manualStart(Integer.parseInt(st.nextToken()));
			}
		}
		else if (command.startsWith("admin_event_finish"))
		{
			if (EventManager.getInstance().getCurrentEvent() != null)
				EventManager.getInstance().manualStop();
			else
				activeChar.sendMessage("No event currently running.");
		}
		return true;
	}
	
	@Override
	public String[] getAdminCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	private static void listEvents(Player activeChar)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(0);
		StringBuilder tb = new StringBuilder();
		int count = 0;
		
		tb.append("<html><body>Edit the following events as you wish - <a action=\"bypass -h admin_configureEvent 0\">General Configs</a><br>");

		Map<Integer, String> events = EventManager.getInstance().getEventMap();
		for (int id : events.keySet())
		{
			count++;
			tb.append("<center><table width=270 " + (count % 2 == 1 ? "" : "bgcolor=5A5A5A") + "><tr><td width=145>" + events.get(id) + "</td><td width=60>" + (EventManager.getInstance().isEnabled(id) ? "<a action=\"bypass -h admin_enableEvent " + id + " 0\">Disable</a>" : "<a action=\"bypass -h admin_enableEvent " + id + " 1\">Enable</a>") + "</td><td width=65><a action=\"bypass -h admin_configureEvent " + id + "\">Configure</a></td></tr></table>");
		}
		
		tb.append("</body></html>");
		html.setHtml(tb.toString());
		activeChar.sendPacket(html);
	}
	
	private static void showMainPage(Player activeChar, int eventId)
	{
		NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		StringBuilder replyMSG = new StringBuilder();
		
		if (eventId == 0)
		{
			EventManager e = EventManager.getInstance();
			
			replyMSG.append("<html><title>General Configs</title><body>");
			replyMSG.append("<center><font color=\"LEVEL\">[General Configs]</font></center><br><br>");
			replyMSG.append("<table><tr><td><edit var=\"input\" width=\"190\"></td></tr></table><br>");
			replyMSG.append("<table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Register Time\" action=\"bypass -h admin_event_set 0 registerTime $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Between Time\" action=\"bypass -h admin_event_set 0 betweenEventsTime $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Voting Enabled\" action=\"bypass -h admin_event_set 0 votePopupEnabled " + (e.getBoolean("votePopupEnabled") ? "false" : "true") + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Show Vote At\" action=\"bypass -h admin_event_set 0 showVotePopupAt $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Debug Enabled\" action=\"bypass -h admin_event_set 0 debug " + (e.getBoolean("debug") ? "false" : "true") + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Tracking Enabled\" action=\"bypass -h admin_event_set 0 statTrackingEnabled " + (e.getBoolean("statTrackingEnabled") ? "false" : "true") + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Buffer Enabled\" action=\"bypass -h admin_event_set 0 eventBufferEnabled " + (e.getBoolean("eventBufferEnabled") ? "false" : "true") + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Buff Number\" action=\"bypass -h admin_event_set 0 maxBuffNum $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Dualbox Protection\" action=\"bypass -h admin_event_set 0 dualboxAllowed " + (e.getBoolean("dualboxAllowed") ? "false" : "true") + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Anti AFK Time\" action=\"bypass -h admin_event_set 0 antiAfkTime $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Anti AFK Tries\" action=\"bypass -h admin_event_set 0 antiAfkDisallowAfter $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Anti AFK Block Time\" action=\"bypass -h admin_event_set 0 antiAfkDisallowTime $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><br>");
			replyMSG.append("<center><font color=\"LEVEL\">[Current Configs]</font></center><br>");
			replyMSG.append("Register Time:&nbsp;<font color=\"00FF00\">" + e.getInt("registerTime") + "</font><br>");
			replyMSG.append("Between Events Time:&nbsp;<font color=\"00FF00\">" + e.getInt("betweenEventsTime") + "</font><br>");
			replyMSG.append("Voting Enabled:&nbsp;<font color=\"00FF00\">" + e.getString("votePopupEnabled") + "</font><br>");
			replyMSG.append("Show Vote At:&nbsp;<font color=\"00FF00\">" + e.getInt("showVotePopupAt") + "</font><br>");
			replyMSG.append("Debug Enabled:&nbsp;<font color=\"00FF00\">" + e.getString("debug") + "</font><br>");
			replyMSG.append("Stat Tracking Enabled:&nbsp;<font color=\"00FF00\">" + e.getString("statTrackingEnabled") + "</font><br>");
			replyMSG.append("Buffer Enabled:&nbsp;<font color=\"00FF00\">" + e.getString("eventBufferEnabled") + "</font><br>");
			replyMSG.append("Buff Max Amount:&nbsp;<font color=\"00FF00\">" + e.getInt("maxBuffNum") + "</font><br>");
			replyMSG.append("Dualbox Protection Disabled:&nbsp;<font color=\"00FF00\">" + e.getString("dualboxAllowed") + "</font><br>");
			replyMSG.append("Anti AFK Time:&nbsp;<font color=\"00FF00\">" + e.getInt("antiAfkTime") + "</font><br>");
			replyMSG.append("Anti AFK Block After:&nbsp;<font color=\"00FF00\">" + e.getInt("antiAfkDisallowAfter") + "</font><br>");
			replyMSG.append("Anti AFK Block Time:&nbsp;<font color=\"00FF00\">" + e.getInt("antiAfkDisallowTime") + "</font><br>");
		}
		else
		{
			Event e = EventManager.getInstance().getEvent(eventId);
			
			if (e == null)
			{
				activeChar.sendMessage("No event with id " + eventId);
				return;
			}
			
			replyMSG.append("<html><title>" + e.getString("eventName") + "</title><body>");
			replyMSG.append("<center><font color=\"LEVEL\">[" + e.getString("eventName") + "]</font></center><br><br>");
			replyMSG.append("<table><tr><td><edit var=\"input\" width=\"190\"></td></tr></table><br>");
			replyMSG.append("<table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Min Lvl\" action=\"bypass -h admin_event_set " + eventId + " minLvl $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Max Lvl\" action=\"bypass -h admin_event_set " + eventId + " maxLvl $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Reward ID\" action=\"bypass -h admin_event_set " + eventId + " rewardId $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Reward Amount\" action=\"bypass -h admin_event_set " + eventId + " rewardAmmount $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			if (e instanceof Simon)
				replyMSG.append("<td width=\"100\"><button value=\"Round Time\" action=\"bypass -h admin_event_set " + eventId + " roundTime $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			else
				replyMSG.append("<td width=\"100\"><button value=\"Event Time\" action=\"bypass -h admin_event_set " + eventId + " matchTime $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Min Players\" action=\"bypass -h admin_event_set " + eventId + " minPlayers $input\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Allow Magic\" action=\"bypass -h admin_event_set " + eventId + " allowUseMagic " + (e.getBoolean("allowUseMagic") ? "false" : "true") + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Remove Buffs\" action=\"bypass -h admin_event_set " + eventId + " removeBuffs " + (e.getBoolean("removeBuffs") ? "false" : "true") + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><table><tr>");
			replyMSG.append("<td width=\"100\"><button value=\"Start\" action=\"bypass -h admin_event_start " + eventId + "\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("<td width=\"100\"><button value=\"Finish\" action=\"bypass -h admin_event_finish\" width=90 height=15 back=\"sek.cbui94\" fore=\"sek.cbui92\"></td>");
			replyMSG.append("</tr></table><br><br>");
			replyMSG.append("<center><font color=\"LEVEL\">[Current Configs]</font></center><br>");
			replyMSG.append("Name:&nbsp;<font color=\"00FF00\">" + e.getString("eventName") + "</font><br>");
			replyMSG.append("Reward ID:&nbsp;<font color=\"00FF00\">" + e.getInt("rewardId") + "</font><br>");
			replyMSG.append("Reward Amount:&nbsp;<font color=\"00FF00\">" + e.getInt("rewardAmmount") + "</font><br>");
			replyMSG.append("Min Lvl:&nbsp;<font color=\"00FF00\">" + e.getInt("minLvl") + "</font><br>");
			replyMSG.append("Max Lvl:&nbsp;<font color=\"00FF00\">" + e.getInt("maxLvl") + "</font><br>");
			replyMSG.append("Min Players:&nbsp;<font color=\"00FF00\">" + e.getInt("minPlayers") + "</font><br>");
			if (e instanceof Simon)
				replyMSG.append("Round Time:&nbsp;<font color=\"00FF00\">" + e.getInt("roundTime") + "</font><br>");
			else
				replyMSG.append("Event Time:&nbsp;<font color=\"00FF00\">" + e.getInt("matchTime") + "</font><br>");
			replyMSG.append("Allow Magic:&nbsp;<font color=\"00FF00\">" + e.getString("allowUseMagic") + "</font><br>");
			replyMSG.append("Remove Buffs:&nbsp;<font color=\"00FF00\">" + e.getString("removeBuffs") + "</font><br>");
		}
		
		replyMSG.append("</body></html>");
		adminReply.setHtml(replyMSG.toString());
		activeChar.sendPacket(adminReply);
	}
}