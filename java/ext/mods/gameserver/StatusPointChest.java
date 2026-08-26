/*
* Copyleft © 2024-2026 L2Lineternity
* * This file is part of L2Lineternity derived from aCis409/RusaCis3.8
* * L2Lineternity is free software: you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by the
* Free Software Foundation, either version 3 of the License.
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

import java.util.LinkedHashMap;
import java.util.Map;

import ext.mods.commons.random.Rnd;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.skills.Formulas;
import ext.mods.gameserver.skills.L2Skill;

/**
 * Rewards players for successfully opening treasure chests (boxes).<br>
 * <br>
 * While under the daily limit, each opened chest grants status points scaled by the chest level,
 * a random bonus roll and the actual unlock chance (best keys yield full value).<br>
 * After the daily limit is exhausted, chests instead drop every currency pool item
 * (union of CostCapItems and ResetCostItems) with low random amounts, without daily limit.
 */
public final class StatusPointChest
{
	private static final String MEMO_DAY = "sp_chest_day";
	private static final String MEMO_COUNT = "sp_chest_count";
	
	private StatusPointChest()
	{
	}
	
	public static void onChestOpened(Player player, L2Skill skill, int chestLevel)
	{
		if (!StatusPointConfig.STATUS_POINTS_ENABLED || !StatusPointConfig.CHEST_REWARD_ENABLED)
			return;
		
		final CharacterStatusPoints data = player.getStatusPointsData();
		if (data == null || data.isOldChar)
			return;
		
		final int today = getTodayStamp();
		int count = player.getMemos().getInteger(MEMO_COUNT, 0);
		if (player.getMemos().getInteger(MEMO_DAY, -1) != today)
		{
			count = 0;
			player.getMemos().set(MEMO_DAY, today);
		}
		
		final int chance = Formulas.getChestUnlockChance(skill, chestLevel);
		
		if (count < StatusPointConfig.CHEST_DAILY_REWARDS)
		{
			final int baseReward = chestLevel + Rnd.get(StatusPointConfig.CHEST_REWARD_BONUS_ROLL + 1);
			final int points = Math.max(1, (int) Math.round(baseReward * chance / 100.0));
			
			data.available += points;
			data.sourceChestPoints += points;
			data.store(player);
			
			count++;
			player.getMemos().set(MEMO_COUNT, count);
			
			player.sendMessage("Chest reward: +" + points + " points.");
			player.sendMessage("Next chest: " + chance + "% chance (" + count + "/" + StatusPointConfig.CHEST_DAILY_REWARDS + " today).");
			return;
		}
		
		dropCurrencyPool(player);
	}
	
	/**
	 * Drops every configured cap/reset currency item at once with a random amount of 1-10 each.
	 */
	private static void dropCurrencyPool(Player player)
	{
		final Map<Integer, Integer> pool = new LinkedHashMap<>();
		pool.putAll(StatusPointConfig.COST_CAP_ITEMS);
		StatusPointConfig.RESET_COST_ITEMS.forEach(pool::putIfAbsent);
		
		if (pool.isEmpty())
			return;
		
		final StringBuilder sb = new StringBuilder();
		for (Map.Entry<Integer, Integer> entry : pool.entrySet())
		{
			final int amount = Rnd.get(1, 10);
			player.addItem(entry.getKey(), amount, true);
			
			if (sb.length() > 0)
				sb.append(", ");
			sb.append(amount).append("x ").append(itemName(entry.getKey()));
		}
		
		player.sendMessage("Daily point limit reached. Chest loot: " + sb.toString());
	}
	
	private static String itemName(int itemId)
	{
		final var item = ext.mods.gameserver.data.xml.ItemData.getInstance().getTemplate(itemId);
		return (item != null) ? item.getName() : "Item#" + itemId;
	}
	
	/**
	 * @return a day-stamp that changes once per real day (UTC days since epoch).
	 */
	private static int getTodayStamp()
	{
		return (int) (System.currentTimeMillis() / 86400000L);
	}
}
