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
 */
package ext.mods.gameserver.handler.bypasshandlers;

import ext.mods.commons.pool.ThreadPool;
import ext.mods.gameserver.enums.GaugeColor;
import ext.mods.gameserver.handler.IBypassHandler;
import ext.mods.gameserver.model.actor.Creature;
import ext.mods.gameserver.model.actor.Npc;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.location.Location;
import ext.mods.gameserver.network.serverpackets.ActionFailed;
import ext.mods.gameserver.network.serverpackets.MagicSkillUse;
import ext.mods.gameserver.network.serverpackets.SetupGauge;
import ext.mods.gameserver.data.manager.RandomPvpZoneManager;
import ext.mods.gameserver.data.manager.RandomPvpZoneManager.PvPZoneData;

/**
 * Bypass handler for the PvP teleport button on the Global Gatekeeper (NPC 50010).
 * Teleports the player to the current active PvP zone managed by RandomPvpZoneManager.
 */
public class PvPTeleport implements IBypassHandler
{
	private static final String[] COMMANDS = { "pvp" };
	private static final int SOE_VISUAL_SKILL_ID = 2036;
	private static final int SOE_VISUAL_SKILL_LEVEL = 1;
	private static final int CAST_TIME_MS = 5000;
	
	@Override
	public boolean useBypass(String command, Player player, Creature target)
	{
		if (!(target instanceof Npc))
			return false;
		
		if (player.isDead())
		{
			player.sendMessage("You cannot teleport while dead.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		if (player.isInOlympiadMode())
		{
			player.sendMessage("You cannot teleport during the Olympiad.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		if (player.isInCombat())
		{
			player.sendMessage("You cannot teleport while in combat.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		if (player.getCast().isCastingNow() || player.isTeleporting())
		{
			player.sendMessage("Please wait for your current action to finish.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		PvPZoneData zone = RandomPvpZoneManager.getInstance().getCurrentZone();
		if (zone == null)
		{
			player.sendMessage("No PvP zone is currently active.");
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		final Location loc = zone.getLocation();
		
		player.broadcastPacket(new MagicSkillUse(player, player, SOE_VISUAL_SKILL_ID, SOE_VISUAL_SKILL_LEVEL, CAST_TIME_MS, 0));
		player.sendPacket(new SetupGauge(GaugeColor.BLUE, CAST_TIME_MS));
		
		ThreadPool.schedule(() ->
		{
			if (player.isDead() || player.isTeleporting())
				return;
			player.teleToLocation(loc);
		}, CAST_TIME_MS);
		
		return true;
	}
	
	@Override
	public String[] getBypassList()
	{
		return COMMANDS;
	}
}
