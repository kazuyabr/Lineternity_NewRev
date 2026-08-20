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
import ext.mods.gameserver.CharacterStatusPoints;
import ext.mods.gameserver.StatusPointConfig;
import ext.mods.gameserver.StatusPointOwner;
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
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		int cost = data.getCostToNext(stat);
		if (data.available < cost)
			return;
		if (isMaxed(player, stat, data))
			return;
		if (data.isAtCap(stat) && !StatusPointConfig.hasCapItems(player))
			return;
		
		if (data.isAtCap(stat))
			StatusPointConfig.chargeCapItems(player);
		
		int current = data.getDistributedValue(stat);
		data.setDistributedValue(stat, current + 1);
		data.available -= cost;
		updateDistributedCount(data, stat);
		data.dirty = true;
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void handleRemove(Player player, String stat)
	{
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		int current = data.getDistributedValue(stat);
		int confirmed = data.getConfirmedValue(stat);
		if (current <= confirmed)
			return;
		
		int refund = StatusPointConfig.getCostForStat(current - 1);
		data.setDistributedValue(stat, current - 1);
		data.available += refund;
		updateDistributedCount(data, stat);
		data.dirty = true;
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void handleConfirm(Player player)
	{
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
		player.removeStatsByOwner(StatusPointOwner.DIRECT);
		Player.applyStatusPointFuncs(player, data);
		Player.applyDirectStatusFuncs(player, data);
		
		String[] allStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT"};
		for (String statName : allStats)
			data.setConfirmedValue(statName, data.getDistributedValue(statName));
		
		data.computeEffectiveBases(player);
		data.dirty = false;
		data.store(player);
		player.broadcastUserInfo();
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void handleReset(Player player)
	{
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null)
			return;
		
		if (data.isOldChar)
		{
			data.migrateOldChar(player);
			data.store(player);
			player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
			player.removeStatsByOwner(StatusPointOwner.DIRECT);
			Player.applyStatusPointFuncs(player, data);
			Player.applyDirectStatusFuncs(player, data);
			data.computeEffectiveBases(player);
			data.dirty = false;
			data.store(player);
			player.broadcastUserInfo();
			player.sendMessage("Migration complete. Points redistributed based on level.");
			new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
			return;
		}
		
		if (data.getTotalDistributed() <= 0)
		{
			player.sendMessage("You have no distributed points to reset.");
			return;
		}
		
		if (!StatusPointConfig.PREMIUM_EXEMPT_FROM_RESET_COST || player.getPremiumService() == 0)
		{
			if (!StatusPointConfig.hasResetCostItems(player))
			{
				player.sendMessage("You need " + StatusPointConfig.getResetCostDisplay() + " to reset.");
				return;
			}
			StatusPointConfig.chargeResetCost(player);
		}
		
		player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
		player.removeStatsByOwner(StatusPointOwner.DIRECT);
		
		int totalSpent = data.getTotalSpent();
		data.available += totalSpent;
		
		String[] allStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT"};
		for (String statName : allStats)
		{
			data.setDistributedValue(statName, 0);
			data.setConfirmedValue(statName, 0);
		}
		data.attrDistributed = 0;
		data.statusDistributed = 0;
		
		Player.applyStatusPointFuncs(player, data);
		Player.applyDirectStatusFuncs(player, data);
		data.computeEffectiveBases(player);
		data.dirty = false;
		data.store(player);
		player.broadcastUserInfo();
		
		player.sendMessage("Status points reset successfully. " + totalSpent + " points returned.");
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void updateDistributedCount(CharacterStatusPoints data, String stat)
	{
		data.attrDistributed = data.attrStr + data.attrCon + data.attrDex + data.attrInt + data.attrWit + data.attrMen;
		data.statusDistributed = data.statusPdef + data.statusMdef + data.statusHp + data.statusMp + data.statusCp
			+ data.statusPatk + data.statusMatk + data.statusAccuracy + data.statusEvasion + data.statusCrit;
	}
	
	private boolean isMaxed(Player player, String stat, CharacterStatusPoints data)
	{
		int distributed = data.getDistributedValue(stat);
		
		return switch (stat)
		{
			case "DEX" -> (data.previewBaseDex + distributed) >= StatusPointConfig.MAX_TOTAL_DEX;
			case "WIT" -> (data.previewBaseWit + distributed) >= StatusPointConfig.MAX_TOTAL_WIT;
			case "PDEF" -> distributed >= StatusPointConfig.MAX_DIRECT_PDEF;
			case "MDEF" -> distributed >= StatusPointConfig.MAX_DIRECT_MDEF;
			case "HP" -> distributed >= StatusPointConfig.MAX_DIRECT_HP;
			case "MP" -> distributed >= StatusPointConfig.MAX_DIRECT_MP;
			case "CP" -> distributed >= StatusPointConfig.MAX_DIRECT_CP;
			case "PATK" -> distributed >= StatusPointConfig.MAX_DIRECT_PATK;
			case "MATK" -> distributed >= StatusPointConfig.MAX_DIRECT_MATK;
			case "ACC" -> distributed >= StatusPointConfig.MAX_DIRECT_ACCURACY;
			case "EVA" -> distributed >= StatusPointConfig.MAX_DIRECT_EVASION;
			case "CRIT" -> distributed >= StatusPointConfig.MAX_DIRECT_CRIT;
			default -> false;
		};
	}
	
	@Override
	public String[] getBypassList()
	{
		return BYPASS_LIST;
	}
}
