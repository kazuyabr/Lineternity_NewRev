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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

public class StatusPointConfig
{
	private static final CLogger LOGGER = new CLogger(StatusPointConfig.class.getName());
	
	// Core
	public static boolean STATUS_POINTS_ENABLED;
	public static int STATUS_POINT_ACTIVATION_DATE;
	
	// Attribute pool
	public static int ATTRIBUTE_POINTS_PER_LEVEL;
	public static int MAX_TOTAL_DEX;
	public static int MAX_TOTAL_WIT;
	
	// Mount pet move speed bonus
	public static final Map<Integer, Integer> MOUNT_MOVE_SPEED_BONUS = new HashMap<>();
	
	// Direct status pool
	public static int DIRECT_STATUS_POINTS_PER_LEVEL;
	public static int PDEF_PER_POINT;
	public static int MAX_DIRECT_PDEF;
	public static int MAX_DIRECT_MDEF;
	public static int MAX_DIRECT_HP;
	public static int MAX_DIRECT_MP;
	public static int MAX_DIRECT_CP;
	public static int MAX_DIRECT_PATK;
	public static int MAX_DIRECT_MATK;
	public static int MAX_DIRECT_ACCURACY;
	public static int MAX_DIRECT_EVASION;
	public static int MAX_DIRECT_CRIT;
	
	// Speed caps
	public static boolean SPEED_CAP_ENABLED;
	public static int MAX_ATTACK_SPEED_POINTS;
	public static int MAX_MAGIC_ATTACK_SPEED_POINTS;
	public static int MAX_MOVEMENT_SPEED_POINTS;
	
	// Reset
	public static final Map<Integer, Integer> RESET_COST_ITEMS = new HashMap<>();
	public static boolean PREMIUM_EXEMPT_FROM_RESET_COST;
	
	// PK
	public static boolean PK_REWARD_ENABLED;
	public static int PK_REMOVED_PER_ATTR_POINT;
	public static int PK_REMOVED_PER_STATUS_POINT;
	public static boolean DEATH_WITH_KARMA_REMOVE_ALL_ATTRIBUTES;
	
	// PvP
	public static boolean PVP_REWARD_ENABLED;
	public static int PVP_MILESTONE_KILLS;
	public static String[] PVP_BONUS_STATS;
	public static int PVP_BONUS_PER_MILESTONE;
	public static int PVP_BONUS_STATUS_PER_MILESTONE;
	
	// Raid
	public static boolean RAID_REWARD_ENABLED;
	public static int RAID_REWARD_ATTR_POINTS;
	public static int RAID_REWARD_STATUS_POINTS;
	public static int RAID_REWARD_TOP_N;
	
	// Siege
	public static boolean SIEGE_REWARD_ENABLED;
	public static int SIEGE_REWARD_ATTR_POINTS;
	public static int SIEGE_REWARD_STATUS_POINTS;
	
	private static final String STATUS_POINTS_FILE = Config.CONFIG_PATH.resolve("statuspoints.properties").toString();
	private static final String PK_REWARDS_FILE = Config.CONFIG_PATH.resolve("pkrewards.properties").toString();
	private static final String PVP_REWARDS_FILE = Config.CONFIG_PATH.resolve("pvprewards.properties").toString();
	private static final String RAID_REWARDS_FILE = Config.CONFIG_PATH.resolve("raidrewards.properties").toString();
	private static final String SIEGE_REWARDS_FILE = Config.CONFIG_PATH.resolve("siegerewards.properties").toString();
	
