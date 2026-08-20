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
import ext.mods.gameserver.CharacterStatusPoints;
import ext.mods.gameserver.StatusPointConfig;
import ext.mods.gameserver.StatusPointOwner;
import ext.mods.gameserver.data.HTMLData;
import ext.mods.gameserver.communitybbs.manager.BaseBBSManager;

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
		
		String html = HTMLData.getInstance().getHtm(player.getLocale(), "html/mods/statuspoint/statuspoint.htm");
		if (html == null || html.isEmpty())
			return;
		
		boolean isOldChar = data.isOldChar;
		
		String[] allStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT"};
		String[] allKeys = {"str", "con", "dex", "int", "wit", "men", "pdef", "mdef", "hp", "mp", "cp", "patk", "matk", "acc", "eva", "crit"};
		
		for (int i = 0; i < allStats.length; i++)
		{
			String stat = allStats[i];
			String key = allKeys[i];
			int preview = getPreviewValue(data, stat);
			int dist = data.getDistributedValue(stat);
			int confirmed = data.getConfirmedValue(stat);
			int unconfirmed = dist - confirmed;
			int cost = data.getCostToNext(stat);
			boolean atCap = data.isAtCap(stat);
			boolean maxed = isMaxed(player, stat, data);
			boolean canAfford = data.available >= cost;
			boolean atCapAndNoItems = atCap && !StatusPointConfig.hasCapItems(player);
			
			if (maxed)
				html = html.replace("%" + key + "_display%", "<font color=40E0D0>" + preview + " MAX</font>");
			else if (unconfirmed > 0)
				html = html.replace("%" + key + "_display%", preview + " <font color=808080>(+" + unconfirmed + ")</font>");
			else
				html = html.replace("%" + key + "_display%", String.valueOf(preview));
			
			String costHtml = "";
			if (!maxed && !isOldChar)
			{
				if (atCap && !StatusPointConfig.COST_CAP_ITEMS.isEmpty())
					costHtml = "<font color=FF0000>" + cost + " pts<br1>" + StatusPointConfig.getCapItemsDisplay() + "</font>";
				else
					costHtml = "<font color=FF0000>" + cost + " pts</font>";
			}
			html = html.replace("%" + key + "_cost%", costHtml);
		}
		
		boolean canConfirm = !isOldChar && data.dirty;
		boolean showReset = isOldChar || data.getTotalDistributed() > 0;
		
		html = html.replace("%confirm_button%", canConfirm ? makeButton("Confirm", "confirm") : "");
		html = html.replace("%reset_button%", showReset ? makeButton("Reset", "reset") : "");
		html = html.replace("%reset_cost%", StatusPointConfig.getResetCostDisplay());
		html = html.replace("%available%", String.valueOf(data.available));
		html = html.replace("%distributed%", String.valueOf(data.getTotalDistributed()));
		
		if (data.karmaPenaltyAttr > 0)
			html = html.replace("%karma_display%", "<font color=LEVEL>Karma Penalty: <font color=FF0000>" + data.karmaPenaltyAttr + "</font></font>");
		else
			html = html.replace("%karma_display%", "");
		
		for (int i = 0; i < allStats.length; i++)
		{
			String stat = allStats[i];
			String key = allKeys[i];
			int dist = data.getDistributedValue(stat);
			int confirmed = data.getConfirmedValue(stat);
			int cost = data.getCostToNext(stat);
			boolean maxed = isMaxed(player, stat, data);
			boolean canAfford = data.available >= cost;
			boolean atCap = data.isAtCap(stat);
			boolean atCapAndNoItems = atCap && !StatusPointConfig.hasCapItems(player);
			
			boolean showPlus = !isOldChar && !maxed && canAfford && !atCapAndNoItems;
			boolean showMinus = data.dirty && (dist > confirmed) && !isOldChar;
			html = html.replace("%" + key + "_buttons%", makePlusMinus(stat, showPlus, showMinus));
		}
		
		BaseBBSManager.separateAndSend(html, player);
	}
	
	private String makePlusMinus(String stat, boolean showPlus, boolean showMinus)
	{
		if (!showPlus && !showMinus)
			return "";
		
		String plus = showPlus
			? "<button value=\"+\" action=\"bypass voiced_statuspoint add " + stat + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>"
			: "";
		String minus = showMinus
			? "<button value=\"-\" action=\"bypass voiced_statuspoint remove " + stat + "\" width=65 height=19 back=L2UI_ch3.smallbutton2_over fore=L2UI_ch3.smallbutton2>"
			: "";
		
		return "<table cellpadding=0 cellspacing=0><tr><td align=center>" + plus + "</td><td align=center>" + minus + "</td></tr></table>";
	}
	
	private String makeButton(String value, String action)
	{
		return "<table cellpadding=0 cellspacing=0><tr><td align=center><button value=\"" + value + "\" action=\"bypass voiced_statuspoint " + action + "\" width=74 height=21 back=L2UI_ch3.Btn1_normalOn fore=L2UI_ch3.Btn1_normal></td></tr></table>";
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
			
			int cost = data.getCostToNext(stat);
			if (data.available < cost)
			{
				player.sendMessage("Not enough points. Need " + cost + " points.");
				break;
			}
			if (isMaxed(player, stat, data))
				break;
			if (data.isAtCap(stat) && !StatusPointConfig.hasCapItems(player))
			{
				player.sendMessage("At cost cap. Need " + StatusPointConfig.getCapItemsDisplay() + " to continue.");
				break;
			}
			
			if (data.isAtCap(stat))
				StatusPointConfig.chargeCapItems(player);
			
			int current = data.getDistributedValue(stat);
			data.setDistributedValue(stat, current + 1);
			data.available -= cost;
			updateDistributedCount(data, stat);
			data.dirty = true;
			break;
		}
		case "remove":
		{
			CharacterStatusPoints data = player.getStatusPointsData();
			if (data == null || data.isOldChar)
				break;
			
			int current = data.getDistributedValue(stat);
			int confirmed = data.getConfirmedValue(stat);
			if (current <= confirmed)
				break;
			
			int refund = StatusPointConfig.getCostForStat(current - 1);
			data.setDistributedValue(stat, current - 1);
			data.available += refund;
			updateDistributedCount(data, stat);
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
			
			String[] allStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT"};
			for (String statName : allStats)
				data.setConfirmedValue(statName, data.getDistributedValue(statName));
			
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
			
			if (data.getTotalDistributed() <= 0)
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
			
			ext.mods.gameserver.model.actor.Player.applyStatusPointFuncs(player, data);
			ext.mods.gameserver.model.actor.Player.applyDirectStatusFuncs(player, data);
			data.computeEffectiveBases(player);
			data.dirty = false;
			data.store(player);
			player.broadcastUserInfo();
			player.sendMessage("Status points reset successfully. " + totalSpent + " points returned.");
			break;
		}
		}
		
		showHtml(player);
	}
	
	private void updateDistributedCount(CharacterStatusPoints data, String stat)
	{
		data.attrDistributed = data.attrStr + data.attrCon + data.attrDex + data.attrInt + data.attrWit + data.attrMen;
		data.statusDistributed = data.statusPdef + data.statusMdef + data.statusHp + data.statusMp + data.statusCp
			+ data.statusPatk + data.statusMatk + data.statusAccuracy + data.statusEvasion + data.statusCrit;
	}
	
	private boolean isDirectStat(String stat)
	{
		return switch (stat)
		{
			case "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT" -> true;
			default -> false;
		};
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
	
	private int getPreviewValue(CharacterStatusPoints data, String stat)
	{
		int dist = data.getDistributedValue(stat);
		return switch (stat)
		{
			case "STR" -> data.previewBaseStr + dist;
			case "CON" -> data.previewBaseCon + dist;
			case "DEX" -> data.previewBaseDex + dist;
			case "INT" -> data.previewBaseInt + dist;
			case "WIT" -> data.previewBaseWit + dist;
			case "MEN" -> data.previewBaseMen + dist;
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
	
	@Override
	public String[] getVoicedCommandList()
	{
		return VOICED_COMMANDS;
	}
}
