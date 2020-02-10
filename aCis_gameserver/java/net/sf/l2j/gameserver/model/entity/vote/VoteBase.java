package net.sf.l2j.gameserver.model.entity.vote;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;

import net.sf.l2j.commons.lang.StringUtil;

import net.sf.l2j.DatabaseFactory;
import net.sf.l2j.gameserver.model.actor.Player;

/**
 * @author Elfocrash
 *
 */
public abstract class VoteBase
{
	public String getPlayerIp(Player player)
	{
		String IP = player.getClient().getConnection().getInetAddress().getHostAddress();
		if (IP.equals("127.0.0.1"))
			return "78.63.104.146";

		return player.getClient().getConnection().getInetAddress().getHostAddress();
	}
	
	public static String getPlayerStaticIp(Player player)
	{
		String IP = player.getClient().getConnection().getInetAddress().getHostAddress();
		if (IP.equals("127.0.0.1"))
			return "78.63.104.146";

		return player.getClient().getConnection().getInetAddress().getHostAddress();
	}
	
	public abstract void reward(Player player);
	
	public abstract void setVoted(Player player);
	
	public void updateDB(Player player, String columnName)
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement(String.format("UPDATE VoteSystem SET %s=?, Account=?, Char_name=? WHERE IP_Address=?",columnName)))
		{
			statement.setLong(1, System.currentTimeMillis());
			statement.setString(2, player.getAccountName());
			statement.setString(3, player.getName());
			statement.setString(4, getPlayerIp(player));
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.out.println("Error in VoteBase::updateDB");
	}
	}
	
	public boolean hasVoted(Player player)
	{
		try
		{
			String endpoint = getApiEndpoint(player);
			if (endpoint.startsWith("err"))
				return false;
			
			String voted = endpoint.startsWith("https://api.hopzone.net") ? StringUtil.substringBetween(getApiResponse(endpoint),"\"voted\":",",\"hopzoneServerTime\"") : getApiResponse(endpoint);
			return tryParseBool(voted);
		}
		catch (Exception e)
		{
			player.sendMessage("Something went wrong. Please try again later.");
			e.printStackTrace();
		}
		return false;
	}
	
	public boolean tryParseBool(String bool)
	{
		if (bool.startsWith("1") || bool.startsWith("true"))
			return true;
		
		return Boolean.parseBoolean(bool.trim());
	}
	
	public abstract String getApiEndpoint(Player player);
	
	public String getApiResponse(String endpoint)
	{
	    StringBuilder stringBuilder = new StringBuilder();
	    try
	    {
	      URL url = new URL(endpoint);
	      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	      connection.addRequestProperty("User-Agent", "Mozilla/4.76"); 
	      connection.setRequestMethod("GET");
	      connection.setReadTimeout(5*1000);
	      connection.connect();

	      try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream())))
	      {
		      String line = null;
		      while ((line = reader.readLine()) != null)
		      {
		        stringBuilder.append(line + "\n");
		      }
	      }
	      connection.disconnect();
	      return stringBuilder.toString();
	    }
	    catch (Exception e)
	    {
		    System.out.println("Something went wrong in VoteBase::getApiResponse");
		    e.printStackTrace();
		    return "err";
	    }
	}
}