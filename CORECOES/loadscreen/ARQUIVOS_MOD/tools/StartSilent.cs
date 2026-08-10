// Start silencioso para Brproject_Distribution — sem janela CMD.
// Desenvolvido por Dev ⩽ A.L.N/⩾ para o BrProject.
using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

internal static class StartSilent
{
    [STAThread]
    private static void Main()
    {
        try
        {
            string baseDir = AppDomain.CurrentDomain.BaseDirectory;
            Directory.SetCurrentDirectory(baseDir);

            string javaw = FindJavaw(baseDir);
            if (javaw == null)
            {
                MessageBox.Show(
                    "Java 25 (javaw.exe) nao encontrado.\nVerifique Gradle\\jdk-25\\bin\\javaw.exe",
                    "Brproject",
                    MessageBoxButtons.OK,
                    MessageBoxIcon.Error);
                return;
            }

            ProcessStartInfo psi = new ProcessStartInfo();
            psi.FileName = javaw;
            psi.Arguments =
                "-Xms256m -Xmx512m " +
                "-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 " +
                "-Dsun.java2d.opengl=false -Dsun.java2d.d3d=false -Dsun.java2d.pmoffscreen=false " +
                "-Dbrproject.safe.graphics=true -Dbrproject.quiet.console=true " +
                "-cp \"libs/*\" ext.mods.security.LicenseInit";
            psi.WorkingDirectory = baseDir;
            psi.UseShellExecute = false;
            psi.CreateNoWindow = true;
            psi.WindowStyle = ProcessWindowStyle.Hidden;

            Process.Start(psi);
        }
        catch (Exception ex)
        {
            MessageBox.Show(ex.Message, "Brproject - erro ao iniciar", MessageBoxButtons.OK, MessageBoxIcon.Error);
        }
    }

    private static string FindJavaw(string baseDir)
    {
        string[] candidates = new string[]
        {
            Path.Combine(baseDir, "Gradle", "jdk-25", "bin", "javaw.exe"),
            Path.Combine(baseDir, "gradle", "jdk-25", "bin", "javaw.exe"),
            @"D:\core-jdk-25\bin\javaw.exe",
            @"D:\core jdk 25\bin\javaw.exe",
            Path.Combine(Environment.GetEnvironmentVariable("JAVA_HOME") ?? "", "bin", "javaw.exe")
        };

        foreach (string c in candidates)
        {
            if (string.IsNullOrWhiteSpace(c) || !File.Exists(c))
                continue;
            if (c.EndsWith("java.exe", StringComparison.OrdinalIgnoreCase))
            {
                string jw = c.Substring(0, c.Length - 8) + "javaw.exe";
                if (File.Exists(jw))
                    return jw;
            }
            return c;
        }
        return null;
    }
}
