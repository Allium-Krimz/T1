# T1 Client

Fabric client mod for Minecraft 1.21.11 plus a native Windows launcher (WPF, no Godot).

Modules: Block ESP, Base Finder, Sus Chunk Finder. Right Shift opens the menu in game.
Needs Fabric Loader 0.18.1+ and Fabric API 0.141.6+1.21.11.

## Easiest: build in the cloud (nothing to install)
1. Create a GitHub repo and push this folder to it.
2. Actions tab -> "Build T1 Client (EXE + JAR)" -> Run workflow.
3. Download the `T1Client-windows` artifact. It holds `T1Launcher.exe`, `t1client-1.2.0.jar`, and `fabric-mod/`.
4. Run `T1Launcher.exe` -> INSTALL JAR (copies the jar into `.minecraft/mods`).

## Build locally on Windows
Install JDK 21 and the .NET 10 SDK, then run `fabric-mod\build-all.bat`.
- JAR: `fabric-mod\build\libs\t1client-1.2.0.jar`
- EXE: `fabric-mod\launcher\T1Launcher\bin\Release\net10.0-windows\win-x64\publish\T1Launcher.exe`
