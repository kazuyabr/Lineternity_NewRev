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
package ext.mods.gameserver.handler.bypasshandlers;

import ext.mods.gameserver.handler.IBypassHandler;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.actor.Creature;
import ext.mods.gameserver.StatusPointConfig;
import ext.mods.gameserver.StatusPointOwner;
import ext.mods.gameserver.skills.funcs.FuncStatusPoint;
import ext.mods.gameserver.enums.skills.Stats;
import ext.mods.commons.logging.CLogger;

public class StatusPoint implements IBypassHandler
{
	private static final CLogger LOGGER = new CLogger(StatusPoint.class.getName());
	private static final String[] BYPASS_LIST = { "statuspoint" };
	
	@Override
	public boolean useBypass(String command, Player player, Creature creature)
	{
		if (!StatusPointConfig.STATUS_POINTS_ENABLED)
			return false;
		
		String[] parts = command.split(" ", 3);
		String action = parts.length > 1 ? parts[1] : "show";
		String stat = parts.length > 2 ? parts[2] : "";
		
		switch (action)
		{
			case "add":
				handleAdd(player, stat);
				break;
			case "remove":
				handleRemove(player, stat);
				break;
			case "confirm":
				handleConfirm(player);
				break;
			case "reset":
				handleReset(player);
				break;
		case "show":
		default:
			new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
			break;
		}
		
		return true;
	}
	
	private void handleAdd(Player player, String stat)
	{
		int available = player.getMemos().getInteger("status_points.available", 0);
		if (available <= 0)
			return;
		
		int current = player.getMemos().getInteger("status_points." + stat, 0);
		
		if (isMaxed(stat, current))
			return;
		
		player.getMemos().set("status_points." + stat, current + 1);
		player.getMemos().set("status_points.available", available - 1);
		player.getMemos().set("status_points.preview", true);
		
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void handleRemove(Player player, String stat)
	{
		int current = player.getMemos().getInteger("status_points." + stat, 0);
		if (current <= 0)
			return;
		
		int available = player.getMemos().getInteger("status_points.available", 0);
		
		player.getMemos().set("status_points." + stat, current - 1);
		player.getMemos().set("status_points.available", available + 1);
		
		if (countDistributed(player) == 0)
			player.getMemos().unset("status_points.preview");
		
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void handleConfirm(Player player)
	{
		if (!player.getMemos().getBool("status_points.preview", false))
			return;
		
		player.getMemos().unset("status_points.preview");
		
		player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
		
		boolean isOldChar = player.getMemos().getBool("status_points.isOldChar", false);
		
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
		for (String stat : stats)
		{
			int points = player.getMemos().getInteger("status_points." + stat, 0);
			if (points > 0)
			{
				try
				{
					Stats enumStat = Stats.valueOf("STAT_" + stat);
					player.addStatFunc(new FuncStatusPoint(player, enumStat, points, isOldChar));
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.error("Invalid stat: {}", stat);
				}
			}
		}
		
		player.broadcastUserInfo();
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void handleReset(Player player)
	{
		int totalDistributed = countDistributed(player);
		boolean isOldChar = player.getMemos().getBool("status_points.isOldChar", false);
		
		if (totalDistributed <= 0 && !isOldChar)
		{
			player.sendMessage("You have no distributed status points to reset.");
			return;
		}
		
		if (!StatusPointConfig.PREMIUM_EXEMPT_FROM_RESET_COST || player.getPremiumService() == 0)
		{
			if (player.getInventory().getItemCount(StatusPointConfig.RESET_ITEM_ID) < StatusPointConfig.RESET_ITEM_COUNT)
			{
				player.sendMessage("You need " + StatusPointConfig.RESET_ITEM_COUNT + " item(s) to reset status points.");
				return;
			}
			
			if (!player.reduceAdena(StatusPointConfig.RESET_ADENA, true))
			{
				player.sendMessage("You need " + StatusPointConfig.RESET_ADENA + " adena to reset status points.");
				return;
			}
			
			player.destroyItemByItemId(StatusPointConfig.RESET_ITEM_ID, StatusPointConfig.RESET_ITEM_COUNT, true);
		}
		
		int available = player.getMemos().getInteger("status_points.available", 0);
		player.getMemos().set("status_points.available", available + totalDistributed);
		
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
		for (String stat : stats)
			player.getMemos().unset("status_points." + stat);
		
		player.getMemos().unset("status_points.preview");
		player.getMemos().set("status_points.isOldChar", false);
		
		player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
		player.broadcastUserInfo();
		
		player.sendMessage("Status points reset successfully. " + totalDistributed + " points returned.");
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
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
	
	private int countDistributed(Player player)
	{
		int total = 0;
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
		for (String stat : stats)
			total += player.getMemos().getInteger("status_points." + stat, 0);
		return total;
	}
	
	@Override
	public String[] getBypassList()
	{
		return BYPASS_LIST;
	}
}
