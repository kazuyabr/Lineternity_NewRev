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

public final class Team
{
	private static final String RESET = "\u001B[0m";
	private static final String BOLD = "\u001B[1m";
	private static final String BRIGHT_CYAN = "\u001B[96m";
	private static final String BRIGHT_YELLOW = "\u001B[93m";

	private static final int LOGO_WIDTH = 117;

	private static final String YELLOW_PREFIX = "::::::::::::::::::::::::::::::::::: ";

	
	private static final String BRAND_LINE =
		"::::::::::::::::::::::::::::::::::::::::::::::::::::::[ L2JBr ]::::::::::::::::::::::::::::::::::::::::::::::::::::::";

	private static final String[] LOGO = {
		":::::::::::::::::##:::::::::#######:::::::::##::::::::##############:::::::##########::::::::::::::::::::::::::::::::",
		":::::::::::::::::##::::::::##:::::##::::::::##::::::::##::::::::::###::::::##::::::###:::::::::::::::::::::::::::::::",
		":::::::::::::::::##:::::::::::::::##::::::::##::::::::##::::::::::###::::::##::::::###:::::::::::::::::::::::::::::::",
		":::::::::::::::::##:::::::::#######:::::::::##::::::::##############:::::::##########::::::::::::::::::::::::::::::::",
		":::::::::::::::::##::::::::##:::::::::::::::##::::::::###############::::::###########:::::::::::::::::::::::::::::::",
		":::::::::::::::::##::::::::##:::::::::::::::##::::::::##:::::::::::##::::::##::::::::##::::::::::::::::::::::::::::::",
		":::::::::::::::::########::#########:::######:::::::::##:::::::::::##::::::##::::::::##::::::::::::::::::::::::::::::",
		"::::::::::::::::::::::::::::::::::::::::::::::::::::::##############:::::::##::::::::##::::::::::::::::::::::::::::::",
	};

	private Team() {}

	public static void info()
	{
		infoGameServer();
	}

	public static void infoLoginServer()
	{
		printBanner("LOGIN SERVER");
	}

	public static void infoGameServer()
	{
		printBanner("GAME SERVER");
	}

	private static void printBanner(String serverName)
	{
		printHeaderLine(BrProjectMeta.TEAM, true);
		
		printHeaderLine(serverName, false);
		
		for (String line : LOGO)
			printAsciiLine(trimLogoLine(line));
			
		System.out.println(BRIGHT_CYAN + BRAND_LINE + RESET);
		
		printYellowDotted(BrProjectMeta.DISTRIB_MODE);
		printYellowDotted(BrProjectMeta.BUILD_LINE);
		printYellowDotted(BrProjectMeta.CORE_LINE);
		
		System.out.println("");
		
		printHeaderLine(BrProjectMeta.BRAND, true);
		System.out.flush();
	}

	/**
	 * Calcula e imprime barras horizontais dinamicamente baseadas no LOGO_WIDTH.
	 * * @param text O texto a ser emoldurado "=[ text ]"
	 * @param rightAligned Se verdadeiro, alinha o texto na direita. Falso centraliza.
	 */
	private static void printHeaderLine(String text, boolean rightAligned)
	{
		if (text == null) text = "Unknown";
		
		String label = "=[ " + text + " ]";
		int dashCount = LOGO_WIDTH - label.length();
		if (dashCount < 0) dashCount = 0;

		StringBuilder sb = new StringBuilder();
		sb.append(BRIGHT_CYAN);

		if (rightAligned)
		{
			for (int i = 0; i < dashCount; i++) sb.append("-");
			sb.append("=[ ").append(BRIGHT_YELLOW).append(text).append(BRIGHT_CYAN).append(" ]");
		}
		else
		{
			int leftDashes = dashCount / 2;
			int rightDashes = dashCount - leftDashes;
			
			for (int i = 0; i < leftDashes; i++) sb.append("-");
			sb.append("=[ ").append(BRIGHT_YELLOW).append(text).append(BRIGHT_CYAN).append(" ]");
			for (int i = 0; i < rightDashes; i++) sb.append("-");
		}

		sb.append(RESET);
		System.out.println(sb.toString());
	}

	private static String trimLogoLine(String line)
	{
		if (line.length() <= LOGO_WIDTH)
			return line;
		return line.substring(0, LOGO_WIDTH);
	}

	private static void printYellowDotted(String text)
	{
		if (text != null && !text.trim().isEmpty()) {
			System.out.println(BRIGHT_CYAN + YELLOW_PREFIX.replace(" ", "") + " " + BRIGHT_YELLOW + text + RESET);
		}
	}

	private static void printAsciiLine(String line)
	{
		String colorized = line.replace(":", BRIGHT_CYAN + ":" + RESET)
		                       .replace("#", BRIGHT_YELLOW + BOLD + "#" + RESET);
		System.out.println(colorized);
	}
}