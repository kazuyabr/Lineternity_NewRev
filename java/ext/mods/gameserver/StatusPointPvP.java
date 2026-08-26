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

import ext.mods.gameserver.model.actor.Player;
import ext.mods.commons.logging.CLogger;

public class StatusPointPvP
{
	private static final CLogger LOGGER = new CLogger(StatusPointPvP.class.getName());
	
	public static void onPvPKill(Player killer)
	{
		if (!StatusPointConfig.PVP_REWARD_ENABLED)
			return;
		
		CharacterStatusPoints data = killer.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		int milestone = StatusPointConfig.PVP_MILESTONE_KILLS;
		if (milestone <= 0)
			return;
		
		data.sourcePvpPoints++;
		
		if (data.sourcePvpPoints % milestone == 0)
		{
			if (StatusPointConfig.PVP_BONUS_POINTS > 0)
			{
				data.available += StatusPointConfig.PVP_BONUS_POINTS;
				killer.sendMessage("PvP reward: +" + StatusPointConfig.PVP_BONUS_POINTS + " points.");
			}
		}
		
		data.store(killer);
	}
}
