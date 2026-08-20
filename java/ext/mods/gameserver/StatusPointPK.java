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

public class StatusPointPK
{
	public static void onKarmaRemoved(Player player, int karmaRemoved)
	{
		if (!StatusPointConfig.PK_REWARD_ENABLED || karmaRemoved <= 0)
			return;
		
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		int totalKarmaRemoved = karmaRemoved;
		
		if (data.karmaPenaltyAttr > 0 && StatusPointConfig.PK_REMOVED_PER_POINT > 0)
		{
			int pointsToRecover = totalKarmaRemoved / StatusPointConfig.PK_REMOVED_PER_POINT;
			if (pointsToRecover > data.karmaPenaltyAttr)
				pointsToRecover = data.karmaPenaltyAttr;
			
			if (pointsToRecover > 0)
			{
				data.karmaPenaltyAttr -= pointsToRecover;
				data.available += pointsToRecover;
				player.sendMessage("Karma penalty reduced: +" + pointsToRecover + " status points recovered.");
			}
		}
		
		data.store(player);
	}
	
	public static void onDeath(Player victim)
	{
		if (!StatusPointConfig.PK_REWARD_ENABLED)
			return;
		
		int karma = victim.getKarma();
		if (karma <= 0)
			return;
		
		CharacterStatusPoints data = victim.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		if (StatusPointConfig.DEATH_WITH_KARMA_REMOVE_ALL_POINTS)
		{
			int totalDistributed = data.getTotalDistributed();
			if (totalDistributed > 0)
			{
				data.karmaPenaltyAttr += totalDistributed;
				
				String[] allStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT"};
				for (String stat : allStats)
					data.setDistributedValue(stat, 0);
				data.attrDistributed = 0;
				data.statusDistributed = 0;
				data.available = 0;
				
				victim.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
				victim.sendMessage("You died with karma! Lost " + totalDistributed + " status points (recoverable via karma removal).");
			}
		}
		
		data.store(victim);
		victim.broadcastUserInfo();
	}
}
