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
package ext.mods.gameserver.data.manager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import ext.mods.commons.data.StatSet;
import ext.mods.commons.data.xml.IXmlReader;
import ext.mods.commons.pool.ThreadPool;
import ext.mods.commons.random.Rnd;
import ext.mods.gameserver.model.location.Location;
import ext.mods.gameserver.model.World;
import ext.mods.gameserver.network.serverpackets.CreatureSay;
import ext.mods.gameserver.enums.SayType;
import ext.mods.Config;

import org.w3c.dom.Document;

/**
 * Manages Random PvP Zones for the Global Gatekeeper (NPC 50010).
 * Cycles through configured PvP zones on a timer, announcing the current zone.
 */
public class RandomPvpZoneManager implements IXmlReader
{
	private static RandomPvpZoneManager _instance;
	
	private final List<PvPZoneData> _zones = new ArrayList<>();
	private PvPZoneData _currentZone;
	private ScheduledFuture<?> _rotationTask;
	
	public static RandomPvpZoneManager getInstance()
	{
		if (_instance == null)
		{
			_instance = new RandomPvpZoneManager();
		}
		return _instance;
	}
	
	public void init()
	{
		if (!Config.RANDOM_PVP_ZONE)
		{
			LOGGER.info("RandomPvpZoneManager: Disabled by configuration.");
			return;
		}
		
		load();
		
		if (_zones.isEmpty())
		{
			LOGGER.warn("RandomPvpZoneManager: No zones defined in randomPvpZones.xml!");
			return;
		}
		
		_currentZone = _zones.get(Rnd.get(_zones.size()));
		
		int interval = Config.RANDOM_PVP_ZONE_INTERVAL;
		_rotationTask = ThreadPool.scheduleAtFixedRate(this::rotateZone, interval * 60 * 1000L, interval * 60 * 1000L);
		
		LOGGER.info("RandomPvpZoneManager: Initialized with " + _zones.size() + " zones, rotating every " + interval + " minutes. Current: " + _currentZone.getName());
	}
	
	@Override
	public void load()
	{
		_zones.clear();
		parseDataFile("xml/randomPvpZones.xml");
		LOGGER.info("Loaded " + _zones.size() + " RandomPvpZone templates.");
	}
	
	@Override
	public void parseDocument(Document doc, Path path)
	{
		forEach(doc, "list", listNode ->
		{
			forEach(listNode, "zone", zoneNode ->
			{
				final StatSet set = parseAttributes(zoneNode);
				
				String name = set.getString("name");
				int x = set.getInteger("x");
				int y = set.getInteger("y");
				int z = set.getInteger("z");
				
				_zones.add(new PvPZoneData(name, new Location(x, y, z)));
			});
		});
	}
	
	private void rotateZone()
	{
		if (_zones.isEmpty())
			return;
		
		PvPZoneData newZone;
		do
		{
			newZone = _zones.get(Rnd.get(_zones.size()));
		}
		while (newZone == _currentZone && _zones.size() > 1);
		
		_currentZone = newZone;
		
		for (ext.mods.gameserver.model.actor.Player player : World.getInstance().getPlayers())
		{
			if (player.isOnline())
			{
				player.sendPacket(new CreatureSay(SayType.CRITICAL_ANNOUNCE, "PvP Zone", "PvP zone changed to: " + _currentZone.getName() + "!"));
			}
		}
	}
	
	public PvPZoneData getCurrentZone()
	{
		return _currentZone;
	}
	
	public String getCurrentZoneName()
	{
		if (_currentZone == null)
			return "None";
		return _currentZone.getName();
	}
	
	public String getCurrentZoneTimeLeft()
	{
		if (_rotationTask == null || _rotationTask.isCancelled())
			return "N/A";
		
		long delay = _rotationTask.getDelay(java.util.concurrent.TimeUnit.MILLISECONDS);
		if (delay <= 0)
			return "rotating...";
		
		long minutes = delay / 60000;
		long seconds = (delay % 60000) / 1000;
		return String.format("%02d:%02d", minutes, seconds);
	}
	
	public void shutdown()
	{
		if (_rotationTask != null)
		{
			_rotationTask.cancel(false);
			_rotationTask = null;
		}
	}
	
	public static class PvPZoneData
	{
		private final String _name;
		private final Location _location;
		
		public PvPZoneData(String name, Location location)
		{
			_name = name;
			_location = location;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public Location getLocation()
		{
			return _location;
		}
	}
}
