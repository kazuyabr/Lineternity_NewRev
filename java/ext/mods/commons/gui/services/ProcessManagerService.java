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
package ext.mods.commons.gui.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import ext.mods.commons.gui.ThemeManager;
import ext.mods.commons.util.JvmOptimizer;

public class ProcessManagerService {
    
    private static final Preferences prefs = Preferences.userRoot().node("ram_allocation_settings");

    public ProcessManagerService() {
    }

    private String getJavaExecutable() {
        String ext = System.getProperty("os.name").toLowerCase().contains("win") ? ".exe" : "";

        // 1) JAVA_HOME vindo do ambiente.
        String javaHome = System.getenv("JAVA_HOME");
        String resolved = resolveJavaFromHome(javaHome, ext);
        if (resolved != null) {
            if (resolved.startsWith("ENV:")) {
                System.err.println("[INFO] Java detectado via JAVA_HOME (" + resolved.substring(4) + ").");
            }
            return resolved.startsWith("ENV:") ? resolved.substring(4) : resolved;
        }
        System.err.println("[AVISO] JAVA_HOME nao definido. Procurando Java no sistema...");

        // 2) java.home embutido (propriedade da JVM em execucao).
        String embeddedHome = System.getProperty("java.home");
        resolved = resolveJavaFromHome(embeddedHome, ext);
        if (resolved != null) return resolved;

        // 3) Varredura de diretorios comuns no Windows.
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            resolved = scanCommonJavaHomes(ext);
            if (resolved != null) return resolved;
        }

