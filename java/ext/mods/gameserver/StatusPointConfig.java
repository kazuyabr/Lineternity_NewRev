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

import ext.mods.Config;
import ext.mods.commons.config.ExProperties;
import ext.mods.commons.logging.CLogger;
import java.nio.file.Path;

public class StatusPointConfig
{
	private static final CLogger LOGGER = new CLogger(StatusPointConfig.class.getName());
	
	public static boolean STATUS_POINTS_ENABLED;
	public static int PDEF_PER_POINT;
	public static int MAX_DEX_POINTS;
	public static int MAX_WIT_POINTS;
	public static int MAX_ATTACK_SPEED_POINTS;
	public static int MAX_MAGIC_ATTACK_SPEED_POINTS;
	public static int MAX_MOVEMENT_SPEED_POINTS;
	public static int RESET_ITEM_ID;
	public static int RESET_ITEM_COUNT;
	public static int RESET_ADENA;
	public static boolean PREMIUM_EXEMPT_FROM_RESET_COST;
	
	public static boolean PK_REWARD_ENABLED;
	public static int PK_REWARD_POINTS_PER_KARMA;
	public static int PK_MIN_KARMA_FOR_DEATH_PENALTY;
	public static int PK_DEATH_KARMA_LOSS;
	
	public static boolean PVP_REWARD_ENABLED;
	public static int PVP_MILESTONE_KILLS;
	public static String[] PVP_BONUS_STATS;
	public static int PVP_BONUS_PER_MILESTONE;
	
	private static final String STATUS_POINTS_FILE = Config.CONFIG_PATH.resolve("statuspoints.properties").toString();
	private static final String PK_REWARDS_FILE = Config.CONFIG_PATH.resolve("pkrewards.properties").toString();
	private static final String PVP_REWARDS_FILE = Config.CONFIG_PATH.resolve("pvprewards.properties").toString();
	
	public static void load()
	{
		final ExProperties statusPoints = Config.initProperties(STATUS_POINTS_FILE);
		
		STATUS_POINTS_ENABLED = statusPoints.getProperty("StatusPointsEnabled", false);
		PDEF_PER_POINT = statusPoints.getProperty("PDefPerPoint", 1);
		MAX_DEX_POINTS = statusPoints.getProperty("MaxDexPoints", 8);
		MAX_WIT_POINTS = statusPoints.getProperty("MaxWitPoints", 8);
		MAX_ATTACK_SPEED_POINTS = statusPoints.getProperty("MaxAttackSpeedPoints", 8);
		MAX_MAGIC_ATTACK_SPEED_POINTS = statusPoints.getProperty("MaxMagicAttackSpeedPoints", 8);
		MAX_MOVEMENT_SPEED_POINTS = statusPoints.getProperty("MaxMovementSpeedPoints", 8);
		RESET_ITEM_ID = statusPoints.getProperty("ResetItemId", 9143);
		RESET_ITEM_COUNT = statusPoints.getProperty("ResetItemCount", 1);
		RESET_ADENA = statusPoints.getProperty("ResetAdena", 100000);
		PREMIUM_EXEMPT_FROM_RESET_COST = statusPoints.getProperty("PremiumExemptFromResetCost", false);
		
		final ExProperties pkRewards = Config.initProperties(PK_REWARDS_FILE);
		
		PK_REWARD_ENABLED = pkRewards.getProperty("PKRewardEnabled", true);
		PK_REWARD_POINTS_PER_KARMA = pkRewards.getProperty("PKRewardPointsPerKarma", 100);
		PK_MIN_KARMA_FOR_DEATH_PENALTY = pkRewards.getProperty("PKMinKarmaForDeathPenalty", 1000);
		PK_DEATH_KARMA_LOSS = pkRewards.getProperty("PKDeathKarmaLoss", 500);
		
		final ExProperties pvpRewards = Config.initProperties(PVP_REWARDS_FILE);
		
		PVP_REWARD_ENABLED = pvpRewards.getProperty("PVPRewardEnabled", true);
		PVP_MILESTONE_KILLS = pvpRewards.getProperty("PVPMilestoneKills", 50);
		PVP_BONUS_PER_MILESTONE = pvpRewards.getProperty("PVPBonusPerMilestone", 1);
		PVP_BONUS_STATS = pvpRewards.getProperty("PVPBonusStats", "STR,CON,INT,MEN").split(",");
		
		LOGGER.info("Loaded " + STATUS_POINTS_FILE);
		LOGGER.info("Loaded " + PK_REWARDS_FILE);
		LOGGER.info("Loaded " + PVP_REWARDS_FILE);
	}
}
