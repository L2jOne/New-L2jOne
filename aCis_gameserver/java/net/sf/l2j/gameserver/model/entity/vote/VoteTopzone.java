package net.sf.l2j.gameserver.model.entity.vote;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * @author Williams
 *
 */
public class VoteTopzone extends VoteBase
{
	@Override
	public void reward(Player player)
	{
		for (IntIntHolder reward : Config.VOTE_REWARDS)
			player.addItem("reward", reward.getId(), reward.getValue(), null, true);
	}
	
	@Override
	public String getApiEndpoint(Player player)
	{
		return String.format("https://l2topzone.com/api.php?API_KEY=%s&SERVER_ID=%d&IP=%s", Config.VOTE_TOPZONE_APIKEY,Config.VOTE_TOPZONE_SERVERID, getPlayerIp(player));
	}
	
	@Override
	public void setVoted(Player player)
	{
		player.setLastTopVote(System.currentTimeMillis());
	}
}