        // 4) PATH via `where` (Windows) ou `which` (Linux/Mac).
        try {
            ProcessBuilder pb = new ProcessBuilder(
                System.getProperty("os.name").toLowerCase().contains("win") ? new String[]{"where", "java"} : new String[]{"sh", "-c", "command -v java"}
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (java.util.Scanner sc = new java.util.Scanner(p.getInputStream())) {
                if (sc.hasNextLine()) {
                    String fromPath = sc.nextLine().trim();
                    if (!fromPath.isEmpty() && new File(fromPath).exists()) {
                        System.err.println("[INFO] Java encontrado no PATH: " + fromPath);
                        return fromPath;
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}

        // 5) Fallback: comando global. Caller lida com FileNotFoundException.
        System.err.println("[AVISO] Java nao encontrado. Tentando executar comando global 'java'.");
        return "java";
    }

    /**
     * Recebe um valor que se *parece* com JAVA_HOME e tenta extrair o
     * caminho do executavel. Aceita:
     *   - pasta do JDK            (ex.: "C:\jdk-25")
     *   - pasta com \bin incluido (ex.: "C:\jdk-25\bin")
     *   - caminho ate o java.exe  (ex.: "C:\jdk-25\bin\java.exe")
     * Retorna o caminho do executavel ou null se nao existir.
     */
    private String resolveJavaFromHome(String home, String ext) {
        if (home == null) return null;
        String h = home.trim();
        if (h.isEmpty()) return null;
        // remove aspas
        while (h.length() >= 2 && h.startsWith("\"") && h.endsWith("\"")) {
            h = h.substring(1, h.length() - 1);
        }
        // remove barra final
        while (h.endsWith("\\") || h.endsWith("/")) h = h.substring(0, h.length() - 1);

        // Se ja termina em java(.exe), retorna direto
        String lower = h.toLowerCase();
        if (lower.endsWith("java" + ext) && new File(h).exists()) return h;

        // Caso 1: h == "...\jdk-XX"        -> h\bin\java(.exe)
        // Caso 2: h == "...\jdk-XX\bin"    -> h\java(.exe)
        String c1 = h + File.separator + "bin" + File.separator + "java" + ext;
        if (new File(c1).exists()) return c1;
        String c2 = h + File.separator + "java" + ext;
        if (new File(c2).exists()) return c2;

        return null;
    }

    /**
     * Varre locais comuns onde JDKs costumam estar instalados no Windows.
     */
    private String scanCommonJavaHomes(String ext) {
        String pattern = "C:\\Program Files\\Eclipse Adoptium\\jdk-*;"
                + "C:\\Program Files\\Java\\jdk-*;"
                + "C:\\Program Files\\Microsoft\\jdk-*;"
                + "C:\\Program Files\\Zulu\\zulu-*;"
                + "C:\\Program Files\\Amazon Corretto\\jdk*;"
                + "C:\\Program Files\\BellSoft\\LibericaJDK-*;"
                + "C:\\Program Files\\OpenJDK\\jdk-*;"
                + "C:\\Program Files\\AdoptOpenJDK\\jdk-*;"
                + "C:\\jdk-*;"
                + "C:\\Program Files (x86)\\Eclipse Adoptium\\jdk-*;"
                + "C:\\Program Files (x86)\\Java\\jdk-*";

        for (String glob : pattern.split(";")) {
            File parent = new File(glob);
            File[] matches = parent.listFiles(new java.io.FilenameFilter() {
                @Override public boolean accept(File dir, String name) { return true; }
            });
            if (matches == null) continue;
            // ordena para pegar a versao mais recente
            java.util.Arrays.sort(matches, Comparator.comparing(File::getName).reversed());
            for (File m : matches) {
                if (!m.isDirectory()) continue;
                String p = m.getAbsolutePath() + "\\bin\\java" + ext;
                if (new File(p).exists()) {
                    System.err.println("[INFO] Java localizado em: " + p);
                    return p;
                }
            }
        }
        return null;
    }

    public void iniciarProcesso(String tipo, String licenseKey, String userEmail, boolean isLightModeEnabled, JFrame frame) {
        
        int memoryMB;
        if (tipo.equalsIgnoreCase("gameserver")) {
            memoryMB = prefs.getInt("gsMemoryMB", 2048);
        } else {
            memoryMB = prefs.getInt("lsMemoryMB", 512);
        }

        System.out.println("\n============================================================");
        System.out.println("  Iniciando " + tipo.toUpperCase() + " com JVM Otimizada");
        System.out.println("============================================================");
        System.out.println("  Memoria JVM: Xms=" + memoryMB + "MB | Xmx=" + memoryMB + "MB");
        
        String caminhoJava = getJavaExecutable();

        if (!new File(caminhoJava).exists()) {
            System.err.println("[AVISO] Caminho exato do Java não encontrado: " + caminhoJava + ". Tentando executar comando global 'java'.");
            caminhoJava = "java";
        }

        File diretorioExecucao = tipo.equals("gameserver") ? new File("game") : new File("login");

        if (!diretorioExecucao.exists()) {
            JOptionPane.showMessageDialog(frame, "A pasta '" + diretorioExecucao.getAbsolutePath() + "' não existe!", "Erro Crítico", JOptionPane.ERROR_MESSAGE);
            return;
        }

        
        String cpString = "";
        try {
            final File libsDir = new File(diretorioExecucao, "../libs").getCanonicalFile();
            cpString = JvmOptimizer.buildRuntimeClasspath(libsDir); 
        } catch (Exception e) {
            System.err.println("[AVISO] Classpath ordenado falhou, usando libs/*: " + e.getMessage());
            cpString = ".." + File.separator + "libs" + File.separator + "*"; 
        }
        

        String mainClass = tipo.equals("gameserver") ? "ext.mods.gameserver.GameServer" : "ext.mods.loginserver.LoginServer";

        List<String> command = new ArrayList<>();
        command.add(caminhoJava);
        
        command.add("-Xms" + memoryMB + "m");
        command.add("-Xmx" + memoryMB + "m");
        
        // Forçar UTF-8 no stdout/stderr do filho para evitar corrupção de acentos/Unicode
        command.add("-Dfile.encoding=UTF-8");
        command.add("-Dsun.stdout.encoding=UTF-8");
        command.add("-Dsun.stderr.encoding=UTF-8");
        
        if (ThemeManager.isSafeGraphics()) {
            command.add("-Dsun.java2d.opengl=false");
            command.add("-Dsun.java2d.d3d=false");
            command.add("-Dsun.java2d.pmoffscreen=false");
            command.add("-Dlineternity.safe.graphics=true");
        }
        
        command.add("-XX:+UseG1GC");
        command.add("-XX:MaxGCPauseMillis=200");
        command.add("-XX:G1HeapRegionSize=16m");
        command.add("-XX:+UseStringDeduplication");
        command.add("-XX:+UseCompressedOops");
        command.add("-XX:+UseCompactObjectHeaders");
        command.add("-XX:+TieredCompilation");
        command.add("-XX:TieredStopAtLevel=4");
        
        if (tipo.equals("gameserver"))
        {
            command.add("-XX:+AutoCreateSharedArchive");
            command.add("-XX:SharedArchiveFile=cache/lineternity_cds.jsa");
            command.add("-Xlog:cds=error");
        }

        command.add("-cp");
        command.add(cpString);
        command.add(mainClass);
        
        if (tipo.equals("gameserver")) {
            command.add(licenseKey);
            command.add(userEmail);
        }

        // Desabilitado: ocultando log do comando JVM para manter painel limpo
        // System.out.println("\n--- COMANDO JVM OTIMIZADO ---");
        // System.out.println(String.join(" ", command));
        // System.out.println("-----------------------------\n");

        new Thread(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(diretorioExecucao);
                pb.redirectErrorStream(true);
                Process processo = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(processo.getInputStream(), StandardCharsets.UTF_8))) {
                    String linha;
                    while ((linha = reader.readLine()) != null) {
                        System.out.println("[" + tipo.toUpperCase() + "] " + linha);
                    }
                }

                int exitCode = processo.waitFor();
                
                if (exitCode == 2) {
                    System.out.println("Reiniciando servidor...");
                    Thread.sleep(1000);
                    iniciarProcesso(tipo, licenseKey, userEmail, isLightModeEnabled, frame);
                } 
                else if (exitCode != 0) {
                    SwingUtilities.invokeLater(() -> 
                        JOptionPane.showMessageDialog(frame, 
                            "Erro no servidor (Código " + exitCode + ").", 
                            "Erro", JOptionPane.ERROR_MESSAGE)
                    );
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}