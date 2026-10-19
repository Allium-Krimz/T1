# T1 Client — Minecraft 1.21.11

A client-side Fabric mod with a minimal dark/purple ClickGUI, module toggles, saved settings, and a separate Windows launcher project.

## Requirements

- Minecraft Java Edition **1.21.11**
- Fabric Loader **0.18.1 or newer**
- Fabric API `0.141.6+1.21.11`
- Fabric Loom `1.14.10`
- JDK 21 (not Java 17 or Java 25 for this build)
- Windows: .NET 10 SDK for building the launcher EXE

## Build the Fabric mod

1. Extract the ZIP to a short path without unusual characters, for example `C:\Projects\t1client-ghost-1.21.11`.
2. Install JDK 21 and make sure `java -version` reports Java 21.
3. Open Command Prompt in the project directory.
4. Run:

   ```bat
   gradlew.bat clean build
   ```

5. The remapped release JAR should appear in `build\libs\t1client-1.1.0.jar` (the exact filename follows `mod_version`).
6. Copy the release JAR into `%APPDATA%\.minecraft\mods`. Use a Fabric 1.21.11 game profile with Fabric API installed.

The project uses Mojang mappings consistently, a single `fabric.mod.json`, no sample Mixin targets, and no YACL dependency. This avoids the original source/mapping mismatch and stale 1.21.1 dependency.

## Build the Windows launcher EXE

Run `publish-launcher.bat` on Windows with the .NET 10 SDK installed. The single-file EXE will be created at:

`launcher\T1Launcher\bin\Release\net10.0-windows\win-x64\publish\T1Launcher.exe`

The launcher can run Gradle, install a release JAR into your selected `.minecraft\mods` directory, open the mods folder, and request the registered official Minecraft Launcher. It does **not** replace the official launcher or handle Microsoft account credentials. Building the EXE is a separate step from building the mod.

## In-game controls

- **Right Shift**: open/close T1 ClickGUI
- **Escape**: close the GUI
- Click categories to filter modules; use the search bar; click a row to toggle a module.
- Module states are saved to `config\t1client.properties` inside the Minecraft game directory.

## Current modules

- **Block ESP**: outlines nearby diamond ore, chests, and spawners found in the client-loaded area. Scans a small radius at an interval and caps the number of markers to avoid a large per-tick scan.
- **Base Finder**: scans every loaded chunk for clusters of storage blocks and spawners and reports them in chat.
- **Sus Chunk Finder**: scores loaded chunks by player-built and automation blocks and reports chunks over a threshold.

No anti-cheat bypass, detection hiding, or authentication workaround is included.

## Notes

The Java sources passed a syntax/type check against the locally cached Minecraft 1.21.11 mapped classes and API jars. This check used small placeholders for some external classes that were unavailable offline; it is not a substitute for the real Loom build or a runtime game test. A full Gradle build could not be executed here because the Gradle distribution/dependencies could not be downloaded, and the Windows EXE could not be built because the .NET SDK/Windows toolchain is not available in this environment. Run the commands above on your Windows PC to perform those full builds.
