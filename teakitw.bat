@echo off
rem
rem Copyright 2026 iamkaf
rem
rem Licensed under the Apache License, Version 2.0 (the "License");
rem you may not use this file except in compliance with the License.
rem You may obtain a copy of the License at
rem
rem     https://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

setlocal EnableExtensions EnableDelayedExpansion

set "TEAKIT_RUNNER_PINNED_VERSION=0.3.0-SNAPSHOT"
set "TEAKIT_GROUP_PATH=com/iamkaf/teakit"
set "TEAKIT_GROUP_PATH_WINDOWS=com\iamkaf\teakit"
set "TEAKIT_ARTIFACT=teakit-runner"

rem TEAKIT_RUNNER_JAR is the escape hatch for local runner development. When it
rem is set, skip platform detection and Maven resolution entirely.
if not "%TEAKIT_RUNNER_JAR%"=="" (
  java -jar "%TEAKIT_RUNNER_JAR%" %*
  exit /b %ERRORLEVEL%
)

if "%TEAKIT_MAVEN_RELEASES%"=="" set "TEAKIT_MAVEN_RELEASES=https://z.kaf.sh/releases"
if "%TEAKIT_MAVEN_SNAPSHOTS%"=="" set "TEAKIT_MAVEN_SNAPSHOTS=https://z.kaf.sh/snapshots"
if "%TEAKIT_MAVEN_LOCAL%"=="" set "TEAKIT_MAVEN_LOCAL=%USERPROFILE%\.m2\repository"
if "%TEAKIT_CACHE_DIR%"=="" (
  set "TEAKIT_CACHE_ROOT=%USERPROFILE%\.cache\teakit"
) else (
  set "TEAKIT_CACHE_ROOT=%TEAKIT_CACHE_DIR%"
)

