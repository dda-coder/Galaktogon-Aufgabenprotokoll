#define MyAppName "GALAKTOGON Aufgabenprotokoll"
#define MyAppVersion "1.0.0"
#define MyAppPublisher "dda-coder"
#define MyAppURL "https://galaktogon-projekt.davidmayer.workers.dev/"
#define MyAppExeName "GAP-Launcher.ps1"

[Setup]
AppId={{A7E4C9D2-8B61-4F0D-A3B8-92C51D0E7A64}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
AppPublisherURL={#MyAppURL}
AppSupportURL={#MyAppURL}
AppUpdatesURL=https://github.com/dda-coder/Galaktogon-Aufgabenprotokoll/releases
DefaultDirName={localappdata}\Programs\GAP
DefaultGroupName={#MyAppName}
DisableProgramGroupPage=yes
PrivilegesRequired=lowest
OutputDir=dist
OutputBaseFilename=GAP-Setup-{#MyAppVersion}
SetupIconFile=assets\gap.ico
UninstallDisplayIcon={app}\gap.ico
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesInstallIn64BitMode=x64
Uninstallable=yes

[Files]
Source: "GAP-Launcher.ps1"; DestDir: "{app}"; Flags: ignoreversion
Source: "assets\gap.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{autoprograms}\GALAKTOGON Aufgabenprotokoll"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File ""{app}\GAP-Launcher.ps1"""; IconFilename: "{app}\gap.ico"; WorkingDir: "{app}"
Name: "{autodesktop}\GALAKTOGON Aufgabenprotokoll"; Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File ""{app}\GAP-Launcher.ps1"""; IconFilename: "{app}\gap.ico"; WorkingDir: "{app}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Desktop-Verknüpfung erstellen"; GroupDescription: "Zusätzliche Verknüpfungen:"

[Run]
Filename: "powershell.exe"; Parameters: "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File ""{app}\GAP-Launcher.ps1"""; Description: "GALAKTOGON Aufgabenprotokoll starten"; Flags: postinstall nowait skipifsilent
