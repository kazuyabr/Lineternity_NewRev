/*
* Copyleft © 2024-2026 L2Brproject
* Detecta processos Java rodando (zumbis/antigos) que podem impedir
* a inicializacao correta de novos LoginServer/GameServer.
*/
package ext.mods.commons.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Detecta processos Java em execucao filtrando por classpath/main class do BR Project.
 *
 * <p>Plataforma-alvo: Windows (usa tasklist + wmic). Em outras plataformas,
 * lanca {@link UnsupportedOperationException} - comportamento intencional,
 * projeto suporta apenas Windows oficialmente.</p>
 */
public final class JavaProcessInspector {

    private static final String BR_PROJECT_LOGIN_MAIN = "ext.mods.loginserver.LoginServer";
    private static final String BR_PROJECT_GAME_MAIN = "ext.mods.gameserver.GameServer";

    private JavaProcessInspector() {}

    /** Tipo de servidor detectado num processo java. */
    public enum ServerType {
        LOGIN_SERVER("LoginServer"),
        GAME_SERVER("GameServer"),
        UNKNOWN("Java (BR Project)");

        private final String label;
        ServerType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /** Informacao de um processo java do BR Project. */
    public static final class JavaProcessInfo {
        public final int pid;
        public final String commandLine;
        public final ServerType type;

        JavaProcessInfo(int pid, String commandLine, ServerType type) {
            this.pid = pid;
            this.commandLine = commandLine;
            this.type = type;
        }

        public boolean isBrProject() { return type != ServerType.UNKNOWN || commandLine.contains("Brproject_Distribution"); }
    }

    /**
     * Lista todos os processos java que parecem ser do BR Project
     * (LoginServer, GameServer, ou classpath contem Brproject_Distribution).
     */
    public static List<JavaProcessInfo> findBrProjectProcesses() {
        List<JavaProcessInfo> result = new ArrayList<>();
        if (!isWindows()) return result;

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "wmic", "process", "where",
                "name='java.exe'",
                "get", "ProcessId,CommandLine", "/FORMAT:CSV"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean skipHeader = true;
                while ((line = r.readLine()) != null) {
                    if (skipHeader) { skipHeader = false; continue; }
                    if (line.isBlank()) continue;
                    JavaProcessInfo info = parseWmicLine(line);
                    if (info != null && info.isBrProject()) result.add(info);
                }
            }
            p.waitFor();
        } catch (IOException | InterruptedException ignored) {
            // fallback silencioso: usuario sem wmic ou erro transitorio
        }
        return result;
    }

    private static JavaProcessInfo parseWmicLine(String csvLine) {
        // formato CSV do wmic: Node,CommandLine,ProcessId
        // CommandLine pode conter virgulas e aspas - parser simples tolerante
        int lastQuoteEnd = csvLine.lastIndexOf("\",");
        if (lastQuoteEnd < 0) return null;
        int pidStart = lastQuoteEnd + 2;
        if (pidStart >= csvLine.length()) return null;
        int pid;
        try { pid = Integer.parseInt(csvLine.substring(pidStart).trim()); }
        catch (NumberFormatException e) { return null; }

        int firstQuote = csvLine.indexOf('"');
        int secondQuote = csvLine.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) return null;
        String cmd = csvLine.substring(firstQuote + 1, secondQuote);

        ServerType type = ServerType.UNKNOWN;
        if (cmd.contains(BR_PROJECT_LOGIN_MAIN)) type = ServerType.LOGIN_SERVER;
        else if (cmd.contains(BR_PROJECT_GAME_MAIN)) type = ServerType.GAME_SERVER;

        return new JavaProcessInfo(pid, cmd, type);
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("win");
    }
}
