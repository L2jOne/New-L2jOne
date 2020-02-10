package net.sf.l2j.gameserver.model.entity.vote;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * @author Elfocrash
 *
 */
public class VoteHopzone extends VoteBase
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
		return String.format("https://api.hopzone.net/lineage2/vote?token=%s&ip_address=%s", Config.VOTE_HOPZONE_APIKEY, getPlayerIp(player));
	}

	@Override
	public void setVoted(Player player)
	{
		player.setLastHopVote(System.currentTimeMillis());
	}
}