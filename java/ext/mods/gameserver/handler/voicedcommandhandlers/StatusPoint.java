/*
 * Copyleft © 2024-2026 L2Lineternity
 * * This file is part of L2Lineternity derived from aCis409/RusaCis3.8
 * * L2Lineternity is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * * L2Lineternity is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 * Our main Developers, Dhousefe-L2JBR, Agazes33, Ban-L2jDev, Warman, SrEli.
 * Our special thanks, Nattan Felipe, Diego Fonseca, Junin, ColdPlay, Denky, MecBew, Localhost, MundvayneHELLBOY,
 * SonecaL2, Eduardo.SilvaL2J, biLL, xpower, xTech, kakuzo, Tiagorosendo, Schuster, LucasStark, damedd
 * as a contribution for the forum L2JBrasil.com
 */
package ext.mods.gameserver.handler.voicedcommandhandlers;

import ext.mods.gameserver.handler.IVoicedCommandHandler;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.network.serverpackets.NpcHtmlMessage;
import ext.mods.gameserver.StatusPointConfig;
import ext.mods.gameserver.StatusPointOwner;
import ext.mods.gameserver.skills.funcs.FuncStatusPoint;
import ext.mods.gameserver.enums.skills.Stats;
import ext.mods.gameserver.StatusPointOwner;

