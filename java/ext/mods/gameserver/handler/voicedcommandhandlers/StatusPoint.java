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
 * SonecaL2, Eduardo.Silva, biLL, xpower, xTech, kakuzo, Tiagorosendo, Schuster, LucasStark, damedd
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
		{
			player.getMemos().unset("status_points.preview");
			showHtml(player);
		}
		
		return true;
	}
	
	public void showHtml(Player player)
	{
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/statuspoint/statuspoint.htm");
		
		int available = player.getMemos().getInteger("status_points.available", 0);
		int distributed = countDistributedPoints(player);
		boolean hasPreview = player.getMemos().getBool("status_points.preview", false);
		
		htm.replace("%available%", available);
		htm.replace("%distributed%", distributed);
		htm.replace("%pdef_bonus%", player.getMemos().getInteger("status_points.pdef", 0));
		
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN"};
		for (String stat : stats)
		{
			int templateBase = getBaseForStat(player, stat);
			int dist = player.getMemos().getInteger("status_points." + stat, 0);
			int total = templateBase + dist;
			
			String display;
			if (hasPreview && dist > 0)
				display = templateBase + " + " + dist + " = " + total;
			else
				display = String.valueOf(total);
			
			htm.replace("%" + stat.toLowerCase() + "_display%", display);
		}
		
		htm.replace("%pvp_kills%", player.getMemos().getInteger("pvp_kills", 0));
		htm.replace("%pvp_milestone%", player.getMemos().getInteger("pvp_milestone", 0));
		htm.replace("%pk_karma_removed%", player.getMemos().getInteger("pk_karma_removed", 0));
		
		boolean canConfirm = hasPreview && distributed > 0;
		
		htm.replace("%confirm_enabled%", canConfirm ? "" : "disabled");
		
		boolean showReset = distributed > 0;
		htm.replace("%reset_enabled%", showReset ? "" : "disabled");
		
		String resetCostText = StatusPointConfig.RESET_ADENA + "x Adena";
		htm.replace("%reset_cost%", resetCostText);
		
		for (String stat : stats)
		{
			int dist = player.getMemos().getInteger("status_points." + stat, 0);
			boolean showPlus = (available > 0) && !isMaxed(stat, dist);
			boolean showMinus = (dist > 0) && hasPreview;
			
			String buttons = "";
			if (showPlus)
				buttons += "<button value=\"+\" action=\"bypass -h voiced_statuspoint add " + stat + "\" width=18 height=18 back=L2UI_CH3.calculate2_bs_down fore=L2UI_CH3.calculate2_bs>&nbsp;";
			if (showMinus)
				buttons += "<button value=\"-\" action=\"bypass -h voiced_statuspoint remove " + stat + "\" width=18 height=18 back=L2UI_CH3.calculate2_bs_down fore=L2UI_CH3.calculate2_bs>";
			
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
			
			String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN"};
			for (String s : stats)
			{
				int points = player.getMemos().getInteger("status_points." + s, 0);
				if (points > 0)
				{
					try
					{
						Stats enumStat = Stats.valueOf("STAT_" + s);
						player.addStatFunc(new FuncStatusPoint(player, enumStat, points, false));
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
			
			int baseSum = getBaseForStat(player, "STR") + getBaseForStat(player, "CON") +
					getBaseForStat(player, "DEX") + getBaseForStat(player, "INT") +
					getBaseForStat(player, "WIT") + getBaseForStat(player, "MEN");
			
			player.getMemos().set("status_points.available", baseSum);
			
			String[] resetStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN"};
			for (String s : resetStats)
				player.getMemos().unset("status_points." + s);
			
			player.getMemos().unset("status_points.preview");
			player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
			player.broadcastUserInfo();
			
			player.sendMessage("Status points reset successfully.");
			break;
		}
		}
		
		showHtml(player);
	}
	
	private int getBaseForStat(Player player, String stat)
	{
		switch (stat)
		{
			case "STR":
				return player.getTemplate().getBaseSTR();
			case "CON":
				return player.getTemplate().getBaseCON();
			case "DEX":
				return player.getTemplate().getBaseDEX();
			case "INT":
				return player.getTemplate().getBaseINT();
			case "WIT":
				return player.getTemplate().getBaseWIT();
			case "MEN":
				return player.getTemplate().getBaseMEN();
			default:
				return 0;
		}
	}
	
	private int countDistributedPoints(Player player)
	{
		int total = 0;
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN"};
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
