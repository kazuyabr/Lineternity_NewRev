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
package ext.mods.commons;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Identidade do projeto — carrega de brand.properties (game/config/).
 * Edite brand.properties para rebrandar o servidor.
 */
public final class BrProjectMeta
{
	private BrProjectMeta() {}

	// Defaults (used if brand.properties is missing or incomplete)
	private static final String DEF_TEAM = "LINETERNITY";
	private static final String DEF_BRAND = "Lineternity";
	private static final String DEF_DISTRIB = "PROIBIDO COMERCIALIZAR OU VENDER ESTE SERVIDOR, SEJA DE FORMA DIRETA OU INDIRETA.";
	private static final String DEF_BUILD = "BUILD 2026 | 3.8 | Lineternity";
	private static final String DEF_CORE = "DEVS: Dhousefe-L2JBR | Agazes33 | Ban-NEXORA | Warman | SrEli | < A.L.N/>";
	private static final String DEF_SIGN = "< A.L.N/>";

	// Loaded values (public for access from Team.java, GUI, etc.)
	public static final String TEAM;
	public static final String BRAND;
	public static final String DISTRIB_MODE;
	public static final String BUILD_LINE;
	public static final String CORE_LINE;
	public static final String SIGNATURE;

	static
	{
		Properties p = new Properties();
		String path = "game/config/brand.properties";
		try (FileInputStream fis = new FileInputStream(path))
		{
			p.load(fis);
		}
		catch (IOException e)
		{
			// File not found — use defaults silently
		}
		TEAM = p.getProperty("team", DEF_TEAM);
		BRAND = p.getProperty("brand", DEF_BRAND);
		DISTRIB_MODE = p.getProperty("distrib_mode", DEF_DISTRIB);
		BUILD_LINE = p.getProperty("build_line", DEF_BUILD);
		CORE_LINE = p.getProperty("core_line", DEF_CORE);
		SIGNATURE = p.getProperty("signature", DEF_SIGN);
	}
}
