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
import ext.mods.gameserver.CharacterStatusPoints;
import ext.mods.gameserver.StatusPointConfig;
import ext.mods.gameserver.StatusPointOwner;

public class StatusPoint implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS = { "statuspoint" };
	
	@Override
	public boolean useVoicedCommand(String command, Player player, String target)
	{
		if (!StatusPointConfig.STATUS_POINTS_ENABLED)
		{
			player.sendMessage("Status Points system is disabled.");
			return false;
		}
		
		// When called from chat: command="statuspoint", target="add STR"
		// When called from HTML bypass: command="statuspoint add STR", target=null
		if ((target == null || target.isEmpty()) && command.contains(" "))
			target = command.substring(command.indexOf(' ') + 1);
		
		if (target != null && !target.isEmpty())
			handleBypass(player, target);
		else
			showHtml(player);
		
		return true;
	}
	
	public void showHtml(Player player)
	{
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null)
			return;
		
		NpcHtmlMessage htm = new NpcHtmlMessage(0);
		htm.setFile(player.getLocale(), "html/mods/statuspoint/statuspoint.htm");
		
		htm.replace("%attr_available%", data.attrAvailable);
		htm.replace("%status_available%", data.statusAvailable);
		
		boolean isOldChar = data.isOldChar;
		
		String[] attrStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN"};
		for (String stat : attrStats)
		{
			int preview = getPreviewStatValue(data, stat);
			htm.replace("%" + stat.toLowerCase() + "_display%", String.valueOf(preview));
		}
		
		String[] directStats = {"PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT"};
		String[] directKeys = {"pdef", "mdef", "hp", "mp", "cp", "patk", "matk", "acc", "eva", "crit"};
		for (int i = 0; i < directStats.length; i++)
		{
			int preview = getPreviewDirectValue(data, directStats[i]);
			htm.replace("%" + directKeys[i] + "_display%", String.valueOf(preview));
		}
		
		boolean canConfirm = !isOldChar && data.dirty;
		boolean showReset = isOldChar || hasAnyDistributed(data);
		
		htm.replace("%confirm_button%", canConfirm ? makeButton("Confirm", "confirm") : "");
		htm.replace("%reset_button%", showReset ? makeButton("Reset", "reset") : "");
		
		String resetCostText = StatusPointConfig.getResetCostDisplay();
		htm.replace("%reset_cost%", resetCostText);
		
		if (data.karmaPenaltyAttr > 0)
			htm.replace("%karma_display%", "<font color=LEVEL>Karma Penalty: <font color=FF0000>" + data.karmaPenaltyAttr + "</font></font>");
		else
			htm.replace("%karma_display%", "");
		
		for (String stat : attrStats)
		{
			int dist = getAttrStatValue(data, stat);
			int confirmed = getConfirmedAttrValue(data, stat);
			boolean showPlus = (data.attrAvailable > 0) && !isMaxedAttr(player, stat, data) && !isOldChar;
			boolean showMinus = data.dirty && (dist > confirmed) && !isOldChar;
			htm.replace("%" + stat.toLowerCase() + "_buttons%", makePlusMinus(stat, showPlus, showMinus));
		}
		
		for (int i = 0; i < directStats.length; i++)
		{
			int dist = getDirectStatValue(data, directStats[i]);
			int confirmed = getConfirmedDirectValue(data, directStats[i]);
			boolean showPlus = (data.statusAvailable > 0) && !isOldChar && !isMaxedDirect(directStats[i], data);
			boolean showMinus = data.dirty && (dist > confirmed) && !isOldChar;
			htm.replace("%" + directKeys[i] + "_buttons%", makePlusMinus(directStats[i], showPlus, showMinus));
		}
		
		player.sendPacket(htm);
	}
	
	private String makePlusMinus(String stat, boolean showPlus, boolean showMinus)
	{
		if (!showPlus && !showMinus)
			return "";
		
		String plus = showPlus
			? "<button value=\"+\" action=\"bypass -h voiced_statuspoint add " + stat + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>"
			: "";
		String minus = showMinus
			? "<button value=\"-\" action=\"bypass -h voiced_statuspoint remove " + stat + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>"
			: "";
		
		return "<table cellpadding=0 cellspacing=0><tr><td>" + plus + "</td><td>" + minus + "</td></tr></table>";
	}
	
	private String makeButton(String value, String action)
	{
		return "<button value=\"" + value + "\" action=\"bypass -h voiced_statuspoint " + action + "\" width=74 height=21 back=L2UI_ch3.Btn1_normalOn fore=L2UI_ch3.Btn1_normal>";
	}
	
	private void handleBypass(Player player, String target)
	{
		String[] parts = target.split(" ", 3);
		String action = parts[0];
		String stat = parts.length > 1 ? parts[1] : "";
		
		switch (action)
		{
		case "add":
		{
			CharacterStatusPoints data = player.getStatusPointsData();
			if (data == null || data.isOldChar)
				break;
			
			if (isDirectStat(stat))
			{
				if (data.statusAvailable <= 0)
					break;
				if (isMaxedDirect(stat, data))
					break;
				int current = getDirectStatValue(data, stat);
				setDirectStatValue(data, stat, current + 1);
				data.statusAvailable--;
				data.statusDistributed++;
			}
			else
			{
				if (data.attrAvailable <= 0)
					break;
				if (isMaxedAttr(player, stat, data))
					break;
				int current = getAttrStatValue(data, stat);
				setAttrStatValue(data, stat, current + 1);
				data.attrAvailable--;
				data.attrDistributed++;
			}
			
			data.dirty = true;
			break;
		}
			case "remove":
			{
				CharacterStatusPoints data = player.getStatusPointsData();
				if (data == null || data.isOldChar)
					break;
				
				if (isDirectStat(stat))
				{
					int current = getDirectStatValue(data, stat);
					int confirmed = getConfirmedDirectValue(data, stat);
					if (current <= confirmed)
						break;
					setDirectStatValue(data, stat, current - 1);
					data.statusAvailable++;
					data.statusDistributed--;
				}
				else
				{
					int current = getAttrStatValue(data, stat);
					int confirmed = getConfirmedAttrValue(data, stat);
					if (current <= confirmed)
						break;
					setAttrStatValue(data, stat, current - 1);
					data.attrAvailable++;
					data.attrDistributed--;
				}
				
				data.dirty = true;
				break;
			}
			case "confirm":
			{
				CharacterStatusPoints data = player.getStatusPointsData();
				if (data == null || data.isOldChar)
					break;
				
				player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
				player.removeStatsByOwner(StatusPointOwner.DIRECT);
				ext.mods.gameserver.model.actor.Player.applyStatusPointFuncs(player, data);
				ext.mods.gameserver.model.actor.Player.applyDirectStatusFuncs(player, data);
				
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
				break;
			}
			case "reset":
			{
				CharacterStatusPoints data = player.getStatusPointsData();
				if (data == null)
					break;
				
				if (data.isOldChar)
				{
					data.migrateOldChar(player);
					player.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
					player.removeStatsByOwner(StatusPointOwner.DIRECT);
					ext.mods.gameserver.model.actor.Player.applyStatusPointFuncs(player, data);
					ext.mods.gameserver.model.actor.Player.applyDirectStatusFuncs(player, data);
					data.computeEffectiveBases(player);
					data.dirty = false;
					data.store(player);
					player.broadcastUserInfo();
					player.sendMessage("Migration complete. Points redistributed based on level.");
					break;
				}
				
				int totalAttrDistributed = data.getAttrDistributed();
				int totalStatusDistributed = data.getStatusDistributed();
				if (totalAttrDistributed <= 0 && totalStatusDistributed <= 0)
				{
					player.sendMessage("You have no distributed points to reset.");
					break;
				}
				
				if (!StatusPointConfig.PREMIUM_EXEMPT_FROM_RESET_COST || player.getPremiumService() == 0)
				{
					if (!StatusPointConfig.hasResetCostItems(player))
					{
						player.sendMessage("You need " + StatusPointConfig.getResetCostDisplay() + " to reset.");
						break;
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
				
				ext.mods.gameserver.model.actor.Player.applyStatusPointFuncs(player, data);
				ext.mods.gameserver.model.actor.Player.applyDirectStatusFuncs(player, data);
				data.computeEffectiveBases(player);
				data.dirty = false;
				data.store(player);
				player.broadcastUserInfo();
				player.sendMessage("Status points reset successfully.");
				break;
			}
		}
		
		showHtml(player);
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
	
	private int getPreviewStatValue(CharacterStatusPoints data, String stat)
	{
		int base = switch (stat)
		{
			case "STR" -> data.previewBaseStr;
			case "CON" -> data.previewBaseCon;
			case "DEX" -> data.previewBaseDex;
			case "INT" -> data.previewBaseInt;
			case "WIT" -> data.previewBaseWit;
			case "MEN" -> data.previewBaseMen;
			default -> 0;
		};
		return base + getAttrStatValue(data, stat);
	}
	
	private int getPreviewDirectValue(CharacterStatusPoints data, String stat)
	{
		int dist = getDirectStatValue(data, stat);
		return switch (stat)
		{
			case "PDEF" -> data.previewBasePdef + dist * StatusPointConfig.PDEF_PER_POINT;
			case "MDEF" -> data.previewBaseMdef + dist;
			case "HP" -> data.previewBaseHp + dist;
			case "MP" -> data.previewBaseMp + dist;
			case "CP" -> data.previewBaseCp + dist;
			case "PATK" -> data.previewBasePatk + dist;
			case "MATK" -> data.previewBaseMatk + dist;
			case "ACC" -> data.previewBaseAccuracy + dist;
			case "EVA" -> data.previewBaseEvasion + dist;
			case "CRIT" -> data.previewBaseCrit + dist;
			default -> 0;
		};
	}
	
	private boolean hasAnyDistributed(CharacterStatusPoints data)
	{
		return data.getAttrDistributed() > 0 || data.getStatusDistributed() > 0;
	}
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
