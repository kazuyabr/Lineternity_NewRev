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
package ext.mods.gameserver.quest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import ext.mods.commons.config.ExProperties;
import ext.mods.gameserver.CharacterStatusPoints;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.scripting.Quest;
import ext.mods.gameserver.scripting.QuestState;
import ext.mods.gameserver.StatusPointConfig;
import ext.mods.commons.logging.CLogger;

public class QuestRewardConfig
{
	private static final CLogger LOGGER = new CLogger(QuestRewardConfig.class.getName());
	private static final Path QUEST_CONFIG_DIR = Paths.get("game/config/quest");
	
	public static void applyQuestRewards(Player player, Quest quest, QuestState st)
	{
		if (!StatusPointConfig.STATUS_POINTS_ENABLED)
			return;
		
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		String questName = quest.getName();
		Path questFile = findQuestConfig(questName);
		
		if (questFile == null || !Files.exists(questFile))
			return;
		
		ExProperties props = new ExProperties();
		try
		{
			props.load(questFile.toFile());
		}
		catch (Exception e)
		{
			LOGGER.error("Could not load quest config: {}", e, questFile);
			return;
		}
		
		boolean repeatable = props.getProperty("Repeatable", false);
		
		if (!repeatable)
		{
			String rewardKey = "quest_rewarded." + questName;
			if (player.getMemos().containsKey(rewardKey))
				return;
		}
		
		int spReward = props.getProperty("StatusPointReward", 0);
		if (spReward > 0 && !repeatable)
		{
			data.available += spReward;
			data.sourceQuestPoints += spReward;
		}
		
		int repeatableSpReward = props.getProperty("RepeatableStatusPointReward", 0);
		if (repeatableSpReward > 0 && repeatable)
		{
			data.available += repeatableSpReward;
			data.sourceQuestPoints += repeatableSpReward;
		}
		
		int pdefReward = props.getProperty("PDefReward", 0);
		if (pdefReward > 0)
		{
			data.statusPdef += pdefReward;
			data.statusDistributed += pdefReward;
		}
		
		int rewardXP = props.getProperty("RewardXP", 0);
		int rewardSP = props.getProperty("RewardSP", 0);
		if (rewardXP > 0 || rewardSP > 0)
			Quest.rewardExpAndSp(player, rewardXP, rewardSP);
		
		for (int i = 1; i <= 5; i++)
		{
			int itemId = props.getProperty("RewardItem" + i + "_Id", 0);
			int count = props.getProperty("RewardItem" + i + "_Count", 0);
			if (itemId > 0 && count > 0)
				Quest.giveItems(player, itemId, count);
		}
		
		if (!repeatable)
			player.getMemos().set("quest_rewarded." + questName, true);
		
		Player.applyDirectStatusFuncs(player, data);
		data.store(player);
		player.broadcastUserInfo();
	}
	
	private static Path findQuestConfig(String questName)
	{
		try
		{
			return Files.walk(QUEST_CONFIG_DIR)
				.filter(f -> f.getFileName().toString().equals(questName + ".properties"))
				.findFirst()
				.orElse(null);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public static void applyPDefBonus(Player player)
	{
		CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null)
			return;
		
		if (data.statusPdef > 0)
			Player.applyDirectStatusFuncs(player, data);
	}
}
