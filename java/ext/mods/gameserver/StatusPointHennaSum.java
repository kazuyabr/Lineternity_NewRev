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

import java.util.Arrays;
import java.util.stream.Stream;

import ext.mods.gameserver.data.xml.HennaData;
import ext.mods.gameserver.enums.actors.ClassId;
import ext.mods.gameserver.model.records.Henna;
import ext.mods.commons.util.ArraysUtil;
import ext.mods.commons.logging.CLogger;

public class StatusPointHennaSum
{
	private static final CLogger LOGGER = new CLogger(StatusPointHennaSum.class.getName());
	
	public static int calculate(ClassId classId)
	{
		if (classId == null || classId == ClassId.NONE)
			return 0;
		
		int sum = 0;
		for (Henna henna : HennaData.getInstance().getHennas())
		{
			if (!ArraysUtil.contains(henna.classes(), classId.getId()))
				continue;
			
			sum += Stream.of(henna.INT(), henna.STR(), henna.CON(), henna.MEN(), henna.DEX(), henna.WIT())
				.mapToInt(Integer::intValue)
				.sum();
		}
		
		return Math.max(sum, 0);
	}
}
