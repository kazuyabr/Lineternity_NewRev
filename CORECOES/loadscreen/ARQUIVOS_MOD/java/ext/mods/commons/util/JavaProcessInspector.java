/*
* Copyleft © 2024-2026 L2Brproject
* Detecta / encerra processos Java (java.exe e javaw.exe) do BR Project.
*
* Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
*/
package ext.mods.commons.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Detecta processos Java em execucao filtrando por classpath/main class do BR Project.
 * Usa {@link ProcessHandle} (funciona com java.exe e javaw.exe).
 * Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
 */
public final class JavaProcessInspector {

    private static final String BR_PROJECT_LOGIN_MAIN = "ext.mods.loginserver.LoginServer";
    private static final String BR_PROJECT_GAME_MAIN = "ext.mods.gameserver.GameServer";
    private static final String BR_PROJECT_LAUNCHER_MAIN = "ext.mods.security.LicenseInit";

    private JavaProcessInspector() {}

    public enum ServerType {
        LOGIN_SERVER("LoginServer"),
        GAME_SERVER("GameServer"),
        LAUNCHER("Launcher"),
        UNKNOWN("Java (BR Project)");

        private final String label;
        ServerType(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    public static final class JavaProcessInfo {
        public final int pid;
        public final String commandLine;
        public final ServerType type;

        JavaProcessInfo(int pid, String commandLine, ServerType type) {
            this.pid = pid;
            this.commandLine = commandLine;
            this.type = type;
        }

        public boolean isBrProject() {
            return type != ServerType.UNKNOWN
                || (commandLine != null && isBrProjectCommand(commandLine));
        }
    }

    public static List<JavaProcessInfo> findBrProjectProcesses() {
        List<JavaProcessInfo> result = new ArrayList<>();
        long self = ProcessHandle.current().pid();

        ProcessHandle.allProcesses().forEach(ph -> {
            try {
                long pid = ph.pid();
                if (pid == self || !ph.isAlive())
                    return;

                Optional<String> cmdOpt = ph.info().command();
                if (cmdOpt.isEmpty())
                    return;

                String exe = cmdOpt.get().toLowerCase(Locale.ROOT);
                if (!exe.endsWith("java.exe") && !exe.endsWith("javaw.exe")
                    && !exe.endsWith("/java") && !exe.endsWith("/javaw")
                    && !exe.equals("java") && !exe.equals("javaw"))
                    return;

                String cmdLine = ph.info().commandLine().orElse("");
                if (cmdLine.isBlank())
                {
                    StringBuilder sb = new StringBuilder(exe);
                    ph.info().arguments().ifPresent(args -> {
                        for (String a : args)
                            sb.append(' ').append(a);
                    });
                    cmdLine = sb.toString();
                }

                // Somente pela linha de comando (main/classpath), nunca so pelo path do JDK
                if (!isBrProjectCommand(cmdLine))
                    return;

                result.add(new JavaProcessInfo((int) pid, cmdLine, detectType(cmdLine)));
            }
            catch (Exception ignored) {}
        });

        return result;
    }

    private static boolean isBrProjectCommand(String cmd) {
        if (cmd == null || cmd.isBlank())
            return false;
        // Match por classe principal / artefato do servidor — evita matar Gradle/Cursor
        return cmd.contains(BR_PROJECT_LOGIN_MAIN)
            || cmd.contains(BR_PROJECT_GAME_MAIN)
            || cmd.contains(BR_PROJECT_LAUNCHER_MAIN)
            || cmd.contains("ext.mods.security.gui.MainFrame")
            || (cmd.toLowerCase(Locale.ROOT).contains("brproject_distribution")
                && (cmd.contains("server.jar") || cmd.contains("libs")));
    }

    private static ServerType detectType(String cmd) {
        if (cmd.contains(BR_PROJECT_LOGIN_MAIN)) return ServerType.LOGIN_SERVER;
        if (cmd.contains(BR_PROJECT_GAME_MAIN)) return ServerType.GAME_SERVER;
        if (cmd.contains(BR_PROJECT_LAUNCHER_MAIN)) return ServerType.LAUNCHER;
        return ServerType.UNKNOWN;
    }

    /**
     * Encerra JVMs do BR Project (java/javaw), exceto o processo atual.
     * O caller deve chamar {@code System.exit}/{@code halt} em seguida.
     */
    public static int killBrProjectProcesses() {
        int killed = 0;
        long self = ProcessHandle.current().pid();

        for (JavaProcessInfo info : findBrProjectProcesses()) {
            if (info.pid == self)
                continue;
            if (killPid(info.pid))
                killed++;
        }

        // Segunda passagem: qualquer filho direto deste launcher
        ProcessHandle.current().descendants().forEach(child -> {
            try {
                if (child.isAlive()) {
                    child.destroy();
                    Thread.sleep(200);
                    if (child.isAlive())
                        child.destroyForcibly();
                }
            }
            catch (Exception ignored) {}
        });

        return killed;
    }

    private static boolean killPid(int pid) {
        try {
            Optional<ProcessHandle> ph = ProcessHandle.of(pid);
            if (ph.isPresent() && ph.get().isAlive()) {
                ProcessHandle handle = ph.get();
                handle.destroy();
                Thread.sleep(250);
                if (handle.isAlive())
                    handle.destroyForcibly();
            }

            // Garante no Windows
            if (isWindows()) {
                Process p = new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(pid))
                    .redirectErrorStream(true)
                    .start();
                p.waitFor();
            }
            return true;
        }
        catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return os.contains("win");
    }
}
