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

public class StatusPointPK
{
	private static final CLogger LOGGER = new CLogger(StatusPointPK.class.getName());
	
	public static void onKarmaRemoved(Player player, int karmaRemoved)
	{
		if (!StatusPointConfig.PK_REWARD_ENABLED)
			return;
		
		if (karmaRemoved <= 0)
			return;
		
		int currentKarmaRemoved = player.getMemos().getInteger("pk_karma_removed", 0);
		currentKarmaRemoved += karmaRemoved;
		
		int pointsPerKarma = StatusPointConfig.PK_REWARD_POINTS_PER_KARMA;
		int newPoints = currentKarmaRemoved / pointsPerKarma;
		int remainingKarma = currentKarmaRemoved % pointsPerKarma;
		
		if (newPoints > 0)
		{
			int available = player.getMemos().getInteger("status_points.available", 0);
			player.getMemos().set("status_points.available", available + newPoints);
			player.sendMessage("You gained " + newPoints + " status points from karma removal.");
		}
		
		player.getMemos().set("pk_karma_removed", remainingKarma);
	}
	
	public static void onDeath(Player victim)
	{
		if (!StatusPointConfig.PK_REWARD_ENABLED)
			return;
		
		int karma = victim.getKarma();
		if (karma < StatusPointConfig.PK_MIN_KARMA_FOR_DEATH_PENALTY)
			return;
		
		victim.getMemos().set("status_points.available", 0);
		
		String[] stats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "POWER_ATTACK", "MAGIC_ATTACK", "MOVEMENT_SPEED"};
		for (String stat : stats)
			victim.getMemos().unset("status_points." + stat);
		
		victim.removeStatsByOwner(StatusPointOwner.PVP);
		victim.getMemos().unset("pvp_kills");
		victim.getMemos().unset("pvp_milestone");
		
		victim.getMemos().unset("pk_karma_removed");
		victim.getMemos().unset("status_points.preview");
		victim.getMemos().unset("status_points.isOldChar");
		
		victim.removeStatsByOwner(StatusPointOwner.DISTRIBUTED);
		victim.broadcastUserInfo();
		
		victim.sendMessage("You died with karma! All status points lost.");
	}
}