rem TeaKit Runner embeds native Javet and swc4j libraries. Resolve a
rem platform-classified jar so each developer downloads only the native
rem libraries for their machine instead of the old all-platform runner.
if "%TEAKIT_RUNNER_CLASSIFIER%"=="" (
  for /f "usebackq delims=" %%C in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$os = if ([System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Windows)) { 'windows' } elseif ([System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::Linux)) { 'linux' } elseif ([System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform([System.Runtime.InteropServices.OSPlatform]::OSX)) { 'macos' } else { throw 'unsupported OS' }; $cpu = switch ([System.Runtime.InteropServices.RuntimeInformation]::OSArchitecture.ToString().ToLowerInvariant()) { 'x64' { 'x64' } 'arm64' { 'arm64' } default { throw 'unsupported CPU architecture' } }; $classifier = $os + '-' + $cpu; if ($classifier -eq 'windows-arm64') { throw 'TeaKit runner does not support windows-arm64 yet because Javet Node has no windows-arm64 artifact' }; if (@('linux-x64','linux-arm64','macos-x64','macos-arm64','windows-x64') -notcontains $classifier) { throw ('unsupported TeaKit runner platform: ' + $classifier) }; Write-Output $classifier"`) do set "TEAKIT_RUNNER_CLASSIFIER=%%C"
  if "!TEAKIT_RUNNER_CLASSIFIER!"=="" (
    echo teakitw: could not detect a supported TeaKit runner platform
    exit /b 1
  )
)

rem `upgrade` does not run TeaKit. It resolves/downloads the newest runner and
rem exits, which lets repositories refresh the cached runner before normal use.
set "TEAKIT_WRAPPER_UPGRADE=0"
if not "%TEAKIT_RUNNER_VERSION%"=="" (
  set "TEAKIT_RUNNER_RESOLVED_VERSION=%TEAKIT_RUNNER_VERSION%"
  if /I "%~1"=="upgrade" set "TEAKIT_WRAPPER_UPGRADE=1"
) else if /I "%~1"=="upgrade" (
  set "TEAKIT_WRAPPER_UPGRADE=1"
  for /f "usebackq delims=" %%V in (`powershell -NoProfile -ExecutionPolicy Bypass -Command "$releases = '%TEAKIT_MAVEN_RELEASES%'; $snapshots = '%TEAKIT_MAVEN_SNAPSHOTS%'; $group = '%TEAKIT_GROUP_PATH%'; $artifact = '%TEAKIT_ARTIFACT%'; $urls = @($releases + '/' + $group + '/' + $artifact + '/maven-metadata.xml', $snapshots + '/' + $group + '/' + $artifact + '/maven-metadata.xml'); foreach ($url in $urls) { try { [xml]$metadata = Invoke-WebRequest -UseBasicParsing -Uri $url; $versioning = $metadata.metadata.versioning; if ($versioning.release) { Write-Output $versioning.release; exit 0 }; if ($versioning.latest) { Write-Output $versioning.latest; exit 0 }; $last = @($versioning.versions.version) | Select-Object -Last 1; if ($last) { Write-Output $last; exit 0 } } catch {} }; exit 1"`) do set "TEAKIT_RUNNER_RESOLVED_VERSION=%%V"
  if "!TEAKIT_RUNNER_RESOLVED_VERSION!"=="" (
    echo teakitw: could not resolve latest TeaKit runner from Kaf Maven
    echo teakitw: set TEAKIT_RUNNER_VERSION=^<version^> to upgrade to an explicit version
    exit /b 1
  )
) else (
  set "TEAKIT_RUNNER_RESOLVED_VERSION=%TEAKIT_RUNNER_PINNED_VERSION%"
)

set "DIST_DIR=%TEAKIT_CACHE_ROOT%\wrapper\dists\%TEAKIT_ARTIFACT%\%TEAKIT_RUNNER_RESOLVED_VERSION%\%TEAKIT_RUNNER_CLASSIFIER%"
set "JAR_PATH=%DIST_DIR%\%TEAKIT_ARTIFACT%.jar"

rem Prefer Maven local during development so unpublished runner builds can be
rem dogfooded from mod repositories without touching Kaf Maven.
if /I not "%TEAKIT_MAVEN_LOCAL%"=="false" (
  set "LOCAL_JAR=%TEAKIT_MAVEN_LOCAL%\%TEAKIT_GROUP_PATH_WINDOWS%\%TEAKIT_ARTIFACT%\%TEAKIT_RUNNER_RESOLVED_VERSION%\%TEAKIT_ARTIFACT%-%TEAKIT_RUNNER_RESOLVED_VERSION%-%TEAKIT_RUNNER_CLASSIFIER%.jar"
  if exist "%LOCAL_JAR%" (
    if "%TEAKIT_WRAPPER_UPGRADE%"=="1" (
      echo teakitw: TeaKit runner %TEAKIT_RUNNER_RESOLVED_VERSION% ^(%TEAKIT_RUNNER_CLASSIFIER%^) is available from Maven local: "%LOCAL_JAR%"
      exit /b 0
    )
    java -jar "%LOCAL_JAR%" %*
    exit /b %ERRORLEVEL%
  )
)

if exist "%JAR_PATH%" goto run

rem Cache the resolved platform jar under a stable local filename. Maven keeps
rem the classifier in the artifact filename, but the cache path already includes
rem it.
mkdir "%DIST_DIR%" 2>nul
powershell -NoProfile -ExecutionPolicy Bypass -Command ^
  "$version = '%TEAKIT_RUNNER_RESOLVED_VERSION%';" ^
  "$releases = '%TEAKIT_MAVEN_RELEASES%';" ^
  "$snapshots = '%TEAKIT_MAVEN_SNAPSHOTS%';" ^
  "$group = '%TEAKIT_GROUP_PATH%';" ^
  "$artifact = '%TEAKIT_ARTIFACT%';" ^
  "$out = '%JAR_PATH%';" ^
  "$base = if ($version.EndsWith('-SNAPSHOT')) { $snapshots } else { $releases };" ^
  "$resolved = $version;" ^
  "$classifier = '%TEAKIT_RUNNER_CLASSIFIER%';" ^
  "if ($version.EndsWith('-SNAPSHOT')) {" ^
  "  [xml]$metadata = Invoke-WebRequest -UseBasicParsing -Uri ($base + '/' + $group + '/' + $artifact + '/' + $version + '/maven-metadata.xml');" ^
  "  $snap = $metadata.metadata.versioning.snapshotVersions.snapshotVersion | Where-Object { $_.extension -eq 'jar' -and $_.classifier -eq $classifier } | Select-Object -First 1;" ^
  "  if (-not $snap) { throw ('snapshot metadata did not contain a TeaKit runner jar for ' + $classifier) };" ^
  "  $resolved = $snap.value;" ^
  "}" ^
  "$url = $base + '/' + $group + '/' + $artifact + '/' + $version + '/' + $artifact + '-' + $resolved + '-' + $classifier + '.jar';" ^
  "Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $out"
if errorlevel 1 (
  echo teakitw: failed to download TeaKit runner from Kaf Maven
  echo teakitw: set TEAKIT_RUNNER_JAR=C:\path\to\teakit-runner.jar for a local development runner
  exit /b 1
)

:run
if "%TEAKIT_WRAPPER_UPGRADE%"=="1" (
  echo teakitw: TeaKit runner %TEAKIT_RUNNER_RESOLVED_VERSION% ^(%TEAKIT_RUNNER_CLASSIFIER%^) is available at "%JAR_PATH%"
  exit /b 0
)

java -jar "%JAR_PATH%" %*