public class StatusPoint implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"statuspoint"
	};
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!StatusPointConfig.STATUS_POINTS_ENABLED)
		{
			player.sendMessage("Status Points system is disabled.");
			return false;
		}
		
		if (target != null && !target.isEmpty())
			handleBypass(player, target);
		else
			showHtml(player);
		
		return true;
	}
	
	public void showHtml(Player player)
	{
		player.getMemos().unset("status_points.preview");
		
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/statuspoint/statuspoint.htm");
		
		int available = player.getMemos().getInteger("status_points.available", 0);
		int distributed = countDistributedPoints(player);
		
		htm.replace("%available%", available);
		htm.replace("%distributed%", distributed);
		htm.replace("%pdef_bonus%", player.getMemos().getInteger("status_points.pdef", 0));
		
		htm.replace("%str_base%", player.getTemplate().getBaseSTR());
		htm.replace("%con_base%", player.getTemplate().getBaseCON());
		htm.replace("%dex_base%", player.getTemplate().getBaseDEX());
		htm.replace("%int_base%", player.getTemplate().getBaseINT());
		htm.replace("%wit_base%", player.getTemplate().getBaseWIT());
		htm.replace("%men_base%", player.getTemplate().getBaseMEN());
		
		htm.replace("%str_dist%", player.getMemos().getInteger("status_points.STR", 0));
		htm.replace("%con_dist%", player.getMemos().getInteger("status_points.CON", 0));
		htm.replace("%dex_dist%", player.getMemos().getInteger("status_points.DEX", 0));
		htm.replace("%int_dist%", player.getMemos().getInteger("status_points.INT", 0));
		htm.replace("%wit_dist%", player.getMemos().getInteger("status_points.WIT", 0));
		htm.replace("%men_dist%", player.getMemos().getInteger("status_points.MEN", 0));
		
		htm.replace("%str_total%", player.getTemplate().getBaseSTR() + player.getMemos().getInteger("status_points.STR", 0));
		htm.replace("%con_total%", player.getTemplate().getBaseCON() + player.getMemos().getInteger("status_points.CON", 0));
		htm.replace("%dex_total%", player.getTemplate().getBaseDEX() + player.getMemos().getInteger("status_points.DEX", 0));
		htm.replace("%int_total%", player.getTemplate().getBaseINT() + player.getMemos().getInteger("status_points.INT", 0));
		htm.replace("%wit_total%", player.getTemplate().getBaseWIT() + player.getMemos().getInteger("status_points.WIT", 0));
		htm.replace("%men_total%", player.getTemplate().getBaseMEN() + player.getMemos().getInteger("status_points.MEN", 0));
		
		htm.replace("%patk_spd%", player.getMemos().getInteger("status_points.POWER_ATTACK", 0));
		htm.replace("%matk_spd%", player.getMemos().getInteger("status_points.MAGIC_ATTACK", 0));
		htm.replace("%mspd%", player.getMemos().getInteger("status_points.MOVEMENT_SPEED", 0));
		
		htm.replace("%pvp_kills%", player.getMemos().getInteger("pvp_kills", 0));
		htm.replace("%pvp_milestone%", player.getMemos().getInteger("pvp_milestone", 0));
		htm.replace("%pk_karma_removed%", player.getMemos().getInteger("pk_karma_removed", 0));
		
		boolean hasPreview = player.getMemos().getBool("status_points.preview", false);
		boolean canConfirm = available > 0 && hasPreview;
		
		htm.replace("%confirm_enabled%", canConfirm ? "" : "disabled");
		
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
		for (String stat : stats)
		{
			int dist = player.getMemos().getInteger("status_points." + stat, 0);
			boolean showPlus = (available > 0) && !isMaxed(stat, dist);
			boolean showMinus = (dist > 0) && hasPreview;
			
			String buttons = "";
			if (showPlus)
				buttons += "<button value=\"+\" action=\"bypass -h voiced_statuspoint add " + stat + "\" width=20 height=20 back=L2UI_CH3.calculate2_bs_down fore=L2UI_CH3.calculate2_bs>";
			if (showMinus)
				buttons += "<button value=\"-\" action=\"bypass -h voiced_statuspoint remove " + stat + "\" width=20 height=20 back=L2UI_CH3.calculate2_bs_down fore=L2UI_CH3.calculate2_bs>";
			
			htm.replace("%" + stat.toLowerCase() + "_buttons%", buttons);
		}
		
		player.sendPacket(htm);
	}
	
	private void handleBypass(Player player, String target)
	{
		String[] parts = target.split(" ", 3);
		String action = parts.length > 1 ? parts[1] : "show";
		String stat = parts.length > 2 ? parts[2] : "";
		
		switch (action)
		{
			case "add":
			{
				int available = player.getMemos().getInteger("status_points.available", 0);
				if (available <= 0)
					break;
				
				int current = player.getMemos().getInteger("status_points." + stat, 0);
				if (isMaxed(stat, current))
					break;
				
				player.getMemos().set("status_points." + stat, current + 1);
				player.getMemos().set("status_points.available", available - 1);
				player.getMemos().set("status_points.preview", true);
				break;
			}
			case "remove":
			{
				int current = player.getMemos().getInteger("status_points." + stat, 0);
				if (current <= 0)
					break;
				
				int available = player.getMemos().getInteger("status_points.available", 0);
				player.getMemos().set("status_points." + stat, current - 1);
				player.getMemos().set("status_points.available", available + 1);
				
				if (countDistributedPoints(player) == 0)
					player.getMemos().unset("status_points.preview");
				break;
			}
			case "confirm":
			{
				if (!player.getMemos().getBool("status_points.preview", false))
					break;
				
				player.getMemos().unset("status_points.preview");
				player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
				
				String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
				for (String stat : stats)
				{
					int points = player.getMemos().getInteger("status_points." + stat, 0);
					if (points > 0)
					{
						try
						{
							Stats enumStat = Stats.valueOf("STAT_" + stat);
							player.addStatFunc(new FuncStatusPoint(player, enumStat, points, StatusPointOwner.DISTRIBUTED));
						}
						catch (IllegalArgumentException e)
						{
						}
					}
				}
				
				player.broadcastUserInfo();
				break;
			}
			case "reset":
			{
				int totalDistributed = countDistributedPoints(player);
				if (totalDistributed <= 0)
				{
					player.sendMessage("You have no distributed status points to reset.");
					break;
				}
				
				if (!StatusPointConfig.PREMIUM_EXEMPT_FROM_RESET_COST || player.getPremiumService() == 0)
				{
					if (player.getInventory().getItemCount(StatusPointConfig.RESET_ITEM_ID) < StatusPointConfig.RESET_ITEM_COUNT)
					{
						player.sendMessage("You need " + StatusPointConfig.RESET_ITEM_COUNT + " item(s) to reset status points.");
						break;
					}
					
					if (!player.reduceAdena(StatusPointConfig.RESET_ADENA, true))
					{
						player.sendMessage("You need " + StatusPointConfig.RESET_ADENA + " adena to reset status points.");
						break;
					}
					
					player.destroyItemByItemId(StatusPointConfig.RESET_ITEM_ID, StatusPointConfig.RESET_ITEM_COUNT, true);
				}
				
				int available = player.getMemos().getInteger("status_points.available", 0);
				player.getMemos().set("status_points.available", available + totalDistributed);
				
				String[] resetStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
				for (String stat : resetStats)
					player.getMemos().unset("status_points." + stat);
				
				player.getMemos().unset("status_points.preview");
				player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
				player.broadcastUserInfo();
				
				player.sendMessage("Status points reset successfully. " + totalDistributed + " points returned.");
				break;
			}
		}
		
		showHtml(player);
	}
	
	private int countDistributedPoints(Player player)
	{
		int total = 0;
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
		for (String stat : stats)
			total += player.getMemos().getInteger("status_points." + stat, 0);
		return total;
	}
	
	private boolean isMaxed(String stat, int current)
	{
		switch (stat)
		{
			case "DEX":
				return current >= StatusPointConfig.MAX_DEX_POINTS;
			case "WIT":
				return current >= StatusPointConfig.MAX_WIT_POINTS;
			case "POWER_ATTACK":
				return current >= StatusPointConfig.MAX_ATTACK_SPEED_POINTS;
			case "MAGIC_ATTACK":
				return current >= StatusPointConfig.MAX_MAGIC_ATTACK_SPEED_POINTS;
			case "MOVEMENT_SPEED":
				return current >= StatusPointConfig.MAX_MOVEMENT_SPEED_POINTS;
			default:
				return false;
		}
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
