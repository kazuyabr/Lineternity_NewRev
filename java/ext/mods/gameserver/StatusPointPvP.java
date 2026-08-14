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
package ext.mods.gameserver;

import java.util.Arrays;

import ext.mods.gameserver.enums.skills.Stats;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.skills.funcs.FuncStatusPoint;
import ext.mods.commons.logging.CLogger;

public class StatusPointPvP
{
	private static final CLogger LOGGER = new CLogger(StatusPointPvP.class.getName());
	
	public static void onPvPKill(Player killer)
	{
		if (!StatusPointConfig.PVP_REWARD_ENABLED)
			return;
		
		int kills = killer.getMemos().getInteger("pvp_kills", 0) + 1;
		killer.getMemos().set("pvp_kills", kills);
		
		int milestone = StatusPointConfig.PVP_MILESTONE_KILLS;
		if (milestone <= 0)
			return;
		
		if (kills % milestone == 0)
			applyMilestoneBonus(killer);
	}
	
	public static void applyBonuses(Player player)
	{
		if (!StatusPointConfig.PVP_REWARD_ENABLED)
			return;
		
		int milestone = player.getMemos().getInteger("pvp_milestone", 0);
		if (milestone <= 0)
			return;
		
		player.removeStatsByOwner(StatusPointOwner.PVP);
		
		for (String stat : StatusPointConfig.PVP_BONUS_STATS)
		{
			int points = player.getMemos().getInteger("status_points.pvp." + stat, 0);
			if (points > 0)
			{
				try
				{
					Stats enumStat = Stats.valueOf("STAT_" + stat.trim());
					player.addStatFunc(new FuncStatusPoint(player, enumStat, points, StatusPointOwner.PVP));
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.error("Invalid PvP bonus stat: {}", stat);
				}
			}
		}
	}
	
	private static void applyMilestoneBonus(Player player)
	{
		String[] stats = StatusPointConfig.PVP_BONUS_STATS;
		int currentMilestone = player.getMemos().getInteger("pvp_milestone", 0);
		
		for (String stat : stats)
		{
			String trimmed = stat.trim();
			int current = player.getMemos().getInteger("status_points.pvp." + trimmed, 0);
			player.getMemos().set("status_points.pvp." + trimmed, current + StatusPointConfig.PVP_BONUS_PER_MILESTONE);
		}
		
		player.getMemos().set("pvp_milestone", currentMilestone + 1);
		
		player.removeStatsByOwner(StatusPointOwner.PVP);
		
		for (String stat : stats)
		{
			String trimmed = stat.trim();
			int points = player.getMemos().getInteger("status_points.pvp." + trimmed, 0);
			if (points > 0)
			{
				try
				{
					Stats enumStat = Stats.valueOf("STAT_" + trimmed);
					player.addStatFunc(new FuncStatusPoint(player, enumStat, points, StatusPointOwner.PVP));
				}
				catch (IllegalArgumentException e)
				{
					LOGGER.error("Invalid PvP bonus stat: {}", trimmed);
				}
			}
		}
		
		player.broadcastUserInfo();
		player.sendMessage("PvP milestone reached! +" + StatusPointConfig.PVP_BONUS_PER_MILESTONE + " to " + String.join(", ", stats) + ".");
	}
}
