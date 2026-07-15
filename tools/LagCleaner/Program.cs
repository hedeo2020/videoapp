using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;

namespace SecureStreamLagCleaner
{
    internal static class Program
    {
        private static readonly HashSet<string> AlwaysClean = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
        {
            "adb",
            "emulator",
            "qemu-system-x86_64",
            "qemu-system-aarch64",
            "studio",
            "studio64",
            "idea",
            "idea64",
            "gradle",
            "gradlew",
            "ffmpeg",
            "node_repl"
        };

        private static readonly HashSet<string> NeverClean = new HashSet<string>(StringComparer.OrdinalIgnoreCase)
        {
            "ChatGPT",
            "codex",
            "codex-command-runner-0.144.2",
            "chrome",
            "msedge",
            "msedgewebview2",
            "explorer",
            "Viber",
            "powershell",
            "pwsh",
            "cmd",
            "conhost"
        };

        private static int Main(string[] args)
        {
            Console.Title = "SecureStream Lag Cleaner";
            Console.WriteLine("SecureStream Lag Cleaner");
            Console.WriteLine("Closes safe leftover Android/build/media processes only.");
            Console.WriteLine();

            var dryRun = args.Any(arg => arg.Equals("--dry-run", StringComparison.OrdinalIgnoreCase));
            var closed = 0;
            var skipped = 0;
            var failed = 0;

            foreach (var process in Process.GetProcesses().OrderByDescending(p => SafeMemoryMb(p)))
            {
                if (!ShouldClean(process, out var reason))
                {
                    skipped++;
                    continue;
                }

                var name = SafeName(process);
                var id = process.Id;
                var memory = SafeMemoryMb(process);
                if (dryRun)
                {
                    Console.WriteLine($"Would close: {name} pid {id} ({memory:0.0} MB) - {reason}");
                    continue;
                }

                try
                {
                    process.Kill();
                    process.WaitForExit(2500);
                    Console.WriteLine($"Closed: {name} pid {id} ({memory:0.0} MB) - {reason}");
                    closed++;
                }
                catch (Exception ex)
                {
                    Console.WriteLine($"Could not close: {name} pid {id} - {ex.Message}");
                    failed++;
                }
            }

            Console.WriteLine();
            Console.WriteLine(dryRun ? "Dry run finished." : "Cleanup finished.");
            Console.WriteLine($"Closed: {closed} | Failed: {failed} | Checked/skipped: {skipped}");
            Console.WriteLine();
            Console.WriteLine("Tip: If Windows is still lagging, close browser tabs or restart Windows.");
            Console.WriteLine("Press Enter to exit.");
            Console.ReadLine();
            return failed > 0 ? 1 : 0;
        }

        private static bool ShouldClean(Process process, out string reason)
        {
            reason = "";
            var name = SafeName(process);
            if (string.IsNullOrWhiteSpace(name)) return false;
            if (NeverClean.Contains(name)) return false;

            if (AlwaysClean.Contains(name))
            {
                reason = "known leftover dev/device process";
                return true;
            }

            if (name.Equals("java", StringComparison.OrdinalIgnoreCase) ||
                name.Equals("javaw", StringComparison.OrdinalIgnoreCase))
            {
                if (process.MainWindowHandle == IntPtr.Zero)
                {
                    reason = "background Java process, usually Gradle/build daemon";
                    return true;
                }
            }

            return false;
        }

        private static string SafeName(Process process)
        {
            try { return process.ProcessName; }
            catch { return ""; }
        }

        private static double SafeMemoryMb(Process process)
        {
            try { return process.WorkingSet64 / 1024.0 / 1024.0; }
            catch { return 0; }
        }
    }
}
