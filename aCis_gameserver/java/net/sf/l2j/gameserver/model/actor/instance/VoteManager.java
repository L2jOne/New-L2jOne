package net.sf.l2j.gameserver.model.actor.instance;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.data.xml.ItemData;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.actor.template.NpcTemplate;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;
import net.sf.l2j.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * @author Williams
 *
 */
public class VoteManager extends Folk
{
    public VoteManager(int objectId, NpcTemplate template)
    {
        super(objectId, template);
    }
   
    @Override
    public void onBypassFeedback(final Player player, String command)
    {
        if (player == null)
            return;
    }
   
    @Override
    public void showChatWindow(Player player)
    {
        NpcHtmlMessage html = new NpcHtmlMessage(0);
        final StringBuilder sb1 = new StringBuilder();
        final StringBuilder sb2 = new StringBuilder();
        final StringBuilder sb3 = new StringBuilder();
       
        if (player.eligibleToVoteHop())
            sb1.append("<a action=\"bypass -h vote hopzone\"> I have voted on hopzone</a><br1>");
        else
            sb1.append(String.format("Vote again in: %s <br1>", player.getVoteCountdownHop()));
       
        if (player.eligibleToVoteTop())
            sb2.append("<a action=\"bypass -h vote topzone\"> I have voted on topzone</a><br1>");
        else
            sb2.append(String.format("Vote again in: %s <br1>", player.getVoteCountdownTop()));
       
        if (player.eligibleToVoteNet())
            sb3.append("<a action=\"bypass -h vote network\"> I have voted on network</a><br1>");
        else
            sb3.append(String.format("Vote again in: %s <br1>", player.getVoteCountdownNet()));
		
		html.setFile(getHtmlPath(getNpcId(), 0));
        html.replace("%VoteTopZone%", sb1.toString());
        html.replace("%VoteHopZone%", sb2.toString());
        html.replace("%VoteNetwork%", sb3.toString());
        html.replace("%char_name%", player.getName());
        html.replace("%website%", Config.SITE_URL);
        for (IntIntHolder reward : Config.VOTE_REWARDS)
        {
          html.replace("%reward%", ItemData.getInstance().getTemplate(reward.getId()).getName());
          html.replace("%value%", reward.getValue());
        } 
    
        player.sendPacket(html);
    }
	
	@Override
	public String getHtmlPath(int npcId, int val)
	{
		String filename = "";
		if (val == 0)
			filename = "" + npcId;
		else
			filename = npcId + "-" + val;
		
		return "data/html/mods/vote/" + filename + ".htm";
	}
}