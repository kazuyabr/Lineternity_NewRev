/*
* Copyleft © 2024-2026 L2Brproject
* * This file is part of L2Brproject derived from aCis409/RusaCis3.8
* * L2Brproject is free software: you can redistribute it and/or modify it
* under the terms of the GNU General Public License as published by the
* Free Software Foundation, either version 3 of the License.
* * L2Brproject is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* General Public License for more details.
* * You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
* Our main Developers, Dhousefe-L2JBR, Agazes33, Ban-L2jDev, Warman, SrEli.
* Our special thanks, Nattan Felipe, Diego Fonseca, Junin, ColdPlay, Denky, MecBew, Localhost, MundvayneHELLBOY, 
* SonecaL2, Eduardo.SilvaL2J, biLL, xpower, xTech, kakuzo, Tiagorosendo, Schuster, LucasStark, damedd
* as a contribution for the forum L2JBrasil.com
*
* Load screen / Start silencioso — desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
 */
package ext.mods.security;

import java.awt.GraphicsEnvironment;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import javax.swing.SwingUtilities;

import ext.mods.commons.ConsoleWindow;
import ext.mods.commons.logging.CLogger;
import ext.mods.security.gui.BootSplash;
import ext.mods.security.gui.MainFrame;

public class LicenseInit
{
	public static final CLogger LOGGER = new CLogger(LicenseInit.class.getName());
	private static boolean _valid;

	public static void main(String[] args)
	{
		System.setProperty("sun.java2d.opengl", "false");
		System.setProperty("sun.java2d.d3d", "false");
		System.setProperty("sun.java2d.pmoffscreen", "false");
		System.setProperty("brproject.quiet.console", "true");

		ConsoleWindow.hide();
		muteConsoleStreams();

		if (GraphicsEnvironment.isHeadless())
			return;

		SwingUtilities.invokeLater(() -> {
			final BootSplash splash = new BootSplash();
			splash.openAndWait(() -> {
				try
				{
					new MainFrame();
				}
				finally
				{
					splash.closeSplash();
					ConsoleWindow.hide();
				}
			});
		});
	}

	private static void muteConsoleStreams()
	{
		try
		{
			OutputStream nullOut = OutputStream.nullOutputStream();
			System.setOut(new PrintStream(nullOut, true, StandardCharsets.UTF_8));
			System.setErr(new PrintStream(nullOut, true, StandardCharsets.UTF_8));
		}
		catch (Throwable ignored) {}
	}

	public static boolean getIsValid()
	{
		return _valid;
	}

	public static void setisValid(boolean val)
	{
		_valid = val;
	}
}
