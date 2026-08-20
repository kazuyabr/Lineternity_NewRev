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
package ext.mods.gameserver.skills.funcs;

import ext.mods.gameserver.enums.skills.Stats;
import ext.mods.gameserver.model.actor.Creature;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.skills.L2Skill;
import ext.mods.gameserver.skills.basefuncs.Func;
import ext.mods.gameserver.StatusPointOwner;

public class FuncStatusPoint extends Func
{
	private final boolean _replaceBase;
	
	public FuncStatusPoint(Player owner, Stats stat, int value)
	{
		this(owner, stat, value, StatusPointOwner.DISTRIBUTED, false);
	}
	
	public FuncStatusPoint(Player owner, Stats stat, int value, Object ownerMarker)
	{
		this(owner, stat, value, ownerMarker, false);
	}
	
	public FuncStatusPoint(Player owner, Stats stat, int value, boolean replaceBase)
	{
		this(owner, stat, value, StatusPointOwner.DISTRIBUTED, replaceBase);
	}
	
	public FuncStatusPoint(Player owner, Stats stat, int value, Object ownerMarker, boolean replaceBase)
	{
		super(ownerMarker, stat, 2, value, null);
		_replaceBase = replaceBase;
	}
	
	private static int getBaseDefault(Stats stat, Player player)
	{
		switch (stat)
		{
			case STAT_STR:
				return player.getTemplate().getBaseSTR();
			case STAT_CON:
				return player.getTemplate().getBaseCON();
			case STAT_DEX:
				return player.getTemplate().getBaseDEX();
			case STAT_INT:
				return player.getTemplate().getBaseINT();
			case STAT_WIT:
				return player.getTemplate().getBaseWIT();
			case STAT_MEN:
				return player.getTemplate().getBaseMEN();
			default:
				return 0;
		}
	}
	
	@Override
	public double calc(Creature effector, Creature effected, L2Skill skill, double base, double value)
	{
		if (effector instanceof Player player)
		{
			if (_replaceBase)
				return value + getValue() - getBaseDefault(getStat(), player);
			return value + getValue();
		}
		return value;
	}
}
