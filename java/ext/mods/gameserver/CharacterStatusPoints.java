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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import ext.mods.commons.logging.CLogger;
import ext.mods.commons.pool.ConnectionPool;
import ext.mods.gameserver.model.actor.Player;

public class CharacterStatusPoints
{
	private static final CLogger LOGGER = new CLogger(CharacterStatusPoints.class.getName());
	
	// Subclass index (0 = base, 1-3 = subclass)
	public int classIndex;
	
	// Unified pool
	public int available;
	
	// Attribute distributed (STR/CON/DEX/INT/WIT/MEN)
	public int attrDistributed;
	public int attrStr;
	public int attrCon;
	public int attrDex;
	public int attrInt;
	public int attrWit;
	public int attrMen;
	
	// Direct status distributed (P.Def/M.Def/HP/MP/CP/P.Atk/M.Atk/Acc/Eva/Crit)
	public int statusDistributed;
	public int statusPdef;
	public int statusMdef;
	public int statusHp;
	public int statusMp;
	public int statusCp;
	public int statusPatk;
	public int statusMatk;
	public int statusAccuracy;
	public int statusEvasion;
	public int statusCrit;
	
	// Karma
	public int karmaPenaltyAttr;
	
	// Source tracking
	public int sourceLevelPoints;
	public int sourceQuestPoints;
	public int sourceRaidPoints;
	public int sourceSiegePoints;
	public int sourcePvpPoints;
	public int sourceKarmaPoints;
	public int sourceChestPoints;
	
	// Meta
	public int version;
	public boolean isOldChar;
	
	// Transient preview fields (not saved to DB)
	public boolean dirty;
	public int previewBaseStr;
	public int previewBaseCon;
	public int previewBaseDex;
	public int previewBaseInt;
	public int previewBaseWit;
	public int previewBaseMen;
	public int previewBasePdef;
	public int previewBaseMdef;
	public int previewBaseHp;
	public int previewBaseMp;
	public int previewBaseCp;
	public int previewBasePatk;
	public int previewBaseMatk;
	public int previewBaseAccuracy;
	public int previewBaseEvasion;
	public int previewBaseCrit;
	
	// Confirmed distributed values (snapshot at last Confirm/Reset)
	public int confirmedAttrStr;
	public int confirmedAttrCon;
	public int confirmedAttrDex;
	public int confirmedAttrInt;
	public int confirmedAttrWit;
	public int confirmedAttrMen;
	public int confirmedStatusPdef;
	public int confirmedStatusMdef;
	public int confirmedStatusHp;
	public int confirmedStatusMp;
	public int confirmedStatusCp;
	public int confirmedStatusPatk;
	public int confirmedStatusMatk;
	public int confirmedStatusAccuracy;
	public int confirmedStatusEvasion;
	public int confirmedStatusCrit;
	
	public int getTotalDistributed()
	{
		return attrStr + attrCon + attrDex + attrInt + attrWit + attrMen
			+ statusPdef + statusMdef + statusHp + statusMp + statusCp
			+ statusPatk + statusMatk + statusAccuracy + statusEvasion + statusCrit;
	}
	
	public int getAttrDistributed()
	{
		return attrStr + attrCon + attrDex + attrInt + attrWit + attrMen;
	}
	
	public int getStatusDistributed()
	{
		return statusPdef + statusMdef + statusHp + statusMp + statusCp
			+ statusPatk + statusMatk + statusAccuracy + statusEvasion + statusCrit;
	}
	
	public int getCostToNext(String stat)
	{
		int current = getDistributedValue(stat);
		return StatusPointConfig.getCostForStat(current);
	}
	
	public boolean isAtCap(String stat)
	{
		int current = getDistributedValue(stat);
		return StatusPointConfig.isAtCap(current);
	}
	
	public int getDistributedValue(String stat)
	{
		return switch (stat)
		{
			case "STR" -> attrStr;
			case "CON" -> attrCon;
			case "DEX" -> attrDex;
			case "INT" -> attrInt;
			case "WIT" -> attrWit;
			case "MEN" -> attrMen;
			case "PDEF" -> statusPdef;
			case "MDEF" -> statusMdef;
			case "HP" -> statusHp;
			case "MP" -> statusMp;
			case "CP" -> statusCp;
			case "PATK" -> statusPatk;
			case "MATK" -> statusMatk;
			case "ACC" -> statusAccuracy;
			case "EVA" -> statusEvasion;
			case "CRIT" -> statusCrit;
			default -> 0;
		};
	}
	
