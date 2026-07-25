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
package ext.mods.gameserver.network.serverpackets;

import ext.mods.gameserver.data.manager.PcCafeManager;
import ext.mods.gameserver.enums.PcCafeConsumeType;

public class ExPCCafePointInfo extends L2GameServerPacket
{
	private final int _score;
	private final int _modify;
	private final int _remainingTime;
	private PcCafeConsumeType _pointType;
	private final int _periodType;
	
	public ExPCCafePointInfo(int score, int modify, PcCafeConsumeType pointType)
	{
		_score = score;
		_modify = modify;
		_remainingTime = 0;
		_pointType = pointType;
		_periodType = PcCafeManager.getInstance().enableEvent();
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xFE);
		writeH(0x31);
		writeD(_score);
		writeD(_modify);
		writeC(_periodType);
		writeD(_remainingTime);
		writeC(_pointType.ordinal());
	}
}