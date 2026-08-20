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
		
		if (isDirectStat(stat))
		{
			if (data.statusAvailable <= 0)
				return;
			if (isMaxedDirect(stat, data))
				return;
			int current = getDirectStatValue(data, stat);
			setDirectStatValue(data, stat, current + 1);
			data.statusAvailable--;
			data.statusDistributed++;
		}
		else
		{
			if (data.attrAvailable <= 0)
				return;
			if (isMaxedAttr(player, stat, data))
				return;
			int current = getAttrStatValue(data, stat);
			setAttrStatValue(data, stat, current + 1);
			data.attrAvailable--;
			data.attrDistributed++;
		}
		
		data.dirty = true;
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private void handleRemove(Player player, String stat)
	{
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		if (isDirectStat(stat))
		{
			int current = getDirectStatValue(data, stat);
			int confirmed = getConfirmedDirectValue(data, stat);
			if (current <= confirmed)
				return;
			setDirectStatValue(data, stat, current - 1);
			data.statusAvailable++;
			data.statusDistributed--;
		}
		else
		{
			int current = getAttrStatValue(data, stat);
			int confirmed = getConfirmedAttrValue(data, stat);
			if (current <= confirmed)
				return;
			setAttrStatValue(data, stat, current - 1);
			data.attrAvailable++;
			data.attrDistributed--;
		}
		
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
		
		data.confirmedAttrStr = data.attrStr;
		data.confirmedAttrCon = data.attrCon;
		data.confirmedAttrDex = data.attrDex;
		data.confirmedAttrInt = data.attrInt;
		data.confirmedAttrWit = data.attrWit;
		data.confirmedAttrMen = data.attrMen;
		data.confirmedStatusPdef = data.statusPdef;
		data.confirmedStatusMdef = data.statusMdef;
		data.confirmedStatusHp = data.statusHp;
		data.confirmedStatusMp = data.statusMp;
		data.confirmedStatusCp = data.statusCp;
		data.confirmedStatusPatk = data.statusPatk;
		data.confirmedStatusMatk = data.statusMatk;
		data.confirmedStatusAccuracy = data.statusAccuracy;
		data.confirmedStatusEvasion = data.statusEvasion;
		data.confirmedStatusCrit = data.statusCrit;
		
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
		
		int totalAttrDistributed = data.getAttrDistributed();
		int totalStatusDistributed = data.getStatusDistributed();
		if (totalAttrDistributed <= 0 && totalStatusDistributed <= 0)
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
		
		if (totalAttrDistributed > 0)
		{
			data.attrAvailable += totalAttrDistributed;
			data.attrStr = 0;
			data.attrCon = 0;
			data.attrDex = 0;
			data.attrInt = 0;
			data.attrWit = 0;
			data.attrMen = 0;
			data.attrDistributed = 0;
		}
		
		if (totalStatusDistributed > 0)
		{
			data.statusAvailable += totalStatusDistributed;
			data.statusPdef = 0;
			data.statusMdef = 0;
			data.statusHp = 0;
			data.statusMp = 0;
			data.statusCp = 0;
			data.statusPatk = 0;
			data.statusMatk = 0;
			data.statusAccuracy = 0;
			data.statusEvasion = 0;
			data.statusCrit = 0;
			data.statusDistributed = 0;
		}
		
		data.confirmedAttrStr = 0;
		data.confirmedAttrCon = 0;
		data.confirmedAttrDex = 0;
		data.confirmedAttrInt = 0;
		data.confirmedAttrWit = 0;
		data.confirmedAttrMen = 0;
		data.confirmedStatusPdef = 0;
		data.confirmedStatusMdef = 0;
		data.confirmedStatusHp = 0;
		data.confirmedStatusMp = 0;
		data.confirmedStatusCp = 0;
		data.confirmedStatusPatk = 0;
		data.confirmedStatusMatk = 0;
		data.confirmedStatusAccuracy = 0;
		data.confirmedStatusEvasion = 0;
		data.confirmedStatusCrit = 0;
		
		Player.applyStatusPointFuncs(player, data);
		Player.applyDirectStatusFuncs(player, data);
		data.computeEffectiveBases(player);
		data.dirty = false;
		data.store(player);
		player.broadcastUserInfo();
		
		player.sendMessage("Status points reset successfully.");
		new ext.mods.gameserver.handler.voicedcommandhandlers.StatusPoint().showHtml(player);
	}
	
	private boolean isDirectStat(String stat)
	{
		return switch (stat)
		{
			case "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT" -> true;
			default -> false;
		};
	}
	
	private int getAttrStatValue(CharacterStatusPoints data, String stat)
	{
		return switch (stat)
		{
			case "STR" -> data.attrStr;
			case "CON" -> data.attrCon;
			case "DEX" -> data.attrDex;
			case "INT" -> data.attrInt;
			case "WIT" -> data.attrWit;
			case "MEN" -> data.attrMen;
			default -> 0;
		};
	}
	
	private void setAttrStatValue(CharacterStatusPoints data, String stat, int value)
	{
		switch (stat)
		{
			case "STR" -> data.attrStr = value;
			case "CON" -> data.attrCon = value;
			case "DEX" -> data.attrDex = value;
			case "INT" -> data.attrInt = value;
			case "WIT" -> data.attrWit = value;
			case "MEN" -> data.attrMen = value;
		}
	}
	
	private int getDirectStatValue(CharacterStatusPoints data, String stat)
	{
		return switch (stat)
		{
			case "PDEF" -> data.statusPdef;
			case "MDEF" -> data.statusMdef;
			case "HP" -> data.statusHp;
			case "MP" -> data.statusMp;
			case "CP" -> data.statusCp;
			case "PATK" -> data.statusPatk;
			case "MATK" -> data.statusMatk;
			case "ACC" -> data.statusAccuracy;
			case "EVA" -> data.statusEvasion;
			case "CRIT" -> data.statusCrit;
			default -> 0;
		};
	}
	
	private void setDirectStatValue(CharacterStatusPoints data, String stat, int value)
	{
		switch (stat)
		{
			case "PDEF" -> data.statusPdef = value;
			case "MDEF" -> data.statusMdef = value;
			case "HP" -> data.statusHp = value;
			case "MP" -> data.statusMp = value;
			case "CP" -> data.statusCp = value;
			case "PATK" -> data.statusPatk = value;
			case "MATK" -> data.statusMatk = value;
			case "ACC" -> data.statusAccuracy = value;
			case "EVA" -> data.statusEvasion = value;
			case "CRIT" -> data.statusCrit = value;
		}
	}
	
	private int getConfirmedAttrValue(CharacterStatusPoints data, String stat)
	{
		return switch (stat)
		{
			case "STR" -> data.confirmedAttrStr;
			case "CON" -> data.confirmedAttrCon;
			case "DEX" -> data.confirmedAttrDex;
			case "INT" -> data.confirmedAttrInt;
			case "WIT" -> data.confirmedAttrWit;
			case "MEN" -> data.confirmedAttrMen;
			default -> 0;
		};
	}
	
	private int getConfirmedDirectValue(CharacterStatusPoints data, String stat)
	{
		return switch (stat)
		{
			case "PDEF" -> data.confirmedStatusPdef;
			case "MDEF" -> data.confirmedStatusMdef;
			case "HP" -> data.confirmedStatusHp;
			case "MP" -> data.confirmedStatusMp;
			case "CP" -> data.confirmedStatusCp;
			case "PATK" -> data.confirmedStatusPatk;
			case "MATK" -> data.confirmedStatusMatk;
			case "ACC" -> data.confirmedStatusAccuracy;
			case "EVA" -> data.confirmedStatusEvasion;
			case "CRIT" -> data.confirmedStatusCrit;
			default -> 0;
		};
	}
	
	private boolean isMaxedAttr(Player player, String stat, CharacterStatusPoints data)
	{
		int distributed = getAttrStatValue(data, stat);
		int total;
		int max;
		
		switch (stat)
		{
			case "DEX":
				total = data.previewBaseDex + distributed;
				max = StatusPointConfig.MAX_TOTAL_DEX;
				return total >= max;
			case "WIT":
				total = data.previewBaseWit + distributed;
				max = StatusPointConfig.MAX_TOTAL_WIT;
				return total >= max;
			default:
				return false;
		}
	}
	
	private boolean isMaxedDirect(String stat, CharacterStatusPoints data)
	{
		int distributed = getDirectStatValue(data, stat);
		int max = getMaxDirectStatValue(stat);
		return distributed >= max;
	}
	
	private int getMaxDirectStatValue(String stat)
	{
		return switch (stat)
		{
			case "PDEF" -> StatusPointConfig.MAX_DIRECT_PDEF;
			case "MDEF" -> StatusPointConfig.MAX_DIRECT_MDEF;
			case "HP" -> StatusPointConfig.MAX_DIRECT_HP;
			case "MP" -> StatusPointConfig.MAX_DIRECT_MP;
			case "CP" -> StatusPointConfig.MAX_DIRECT_CP;
			case "PATK" -> StatusPointConfig.MAX_DIRECT_PATK;
			case "MATK" -> StatusPointConfig.MAX_DIRECT_MATK;
			case "ACC" -> StatusPointConfig.MAX_DIRECT_ACCURACY;
			case "EVA" -> StatusPointConfig.MAX_DIRECT_EVASION;
			case "CRIT" -> StatusPointConfig.MAX_DIRECT_CRIT;
			default -> Integer.MAX_VALUE;
		};
	}
	
	@Override
	public String[] getBypassList()
	{
		return BYPASS_LIST;
	}
}