	public void setDistributedValue(String stat, int value)
	{
		switch (stat)
		{
			case "STR" -> attrStr = value;
			case "CON" -> attrCon = value;
			case "DEX" -> attrDex = value;
			case "INT" -> attrInt = value;
			case "WIT" -> attrWit = value;
			case "MEN" -> attrMen = value;
			case "PDEF" -> statusPdef = value;
			case "MDEF" -> statusMdef = value;
			case "HP" -> statusHp = value;
			case "MP" -> statusMp = value;
			case "CP" -> statusCp = value;
			case "PATK" -> statusPatk = value;
			case "MATK" -> statusMatk = value;
			case "ACC" -> statusAccuracy = value;
			case "EVA" -> statusEvasion = value;
			case "CRIT" -> statusCrit = value;
		}
	}
	
	public int getConfirmedValue(String stat)
	{
		return switch (stat)
		{
			case "STR" -> confirmedAttrStr;
			case "CON" -> confirmedAttrCon;
			case "DEX" -> confirmedAttrDex;
			case "INT" -> confirmedAttrInt;
			case "WIT" -> confirmedAttrWit;
			case "MEN" -> confirmedAttrMen;
			case "PDEF" -> confirmedStatusPdef;
			case "MDEF" -> confirmedStatusMdef;
			case "HP" -> confirmedStatusHp;
			case "MP" -> confirmedStatusMp;
			case "CP" -> confirmedStatusCp;
			case "PATK" -> confirmedStatusPatk;
			case "MATK" -> confirmedStatusMatk;
			case "ACC" -> confirmedStatusAccuracy;
			case "EVA" -> confirmedStatusEvasion;
			case "CRIT" -> confirmedStatusCrit;
			default -> 0;
		};
	}
	
	public void setConfirmedValue(String stat, int value)
	{
		switch (stat)
		{
			case "STR" -> confirmedAttrStr = value;
			case "CON" -> confirmedAttrCon = value;
			case "DEX" -> confirmedAttrDex = value;
			case "INT" -> confirmedAttrInt = value;
			case "WIT" -> confirmedAttrWit = value;
			case "MEN" -> confirmedAttrMen = value;
			case "PDEF" -> confirmedStatusPdef = value;
			case "MDEF" -> confirmedStatusMdef = value;
			case "HP" -> confirmedStatusHp = value;
			case "MP" -> confirmedStatusMp = value;
			case "CP" -> confirmedStatusCp = value;
			case "PATK" -> confirmedStatusPatk = value;
			case "MATK" -> confirmedStatusMatk = value;
			case "ACC" -> confirmedStatusAccuracy = value;
			case "EVA" -> confirmedStatusEvasion = value;
			case "CRIT" -> confirmedStatusCrit = value;
		}
	}
	
	public int getTotalSpent()
	{
		int total = 0;
		String[] allStats = {"STR", "CON", "DEX", "INT", "WIT", "MEN", "PDEF", "MDEF", "HP", "MP", "CP", "PATK", "MATK", "ACC", "EVA", "CRIT"};
		for (String stat : allStats)
		{
			int distributed = getDistributedValue(stat);
			for (int i = 0; i < distributed; i++)
				total += StatusPointConfig.getCostForStat(i);
		}
		return total;
	}
	
	public static CharacterStatusPoints load(Player player)
	{
		CharacterStatusPoints data = new CharacterStatusPoints();
		data.classIndex = player.getClassIndex();
		
		try (Connection con = ConnectionPool.getConnection())
		{
			PreparedStatement ps = con.prepareStatement("SELECT * FROM character_status_points WHERE char_id = ? AND class_index = ?");
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, data.classIndex);
			ResultSet rs = ps.executeQuery();
			
			if (rs.next())
			{
				data.available = rs.getInt("available");
				data.attrDistributed = rs.getInt("attr_distributed");
				data.attrStr = rs.getInt("attr_str");
				data.attrCon = rs.getInt("attr_con");
				data.attrDex = rs.getInt("attr_dex");
				data.attrInt = rs.getInt("attr_int");
				data.attrWit = rs.getInt("attr_wit");
				data.attrMen = rs.getInt("attr_men");
				
				data.statusDistributed = rs.getInt("status_distributed");
				data.statusPdef = rs.getInt("status_pdef");
				data.statusMdef = rs.getInt("status_mdef");
				data.statusHp = rs.getInt("status_hp");
				data.statusMp = rs.getInt("status_mp");
				data.statusCp = rs.getInt("status_cp");
				data.statusPatk = rs.getInt("status_patk");
				data.statusMatk = rs.getInt("status_matk");
				data.statusAccuracy = rs.getInt("status_accuracy");
				data.statusEvasion = rs.getInt("status_evasion");
				data.statusCrit = rs.getInt("status_crit");
				
				data.karmaPenaltyAttr = rs.getInt("karma_penalty_attr");
				
				data.sourceLevelPoints = rs.getInt("source_level_points");
				data.sourceQuestPoints = rs.getInt("source_quest_points");
				data.sourceRaidPoints = rs.getInt("source_raid_points");
				data.sourceSiegePoints = rs.getInt("source_siege_points");
				data.sourcePvpPoints = rs.getInt("source_pvp_points");
				data.sourceKarmaPoints = rs.getInt("source_karma_points");
				
				try
				{
					data.sourceChestPoints = rs.getInt("source_chest_points");
				}
				catch (SQLException e)
				{
					// Migration 006 not applied yet
					data.sourceChestPoints = 0;
				}
				
				data.version = rs.getInt("version");
				
				data.isOldChar = data.version < 3 && player.getCreateTime() < StatusPointConfig.STATUS_POINT_ACTIVATION_DATE;
				
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
			}
			else
			{
				data.isOldChar = player.getCreateTime() < StatusPointConfig.STATUS_POINT_ACTIVATION_DATE;
				data.version = 1;
				data.insert(player);
			}
			
			rs.close();
			ps.close();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to load status points for char {}.", e, player.getName());
		}
		