	public static void load()
	{
		final ExProperties sp = Config.initProperties(STATUS_POINTS_FILE);
		
		STATUS_POINTS_ENABLED = sp.getProperty("StatusPointsEnabled", false);
		
		String activationDateStr = sp.getProperty("StatusPointActivationDate", "2026-01-01");
		try
		{
			STATUS_POINT_ACTIVATION_DATE = (int) LocalDate.parse(activationDateStr).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		}
		catch (Exception e)
		{
			STATUS_POINT_ACTIVATION_DATE = (int) LocalDate.parse("2026-01-01").atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
		}
		
		ATTRIBUTE_POINTS_PER_LEVEL = sp.getProperty("AttributePointsPerLevel", 2);
		MAX_TOTAL_DEX = sp.getProperty("MaxTotalDex", 40);
		MAX_TOTAL_WIT = sp.getProperty("MaxTotalWit", 40);
		
		String mountMoveSpeedStr = sp.getProperty("MountMoveSpeedBonus", "");
		if (!mountMoveSpeedStr.isEmpty())
		{
			String[] entries = mountMoveSpeedStr.split(",");
			for (String entry : entries)
			{
				String[] parts = entry.trim().split(":");
				if (parts.length == 2)
				{
					int npcId = Integer.parseInt(parts[0].trim());
					int speedBonus = Integer.parseInt(parts[1].trim());
					MOUNT_MOVE_SPEED_BONUS.put(npcId, speedBonus);
				}
			}
		}
		
		DIRECT_STATUS_POINTS_PER_LEVEL = sp.getProperty("DirectStatusPointsPerLevel", 1);
		PDEF_PER_POINT = sp.getProperty("PDefPerPoint", 1);
		MAX_DIRECT_PDEF = sp.getProperty("MaxDirectPDef", 200);
		MAX_DIRECT_MDEF = sp.getProperty("MaxDirectMDef", 100);
		MAX_DIRECT_HP = sp.getProperty("MaxDirectHp", 500);
		MAX_DIRECT_MP = sp.getProperty("MaxDirectMp", 200);
		MAX_DIRECT_CP = sp.getProperty("MaxDirectCp", 300);
		MAX_DIRECT_PATK = sp.getProperty("MaxDirectPAtk", 50);
		MAX_DIRECT_MATK = sp.getProperty("MaxDirectMAtk", 50);
		MAX_DIRECT_ACCURACY = sp.getProperty("MaxDirectAccuracy", 20);
		MAX_DIRECT_EVASION = sp.getProperty("MaxDirectEvasion", 20);
		MAX_DIRECT_CRIT = sp.getProperty("MaxDirectCrit", 20);
		
		SPEED_CAP_ENABLED = true;
		MAX_ATTACK_SPEED_POINTS = sp.getProperty("MaxAttackSpeedPoints", 1300);
		MAX_MAGIC_ATTACK_SPEED_POINTS = sp.getProperty("MaxMagicAttackSpeedPoints", 2200);
		MAX_MOVEMENT_SPEED_POINTS = sp.getProperty("MaxMovementSpeedPoints", 220);
		
		String resetCostStr = sp.getProperty("ResetCostItems", "9143:1");
		if (!resetCostStr.isEmpty())
		{
			String[] entries = resetCostStr.split(",");
			for (String entry : entries)
			{
				String[] parts = entry.trim().split(":");
				if (parts.length == 2)
				{
					int itemId = Integer.parseInt(parts[0].trim());
					int count = Integer.parseInt(parts[1].trim());
					RESET_COST_ITEMS.put(itemId, count);
				}
			}
		}
		PREMIUM_EXEMPT_FROM_RESET_COST = sp.getProperty("PremiumExemptFromResetCost", false);
		
		final ExProperties pk = Config.initProperties(PK_REWARDS_FILE);
		
		PK_REWARD_ENABLED = pk.getProperty("PKRewardEnabled", true);
		PK_REMOVED_PER_ATTR_POINT = pk.getProperty("KarmaRemovedPerAttributePoint", 10);
		PK_REMOVED_PER_STATUS_POINT = pk.getProperty("KarmaRemovedPerStatusPoint", 20);
		DEATH_WITH_KARMA_REMOVE_ALL_ATTRIBUTES = pk.getProperty("DeathWithKarmaRemoveAllAttributes", true);
		
		final ExProperties pvp = Config.initProperties(PVP_REWARDS_FILE);
		
		PVP_REWARD_ENABLED = pvp.getProperty("PVPRewardEnabled", true);
		PVP_MILESTONE_KILLS = pvp.getProperty("PVPMilestoneKills", 50);
		PVP_BONUS_PER_MILESTONE = pvp.getProperty("PVPBonusAttributePoints", 1);
		PVP_BONUS_STATUS_PER_MILESTONE = pvp.getProperty("PVPBonusStatusPoints", 0);
		PVP_BONUS_STATS = pvp.getProperty("PVPBonusStats", "STR,CON,INT,MEN").split(",");
		
		final ExProperties raid = Config.initProperties(RAID_REWARDS_FILE);
		
		RAID_REWARD_ENABLED = raid.getProperty("RaidRewardEnabled", true);
		RAID_REWARD_ATTR_POINTS = raid.getProperty("RaidRewardAttributePoints", 5);
		RAID_REWARD_STATUS_POINTS = raid.getProperty("RaidRewardStatusPoints", 2);
		RAID_REWARD_TOP_N = raid.getProperty("RaidRewardTopN", 5);
		
		final ExProperties siege = Config.initProperties(SIEGE_REWARDS_FILE);
		
		SIEGE_REWARD_ENABLED = siege.getProperty("SiegeRewardEnabled", true);
		SIEGE_REWARD_ATTR_POINTS = siege.getProperty("SiegeRewardAttributePoints", 10);
		SIEGE_REWARD_STATUS_POINTS = siege.getProperty("SiegeRewardStatusPoints", 5);
		
		LOGGER.info("Loaded " + STATUS_POINTS_FILE);
		LOGGER.info("Loaded " + PK_REWARDS_FILE);
		LOGGER.info("Loaded " + PVP_REWARDS_FILE);
		LOGGER.info("Loaded " + RAID_REWARDS_FILE);
		LOGGER.info("Loaded " + SIEGE_REWARDS_FILE);
	}
	
	public static int getMountMoveSpeedBonus(int npcId)
	{
		return MOUNT_MOVE_SPEED_BONUS.getOrDefault(npcId, 0);
	}
	
	public static String getResetCostDisplay()
	{
		if (RESET_COST_ITEMS.isEmpty())
			return "Free";
		
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Integer> entry : RESET_COST_ITEMS.entrySet())
		{
			if (sb.length() > 0)
				sb.append(" + ");
			
			ext.mods.gameserver.data.xml.ItemData itemData = ext.mods.gameserver.data.xml.ItemData.getInstance();
			ext.mods.gameserver.model.item.kind.Item item = itemData.getTemplate(entry.getKey());
			String name = (item != null) ? item.getName() : "Item#" + entry.getKey();
			sb.append(entry.getValue()).append("x ").append(name);
		}
		return sb.toString();
	}
	
	public static boolean hasResetCostItems(ext.mods.gameserver.model.actor.Player player)
	{
		for (Map.Entry<Integer, Integer> entry : RESET_COST_ITEMS.entrySet())
		{
			var itemInstance = player.getInventory().getItemByItemId(entry.getKey());
			if (itemInstance == null || itemInstance.getCount() < entry.getValue())
				return false;
		}
		return true;
	}
	
	public static void chargeResetCost(ext.mods.gameserver.model.actor.Player player)
	{
		for (Map.Entry<Integer, Integer> entry : RESET_COST_ITEMS.entrySet())
			player.destroyItemByItemId(entry.getKey(), entry.getValue(), true);
	}
}
