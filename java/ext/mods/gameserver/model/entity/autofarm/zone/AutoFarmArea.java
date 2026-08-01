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
package ext.mods.gameserver.model.entity.autofarm.zone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ext.mods.gameserver.model.entity.autofarm.AutoFarmManager.AutoFarmType;
import ext.mods.gameserver.model.entity.autofarm.AutoFarmManager;
import ext.mods.gameserver.model.entity.autofarm.AutoFarmProfile;
import ext.mods.gameserver.model.entity.autofarm.zone.form.ZoneNPolyZ;
import ext.mods.gameserver.idfactory.IdFactory;
import ext.mods.gameserver.model.actor.Creature;
import ext.mods.gameserver.model.actor.Player;
import ext.mods.gameserver.model.actor.instance.Monster;
import ext.mods.gameserver.model.location.Location;
import ext.mods.gameserver.model.zone.type.subtype.ZoneType;

public class AutoFarmArea extends ZoneType
{
	protected final Set<String> _monsterHistory = new HashSet<>();
	private final String _name;
	private final int _ownerId;
	private final AutoFarmType _type;
	private final List<Location> _nodes = new ArrayList<>();
	private boolean _isFromDb;
	private boolean _isChanged;
	
	public AutoFarmArea(String name, int ownerId, AutoFarmType type)
	{
		super(IdFactory.getInstance().getNextId());
		
		_name = name;
		_ownerId = ownerId;
		_type = type;
	}
	
	public AutoFarmArea(int id, String name, int ownerId, AutoFarmType type)
	{
		super(id);
		
		_name = name;
		_ownerId = ownerId;
		_type = type;
		_isFromDb = true;
	}
	
	@Override
	public void onEnter(Creature character)
	{
	}

	@Override
	public void onExit(Creature character)
	{
	}
	
	public ZoneNPolyZ getZoneZ()
	{
		return (ZoneNPolyZ) getZone();
	}
	
	public List<Monster> getMonsters()
	{
		return null;
	}
	
	public Set<String> getMonsterHistory()
	{
		return _monsterHistory;
	}
	
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	public AutoFarmType getType()
	{
		return _type;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public List<Location> getNodes()
	{
		return _nodes;
	}
	
	public AutoFarmZone getFarmZone()
	{
		return null;
	}
	
	public AutoFarmRoute getRouteZone()
	{
		return null;
	}
	
	public boolean isFromDb()
	{
		return _isFromDb;
	}
	
	public void setIsFromDb()
	{
		_isFromDb = true;
	}
	
	/*
	 * Check if any node has been added or removed from this area.
	 */
	public boolean isChanged()
	{
		return _isChanged;
	}
	
	public void setIsChanged(boolean status)
	{
		_isChanged = status;
	}
	
	public boolean isMovementAllowed()
	{
		switch (_type)
		{
			case OPEN:
				return getProfile().getFinalRadius() > getProfile().getAttackRange();
				
			case ROTA:
				return false;
				
			default:
				return true;
		}
	}
	
	public AutoFarmProfile getProfile()
	{
		return AutoFarmManager.getInstance().getPlayer(_ownerId);
	}
	
	public Player getOwner()
	{
		return getProfile().getPlayer();
	}

	public boolean isOwnerNearOrInside(int proximityRadius)
	{
		final Player player = getOwner();
		final int ax1 = player.getX() - proximityRadius;
		final int ax2 = player.getX() + proximityRadius;
		final int ay1 = player.getY() - proximityRadius;
		final int ay2 = player.getY() + proximityRadius;
		final int az1 = player.getZ() - proximityRadius;
		final int az2 = player.getZ() + proximityRadius;
	    return getZoneZ().intersectsRectangle(ax1, ax2, ay1, ay2, az1, az2);
	}
	
	public boolean isOwnerNearEdge(int proximityRadius)
	{
		final Player player = getOwner();
		final int ax1 = player.getX() - proximityRadius;
		final int ax2 = player.getX() + proximityRadius;
		final int ay1 = player.getY() - proximityRadius;
		final int ay2 = player.getY() + proximityRadius;
	    return getZoneZ().intersectsRectangleOnEdge(ax1, ax2, ay1, ay2);
	}
}