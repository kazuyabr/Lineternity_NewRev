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
 */
package ext.mods.gameserver;

import ext.mods.gameserver.model.actor.Player;
import ext.mods.commons.logging.CLogger;

public class StatusPointSiege
{
	private static final CLogger LOGGER = new CLogger(StatusPointSiege.class.getName());
	
	public static void onSiegeWin(Player[] winningClanMembers)
	{
		if (!StatusPointConfig.SIEGE_REWARD_ENABLED)
			return;
		
		if (StatusPointConfig.SIEGE_REWARD_ATTR_POINTS <= 0 && StatusPointConfig.SIEGE_REWARD_STATUS_POINTS <= 0)
			return;
		
		if (winningClanMembers == null)
			return;
		
		for (Player player : winningClanMembers)
		{
			if (player == null || !player.isOnline())
				continue;
			
			CharacterStatusPoints data = player.getStatusPointsData();
			if (data == null || data.isOldChar)
				continue;
			
			if (StatusPointConfig.SIEGE_REWARD_ATTR_POINTS > 0)
			{
				data.attrAvailable += StatusPointConfig.SIEGE_REWARD_ATTR_POINTS;
				data.sourceSiegePoints += StatusPointConfig.SIEGE_REWARD_ATTR_POINTS;
			}
			
			if (StatusPointConfig.SIEGE_REWARD_STATUS_POINTS > 0)
			{
				data.statusAvailable += StatusPointConfig.SIEGE_REWARD_STATUS_POINTS;
				data.sourceSiegePoints += StatusPointConfig.SIEGE_REWARD_STATUS_POINTS;
			}
			
			data.store(player);
			player.sendMessage("Siege victory reward: +" + StatusPointConfig.SIEGE_REWARD_ATTR_POINTS + " attr, +" + StatusPointConfig.SIEGE_REWARD_STATUS_POINTS + " status points.");
		}
	}
}
