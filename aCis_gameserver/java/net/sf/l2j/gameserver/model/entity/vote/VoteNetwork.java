package net.sf.l2j.gameserver.model.entity.vote;

import net.sf.l2j.Config;
import net.sf.l2j.gameserver.model.actor.Player;
import net.sf.l2j.gameserver.model.holder.IntIntHolder;

/**
 * @author Williams
 *
 */
public class VoteNetwork extends VoteBase
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
		return String.format("https://l2network.eu/index.php?a=in&u=%s&ipc=%s", Config.VOTE_NETWORK_NAME, getPlayerIp(player));
	}

	@Override
	public void setVoted(Player player)
	{
		player.setLastNetVote(System.currentTimeMillis());
	}
}