using Microsoft.Win32;
using System.Diagnostics;
using System.IO;
using System.Windows;
using System.Windows.Media;

namespace T1Launcher;

public partial class MainWindow : Window
{
    private bool _building;

    public MainWindow()
    {
        InitializeComponent();
        MinecraftPathBox.Text = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), ".minecraft");
        ProjectPathBox.Text = FindProjectRoot() ?? Environment.GetFolderPath(Environment.SpecialFolder.MyDocuments);
        AppendLog("T1 Launcher started.");
        AppendLog("Target: Minecraft Java 1.21.11 / Fabric / Java 21.");
        RefreshStatus();
    }

    private void BrowseMinecraft_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFolderDialog { Title = "Select your .minecraft folder", Multiselect = false };
        if (Directory.Exists(MinecraftPathBox.Text)) dialog.InitialDirectory = MinecraftPathBox.Text;
        if (dialog.ShowDialog(this) == true)
        {
            MinecraftPathBox.Text = dialog.FolderName;
            RefreshStatus();
        }
    }

    private void BrowseProject_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFolderDialog { Title = "Select the T1 Client project folder", Multiselect = false };
        if (Directory.Exists(ProjectPathBox.Text)) dialog.InitialDirectory = ProjectPathBox.Text;
        if (dialog.ShowDialog(this) == true)
        {
            ProjectPathBox.Text = dialog.FolderName;
            RefreshStatus();
        }
    }

    private async void BuildMod_Click(object sender, RoutedEventArgs e)
    {
        if (_building) return;
        string project = ProjectPathBox.Text.Trim();
        if (!Directory.Exists(project) || !File.Exists(Path.Combine(project, "gradlew.bat")))
        {
            SetStatus("Select the project folder", isError: true);
            AppendLog("ERROR: The selected folder must contain gradlew.bat and build.gradle.");
            return;
        }

        _building = true;
        SetButtonsEnabled(false);
        SetStatus("Building…");
        AppendLog("> gradlew.bat build");

        try
        {
            var start = new ProcessStartInfo
            {
                FileName = "cmd.exe",
                Arguments = "/d /c gradlew.bat build",
                WorkingDirectory = project,
                UseShellExecute = false,
                RedirectStandardOutput = true,
                RedirectStandardError = true,
                CreateNoWindow = true
            };
            using var process = new Process { StartInfo = start, EnableRaisingEvents = true };
            process.OutputDataReceived += (_, args) => { if (args.Data is not null) Dispatcher.Invoke(() => AppendLog(args.Data)); };
            process.ErrorDataReceived += (_, args) => { if (args.Data is not null) Dispatcher.Invoke(() => AppendLog(args.Data)); };
            process.Start();
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();
            await process.WaitForExitAsync();

            if (process.ExitCode == 0)
            {
                SetStatus("Build succeeded");
                AppendLog("BUILD SUCCESS. You can install the output JAR now.");
            }
            else
            {
                SetStatus($"Build failed ({process.ExitCode})", isError: true);
                AppendLog("BUILD FAILED. Review the output above; Gradle may need an internet connection to download dependencies.");
            }
        }
        catch (Exception ex)
        {
            SetStatus("Build could not start", isError: true);
            AppendLog("ERROR: " + ex.Message);
        }
        finally
        {
            _building = false;
            SetButtonsEnabled(true);
            RefreshStatus(keepStatus: true);
        }
    }

    private void InstallJar_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            string project = ProjectPathBox.Text.Trim();
            string minecraft = MinecraftPathBox.Text.Trim();
            if (!Directory.Exists(project)) throw new DirectoryNotFoundException("Project directory does not exist.");
            if (!Directory.Exists(minecraft)) throw new DirectoryNotFoundException("Minecraft directory does not exist. Use Browse to select it.");

            string jar = FindReleaseJar(project) ?? throw new FileNotFoundException("No release JAR found in build/libs. Click Build Mod first.");
            string modsDirectory = Path.Combine(minecraft, "mods");
            Directory.CreateDirectory(modsDirectory);
            string destination = Path.Combine(modsDirectory, Path.GetFileName(jar));
            File.Copy(jar, destination, overwrite: true);
            AppendLog($"Installed: {destination}");
            SetStatus("Mod installed");
            MessageBox.Show(this,
                "T1 Client was copied to the mods folder. Make sure the official launcher has a Fabric 1.21.11 profile selected before starting Minecraft.",
                "T1 Client installed", MessageBoxButton.OK, MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            AppendLog("ERROR: " + ex.Message);
            SetStatus("Install failed", isError: true);
            MessageBox.Show(this, ex.Message, "Could not install T1 Client", MessageBoxButton.OK, MessageBoxImage.Warning);
        }
    }

    private void OpenMods_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            string mods = Path.Combine(MinecraftPathBox.Text.Trim(), "mods");
            Directory.CreateDirectory(mods);
            Process.Start(new ProcessStartInfo("explorer.exe", $"\"{mods}\"") { UseShellExecute = true });
        }
        catch (Exception ex)
        {
            AppendLog("ERROR: " + ex.Message);
            SetStatus("Could not open mods folder", isError: true);
        }
    }

    private void OpenLauncher_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            string[] candidates =
            {
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Programs", "Minecraft Launcher", "MinecraftLauncher.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles), "Minecraft Launcher", "MinecraftLauncher.exe"),
                Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86), "Minecraft Launcher", "MinecraftLauncher.exe")
            };
            string? launcher = candidates.FirstOrDefault(File.Exists);
            if (launcher is not null)
            {
                Process.Start(new ProcessStartInfo(launcher) { UseShellExecute = true });
                AppendLog("Opened the installed Minecraft Launcher.");
                SetStatus("Launcher opened");
                return;
            }

            Process.Start(new ProcessStartInfo("minecraft://") { UseShellExecute = true });
            AppendLog("Requested Minecraft Launcher via the registered minecraft:// protocol.");
            SetStatus("Launcher request sent");
        }
        catch (Exception ex)
        {
            AppendLog("Could not open Minecraft Launcher: " + ex.Message);
            SetStatus("Open launcher manually", isError: true);
            MessageBox.Show(this,
                "Windows did not find a registered Minecraft Launcher. Open the official launcher manually, then select your Fabric 1.21.11 installation.",
                "Minecraft Launcher not found", MessageBoxButton.OK, MessageBoxImage.Information);
        }
    }

    private static string? FindReleaseJar(string projectDirectory)
    {
        // Prefer a freshly built JAR, then fall back to a prebuilt JAR shipped next to the launcher EXE.
        var dirs = new List<string> { Path.Combine(projectDirectory, "build", "libs"), AppContext.BaseDirectory };
        return dirs.Where(Directory.Exists)
            .SelectMany(d => Directory.GetFiles(d, "t1client*.jar"))
            .Where(path => !Path.GetFileName(path).Contains("-dev", StringComparison.OrdinalIgnoreCase)
                        && !Path.GetFileName(path).Contains("-sources", StringComparison.OrdinalIgnoreCase)
                        && !Path.GetFileName(path).Contains("-javadoc", StringComparison.OrdinalIgnoreCase))
            .OrderByDescending(File.GetLastWriteTimeUtc)
            .FirstOrDefault();
    }

    private static string? FindProjectRoot()
    {
        foreach (string start in new[] { AppContext.BaseDirectory, Environment.CurrentDirectory })
        {
            var current = new DirectoryInfo(start);
            while (current is not null)
            {
                string sibling = Path.Combine(current.FullName, "fabric-mod");
                if (File.Exists(Path.Combine(sibling, "gradlew.bat")) && File.Exists(Path.Combine(sibling, "build.gradle")))
                    return sibling;
                if (File.Exists(Path.Combine(current.FullName, "gradlew.bat"))
                    && File.Exists(Path.Combine(current.FullName, "build.gradle")))
                    return current.FullName;
                current = current.Parent;
            }
        }
        return null;
    }

    private void RefreshStatus(bool keepStatus = false)
    {
        string minecraft = MinecraftPathBox.Text.Trim();
        string project = ProjectPathBox.Text.Trim();
        if (Directory.Exists(minecraft))
        {
            MinecraftPathHint.Text = Directory.Exists(Path.Combine(minecraft, "mods"))
                ? "Minecraft folder found • mods folder is ready"
                : "Minecraft folder found • mods folder will be created when needed";
        }
        else
        {
            MinecraftPathHint.Text = "Folder not found • select your Minecraft Java game directory";
        }

        if (!keepStatus && FindReleaseJar(project) is not null) SetStatus("Release JAR ready to install");
        else if (!keepStatus && Directory.Exists(project)) SetStatus("Ready to build");
    }

    private void SetStatus(string text, bool isError = false)
    {
        StatusText.Text = text;
        StatusText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString(isError ? "#FF7777" : "#CB8CFF"));
    }

    private void SetButtonsEnabled(bool enabled)
    {
        // Buttons are nested inside panels, so traverse the visual tree instead of
        // only inspecting the root Grid. Otherwise the build/install controls remain active.
        foreach (var button in FindVisualChildren<System.Windows.Controls.Button>(this))
            button.IsEnabled = enabled;
    }

    private static IEnumerable<T> FindVisualChildren<T>(DependencyObject parent) where T : DependencyObject
    {
        int count = VisualTreeHelper.GetChildrenCount(parent);
        for (int index = 0; index < count; index++)
        {
            DependencyObject child = VisualTreeHelper.GetChild(parent, index);
            if (child is T typedChild)
                yield return typedChild;

            foreach (T descendant in FindVisualChildren<T>(child))
                yield return descendant;
        }
    }

    private void AppendLog(string line)
    {
        BuildLog.AppendText($"[{DateTime.Now:HH:mm:ss}] {line}{Environment.NewLine}");
        BuildLog.ScrollToEnd();
    }
}