		return data;
	}
	
	public void store(Player player)
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			PreparedStatement ps = con.prepareStatement(
				"UPDATE character_status_points SET " +
				"available=?, " +
				"attr_distributed=?, attr_str=?, attr_con=?, attr_dex=?, attr_int=?, attr_wit=?, attr_men=?, " +
				"status_distributed=?, status_pdef=?, status_mdef=?, status_hp=?, status_mp=?, status_cp=?, " +
				"status_patk=?, status_matk=?, status_accuracy=?, status_evasion=?, status_crit=?, " +
				"karma_penalty_attr=?, " +
				"source_level_points=?, source_quest_points=?, source_raid_points=?, source_siege_points=?, source_pvp_points=?, source_karma_points=?, " +
				"source_chest_points=?, " +
				"version=? WHERE char_id=? AND class_index=?");
			
			ps.setInt(1, available);
			
			ps.setInt(2, attrDistributed);
			ps.setInt(3, attrStr);
			ps.setInt(4, attrCon);
			ps.setInt(5, attrDex);
			ps.setInt(6, attrInt);
			ps.setInt(7, attrWit);
			ps.setInt(8, attrMen);
			
			ps.setInt(9, statusDistributed);
			ps.setInt(10, statusPdef);
			ps.setInt(11, statusMdef);
			ps.setInt(12, statusHp);
			ps.setInt(13, statusMp);
			ps.setInt(14, statusCp);
			ps.setInt(15, statusPatk);
			ps.setInt(16, statusMatk);
			ps.setInt(17, statusAccuracy);
			ps.setInt(18, statusEvasion);
			ps.setInt(19, statusCrit);
			
			ps.setInt(20, karmaPenaltyAttr);
			
			ps.setInt(21, sourceLevelPoints);
			ps.setInt(22, sourceQuestPoints);
			ps.setInt(23, sourceRaidPoints);
			ps.setInt(24, sourceSiegePoints);
			ps.setInt(25, sourcePvpPoints);
			ps.setInt(26, sourceKarmaPoints);
			
			ps.setInt(27, sourceChestPoints);
			
			ps.setInt(28, version);
			ps.setInt(29, player.getObjectId());
			ps.setInt(30, classIndex);
			
			ps.executeUpdate();
			ps.close();
		}
		catch (SQLException e)
		{
			// Migration 006 not applied yet - fall back to legacy update without source_chest_points
			storeLegacy(player, e);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to store status points for char {}.", e, player.getName());
		}
	}
	
	private void storeLegacy(Player player, SQLException cause)
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			PreparedStatement ps = con.prepareStatement(
				"UPDATE character_status_points SET " +
				"available=?, " +
				"attr_distributed=?, attr_str=?, attr_con=?, attr_dex=?, attr_int=?, attr_wit=?, attr_men=?, " +
				"status_distributed=?, status_pdef=?, status_mdef=?, status_hp=?, status_mp=?, status_cp=?, " +
				"status_patk=?, status_matk=?, status_accuracy=?, status_evasion=?, status_crit=?, " +
				"karma_penalty_attr=?, " +
				"source_level_points=?, source_quest_points=?, source_raid_points=?, source_siege_points=?, source_pvp_points=?, source_karma_points=?, " +
				"version=? WHERE char_id=? AND class_index=?");
			
			ps.setInt(1, available);
			ps.setInt(2, attrDistributed);
			ps.setInt(3, attrStr);
			ps.setInt(4, attrCon);
			ps.setInt(5, attrDex);
			ps.setInt(6, attrInt);
			ps.setInt(7, attrWit);
			ps.setInt(8, attrMen);
			ps.setInt(9, statusDistributed);
			ps.setInt(10, statusPdef);
			ps.setInt(11, statusMdef);
			ps.setInt(12, statusHp);
			ps.setInt(13, statusMp);
			ps.setInt(14, statusCp);
			ps.setInt(15, statusPatk);
			ps.setInt(16, statusMatk);
			ps.setInt(17, statusAccuracy);
			ps.setInt(18, statusEvasion);
			ps.setInt(19, statusCrit);
			ps.setInt(20, karmaPenaltyAttr);
			ps.setInt(21, sourceLevelPoints);
			ps.setInt(22, sourceQuestPoints);
			ps.setInt(23, sourceRaidPoints);
			ps.setInt(24, sourceSiegePoints);
			ps.setInt(25, sourcePvpPoints);
			ps.setInt(26, sourceKarmaPoints);
			ps.setInt(27, version);
			ps.setInt(28, player.getObjectId());
			ps.setInt(29, classIndex);
			
			ps.executeUpdate();
			ps.close();
			
			LOGGER.warn("Stored legacy (missing source_chest_points column). Run migration 006. Char {}.", player.getName(), cause);
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to store status points (legacy) for char {}.", e, player.getName());
		}
	}
	
	private void insert(Player player)
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			PreparedStatement ps = con.prepareStatement(
				"INSERT INTO character_status_points (char_id, class_index, version) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE char_id=char_id");
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, classIndex);
			ps.executeUpdate();
			ps.close();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to insert status points for char {}.", e, player.getName());
		}
	}
	
	public void migrateOldChar(Player player)
	{
		if (!isOldChar)
			return;
		
		isOldChar = false;
		version = 3;
		
		int level = player.getStatus().getLevel();
		available = level * StatusPointConfig.POINTS_PER_LEVEL;
		
		attrDistributed = 0;
		attrStr = 0;
		attrCon = 0;
		attrDex = 0;
		attrInt = 0;
		attrWit = 0;
		attrMen = 0;
		
		statusDistributed = 0;
		statusPdef = 0;
		statusMdef = 0;
		statusHp = 0;
		statusMp = 0;
		statusCp = 0;
		statusPatk = 0;
		statusMatk = 0;
		statusAccuracy = 0;
		statusEvasion = 0;
		statusCrit = 0;
	}
	
	public void delete(Player player)
	{
		try (Connection con = ConnectionPool.getConnection())
		{
			PreparedStatement ps = con.prepareStatement("DELETE FROM character_status_points WHERE char_id = ? AND class_index = ?");
			ps.setInt(1, player.getObjectId());
			ps.setInt(2, classIndex);
			ps.executeUpdate();
			ps.close();
		}
		catch (Exception e)
		{
			LOGGER.error("Failed to delete status points for char {}.", e, player.getName());
		}
	}
	
	public void computeEffectiveBases(Player player)
	{
		previewBaseStr = player.getTemplate().getBaseSTR();
		previewBaseCon = player.getTemplate().getBaseCON();
		previewBaseDex = player.getTemplate().getBaseDEX();
		previewBaseInt = player.getTemplate().getBaseINT();
		previewBaseWit = player.getTemplate().getBaseWIT();
		previewBaseMen = player.getTemplate().getBaseMEN();
		
		previewBasePdef = player.getStatus().getPDef(null) - statusPdef * StatusPointConfig.PDEF_PER_POINT;
		previewBaseMdef = player.getStatus().getMDef(null, null) - statusMdef;
		previewBaseHp = player.getStatus().getMaxHp() - statusHp;
		previewBaseMp = player.getStatus().getMaxMp() - statusMp;
		previewBaseCp = player.getStatus().getMaxCp() - statusCp;
		previewBasePatk = player.getStatus().getPAtk(null) - statusPatk;
		previewBaseMatk = player.getStatus().getMAtk(null, null) - statusMatk;
		previewBaseAccuracy = player.getStatus().getAccuracy() - statusAccuracy;
		previewBaseEvasion = player.getStatus().getEvasionRate(null) - statusEvasion;
		previewBaseCrit = player.getStatus().getCriticalHit(null, null) - statusCrit;
	}
}
