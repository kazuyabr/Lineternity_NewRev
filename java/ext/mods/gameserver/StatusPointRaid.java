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

import java.util.List;

import ext.mods.gameserver.model.actor.Player;
import ext.mods.commons.logging.CLogger;

public class StatusPointRaid
{
	private static final CLogger LOGGER = new CLogger(StatusPointRaid.class.getName());
	
	public static void onRaidBossKilled(List<Player> topDealers)
	{
		if (!StatusPointConfig.RAID_REWARD_ENABLED)
			return;
		
		if (StatusPointConfig.RAID_REWARD_POINTS <= 0)
			return;
		
		if (topDealers == null || topDealers.isEmpty())
			return;
		
		int maxRewards = Math.min(topDealers.size(), StatusPointConfig.RAID_REWARD_TOP_N);
		
		for (int i = 0; i < maxRewards; i++)
		{
			Player player = topDealers.get(i);
			if (player == null || !player.isOnline())
				continue;
			
			CharacterStatusPoints data = player.getStatusPointsData();
			if (data == null || data.isOldChar)
				continue;
			
			data.available += StatusPointConfig.RAID_REWARD_POINTS;
			data.sourceRaidPoints += StatusPointConfig.RAID_REWARD_POINTS;
			
			data.store(player);
			player.sendMessage("Raid Boss reward: +" + StatusPointConfig.RAID_REWARD_POINTS + " status points.");
		}
	}
}
