# Warsmash: The OpenMW for Warcraft III

Warsmash is a free/open-source reimplementation of the Warcraft III engine —
the same long-term mission [OpenMW](https://openmw.org/) took for Morrowind:
full compatibility on a modern, community-maintained engine that can be studied,
modified, and preserved indefinitely.

Warsmash ships **engine code only**. You must own Warcraft III assets and point
Warsmash at them via `warsmash.ini`.

---

## Quick Start

### 1) Clone and run tests

```bash
git clone https://github.com/Retera/WarsmashModEngine.git
cd WarsmashModEngine
./gradlew :core:test
```

### 2) Configure assets

Edit `core/assets/warsmash.ini` and set the `[DataSources]` paths to your
Warcraft III installation (details and patch-specific notes below).

### 3) Launch

```bash
./gradlew :desktop:runGame
```

Useful launcher flags:

- `-help` — show all options and exit
- `-profile safe|balanced|high` — preset launch profile
- `-window [width height]` — force windowed mode
- `-fps <value>` — cap foreground/background FPS (`0` uncapped)
- `-vsync` / `-novsync`
- `-msaa <samples>` (including `-msaa 0` to disable)
- `-validate` — validate `warsmash.ini` data-source paths and exit
- `-ini <path>` — use a custom config
- `-loadfile <path>` — auto-load map or TOC
- `-nolog` — keep logs on console

---

## Vision

Warcraft III produced one of the most influential modding ecosystems in RTS
history. Warsmash aims to keep that ecosystem alive with an open engine that:

- runs WC3 maps/mods on modern systems,
- is portable (Java + LibGDX: Linux/Windows/macOS),
- is hackable and inspectable end-to-end,
- treats frame-time stability and allocation churn as first-class bugs,
- documents architecture/compatibility/perf tradeoffs in-repo.

---

### In the News
Some social posts in 2022 claimed Warsmash was taken down by Activision
Blizzard. That did **not** happen; confusion originated from a parody video
takedown. If you want details, ask in Discord.

## Gameplay Example
[![GAMEPLAY VIDEO](http://img.youtube.com/vi/EO-FDeQhFWc/0.jpg)](https://www.youtube.com/watch?v=EO-FDeQhFWc)

## Discord
https://discord.com/invite/ucjftZ7x7H

---

## Project Status & Roadmap

Modernization progress is tracked in:

- [`docs/ENGINE_MODERNIZATION_ANALYSIS.md`](docs/ENGINE_MODERNIZATION_ANALYSIS.md)
- [`CHANGELOG.md`](CHANGELOG.md)

| Phase | Focus | Status |
|-------|-------|--------|
| **A** | Diagnostics, launcher QoL, CI, docs | **Complete** |
| **B** | Light-system leak fix, GLSL normalization, parser consolidation design | **Complete** |
| **C** | Render hot-path allocation/frame-time reductions | **Complete** |
| **D** | Parser unification, server hardening, async asset pipeline | **In Progress** |
| **E** | JASS/Lua coverage, map-format support to 1.32, multiplayer hardening | Planned |
| **F** | Community modding layer (asset overrides, mod APIs/tooling) | Planned |

### Recent engineering highlights

- Parser migration completed for runtime `MappedData` callers through canonical
  `DataTable` paths.
- `ObjectPool<T>` and `SimulationBudgetTracker` are wired into simulation
  hotspots and per-tick diagnostics.
- Launcher profile/flag precedence was polished for deterministic overrides, and
  `-help` now exits before display initialization.
- Lightning-effect batching fixed byte/count mismatches in GL index-buffer
  uploads and avoids unnecessary index-buffer rebuilds when count is unchanged.
- Unit reach checks now use squared-distance fast paths where equivalent; a
  pathfinding bounds guard was hardened to avoid edge-case out-of-range access.

### Current top priority

The primary remaining Phase D deliverable is threaded async map/asset loading
with progress feedback.

---

## Before you Begin: INI File
Regardless of whether you want to edit Warsmash from an IDE, run it from command line, or build a binary release, in any case you will run into the problem of the `warsmash.ini` config file. I maintained the Retera Model Studio project since 2012, and in particular I was constantly updating my code in parallel with Activision Blizzard's Warcraft III leading up to the release of Reforged. The decisions their Classics Games Team made in those years substantially impacted and split apart the Warcraft III modding community. Rather than Reforged releasing all at once and breaking everything, at first from 2017 - 2019 there were a series of patches that dramatically altered and restructured the Warcraft III game, sometimes to an entirely new structure _in each patch_, all while **not using the Reforged label**. The first patch in that patch cycle was the Patch 1.27, followed by many others. Originally, in 2017, the fundamental Warcraft III asset loading utilities that I wrote for Retera Model Studio (that I now also use on Warsmash!) were using code that I had written to automatically detect the location of the "one true Warcraft III installation" on the user computer from the Windows registry.

But, unfortunately, Activision eliminated the use of this registry key and then switched to a different one by the time Reforged released. Along the way, there were some Frozen Throne builds that do not honor nor record a registry key of their location. Also, the files inside the Warcraft III installation were dramatically reorganized into Activision's newer content distribution archives in 2018, so even if it were possible to automatically locate a copy of Warcraft III on the user computer, it would not be a guarantee that the automatically detected game had any particular file layout or structure for where to find the 3D assets.

Because of this problem, Retera Model Studio made a fundamental switch in the year 2019 as the era of Frozen Throne was coming to an end and Reforged was beginning. I changed my "automatic detection" to simply not be automatic. By putting the user in control of finding their Warcraft III installation's files, the users were also able to fix any issues with locating assets for that tool.

So, when I copied the asset resolution from that system into Warsmash and expanded upon it, I likewise wanted a user-configurable notion of where to find assets. But now it had the added design constraint of aiming to allow users to record many different configurations to each their own INI file, and then use one single Warsmash binary to launch from any one of many possible configurations. The `warsmash.ini` block labeled `[DataSources]` essentially describes a virtual file system made from layers of one or more "places to look" for a file at a given path. There can be many archives loaded, and any attempt to resolve a given file looks at each archive in order to find it, from newest to oldest. So, setting up this virtual file system is _in the user's hands._ However, once the user establishes this, Warsmash can fundamentally accept and play the game from the virtual file system of any of the following (all of which I have tested at some point):

- Warcraft III: Frozen Throne: Patch 1.22 - 1.28
  - `[DataSources]` set to using MPQ files + the "resources" folder from Warsmash
  - `[Emulator]` block required to have `MaxPlayers=16`
- Warcraft III: Frozen Throne: Patch 1.29
  - `[DataSources]` set to using MPQ files + the "resources" folder from Warsmash, but `War3Patch.mpq` is gone and should be removed from the list
  - `[Emulator]` block required to have `MaxPlayers=28`
- Warcraft III: Frozen Throne: Patch 1.30
  - `[DataSources]` using manually extracted folders containing whatever is in the CASC files, this means folders named `.mpq` that are not `MPQ` archives like we see on previous patches
  - Still also include the "resources" folder from Warsmash (it is a folder with some necessary, patch-agnostic custom Warsmash assets that need to be in this virtual file system)
  - The CASC parser in Warsmash is only working for Patch 1.32.10 and beyond, currently, but Warsmash will probe for the unique Patch 1.30 folder names for tilesets and such if they are included in the virtual file system  -- these file names do not match any other patch
  - `[Emulator]` block required to have `MaxPlayers=28`
  - This patch has not been tested in a year or so on Warsmash and may experience unexpected issues from more recent code changes
- Warcraft III: Frozen Throne: Patch 1.31
  - `[DataSources]` using manually extracted folders containing whatever is in the CASC files, this means folders named `.w3mod` that are not `MPQ` archives like we see on previous patches and are not even named `MPQ` on this patch
  - Still also include the "resources" folder from Warsmash (it is a folder with some necessary, patch-agnostic custom Warsmash assets that need to be in this virtual file system)
  - The CASC parser in Warsmash is only working for Patch 1.32.10 and beyond, currently, but Warsmash will probe for the unique Patch 1.31 folder names for tilesets and such if they are included in the virtual file system  -- these file names also match Reforged typically
  - `[Emulator]` block required to have `MaxPlayers=28`
  - This patch has not been tested in a year or so on Warsmash and may experience unexpected issues from more recent code changes
- Warcraft III: Frozen Throne: Patch 1.32
  - `[DataSources]` using CASC directly. The Warsmash INI has a special notation called "Prefixes" to designate which `.w3mod`'s to load, which is the new naming from 1.31 and onward, using subdirectories of the virtual file system to replace what had once been independent MPQ archives on older versions
  - If you want to display the Reforged HD unit models, add the `war3.w3mod\_hd.w3mod` prefix and also `war3.w3mod\_hd.w3mod\_locales\enus.w3mod`  (see `./core/assets/warsmashRF.ini` as an example), but you could use a different language instead of `enus` if you wanted. To see how use of `_hd.w3mod` might look: https://youtu.be/-Oa3iOY6wU0
  - Still also include the "resources" folder from Warsmash (it is a folder with some necessary, patch-agnostic custom Warsmash assets that need to be in this virtual file system)
  - The CASC parser in Warsmash is only working for Patch 1.32.10 and beyond, currently, but was tested and worked great on 1.32.10 for me for the last year or two
  - `[Emulator]` block required to have `MaxPlayers=28`
  - From this patch and onwards, some of the sound data tables were reformatted and Warsmash does not yet fully read the new table formats and file locations. Probably only music and combat attack sounds work -- the unit voices were changed to accomodate the Reforged lip sync and are not loading on Warsmash yet at the time of writing, even if you play the "Classic" graphics and avoid loading the `_hd.w3mod` mods listed above
  - From this patch and onwards, all texture assets are in the DDS format instead of BLP and this introduces an sRGB calculation that was necessary on BLP but no longer necessary for DDS. Historically there were cases where the BLP sRGB fix was enabled in Warsmash even for the DDS textures which could incorrectly cause some textures to be extremely visually dark (on both "Reforged" or "Classic" graphics). This bug is probably fixed now but is worth keeping an eye out for. Some fans of the game have shown that the DDS compression artifacts in "Classic" do actually look different and perhaps worse than their historical BLP counterparts. So, hero glow for example shows blocky squares instead of a clean gradient glow, and there is not anything Warsmash can do about that other than if I encourage you to use an older version of Warcraft III assets
  - From this patch and onwards, all audio file assets are in the FLAC format instead of WAV. The underlying LibGDX game engine that I use for audio does not natively support FLAC and so Warsmash's attempts to translate them on the fly to WAV in memory while parsing. This translation layer uses an open source FLAC parser found online (cited in https://github.com/Retera/WarsmashModEngine/blob/main/desktop/src/io/nayuki/flac/README.md). Notably this FLAC parser drops some bytes of data and has a loss of precision in order to retrofit the data into the WAV parser from LibGDX. So, the audio may sound 'tinny' or 'worse' in an unintended way that does not match the actual underlying files. The actual audio files might sound better if you played them individually with an external program than they do inside the Warsmash experience. This issue is particularly relevant to the modified version of the audio files included in the `_hd.w3mod` mod ("Reforged")
- **But Not** Warcraft III: Frozen Throne: Patch 1.33 and beyond
  - Maps saved on this patch might still open on a Warsmash configuration that uses Patch 1.32.10 assets, but all the 3D models of Warcraft 3 fundamentally changed format in this patch and onwards. I do not parse the new model format on Warsmash, because I can already have a great time on Warsmash by using an older patch, even if I wanted to tinker with "Reforged" graphics. If you really want this feature, and are tied to the latest patch for some reason, consider contributing to Warsmash.

## How to Build/Run the Code

### From IntelliJ IDE
1. Download IntelliJ
2. Open the repo using "Open from VCS" and enter the URL for Warsmash: https://github.com/Retera/WarsmashModEngine.git
3. Manually modify the contents of `core/assets/warsmash.ini` to locate game assets (i.e. Warcraft 3) on your computer from some other directory
4. Run the Gradle target called `runGame`
5. If you wish to repeatedly play a test map, you can use `runGame -Pargs="-loadfile WorldEditTestMap.w3x -window"` to automatically launch a map named `WorldEditTestMap.w3x` when launching Warsmash from the IDE in windowed mode
6. Additional launcher QoL/performance flags are available when passing `-Pargs`: `-windowed [width height]`, `-fps <value>`, `-vsync` / `-novsync`, `-msaa <samples>`, `-ini <path>`, and `-help` for command documentation.

### From the Eclipse IDE
1. Download Eclipse
2. Go to `Help > Eclipse Marketplace` and locate and install the plugin called `ANTLR 4 IDE`
3. Use `git clone https://github.com/Retera/WarsmashModEngine.git` outside of Eclipse to copy Warsmash to some folder
   1. Often I use `C:/Users/<name>/source/repos/Warsmash/project`
4. Designate an Eclipse workspace that is not the folder created above in Step 3
   1. Often I use `C:/Users/<name>/source/repos/Warsmash/workspace`
5. Now inside Eclipse with the ANTLR plugin installed, use `File > Import` and choose `Gradle > Existing Gradle Project`. 
6. Navigate slowly through the menus, using the `Next` button and not `Finish`.
   1. If one of the steps gives you a crash with a Java stacktrace instead of a list of project components, this means that your Eclipse was run while using your System's installation of Java (when running Gradle) even though Eclipse uses the version of Java specified in the preferences menus for everything else.
   2. To work around the above issue, Download the Eclipse Temurin JDK 17 and install it on the system then restart Eclipse IDE and repeat the Gradle import
7. We must always manually change some settings in ANTLR preferences that reset whenever Eclipse closes and opens, because the ANTLR plugin is not well-maintained.
   1. Choose `Window > Preferences` then `ANTLR 4 > Tools`. Click to expand `Options`.
   2. Uncheck `Generate a parse tree listener (-listener)`
   3. Check `Generate parse tree visitors (-visitor)`
   4. Modify the `Directory` setting to be `./build/generated-src`
   5. If you have silent failures when running Warsmash, download the JAR file of ANTLR 4.7 instead of 4.4 default and point the ANTLR JAR setting to the download, although usually I do not have to do this.
   6. Press OK and exit this menu.
8. Select all of the projects (`warsmas-core`, `warsmash-desktop`, `warsmash-fdfparser`, `warsmash-jassparser`, `warsmash-server`, `warsmash-shared`) and go to `Right click > Gradle > Refresh Gradle Project`.
9. Specifically enter into the `warsmash-fdfparser/antlr-src` folder and add and remove a space in the file there, then save. This should trigger the ANTLR plugin to regenerate the generated code.
10. Repeat step 9 for the folder `warsmash-jassparser/antlr-src`. You might also need to delete the contents of the `build/generated-src` folders in these projects if those folders generated some files before you edited the Preferences to specify `visitor` instead of `listener`.
11. Once everything above is resolved and the individual Java projects no longer have red error icons next to them, press `CTRL+SHIFT+T` and type `DesktopLauncher` to open the `DesktopLauncher.java` file. Right click on `main`the method name, and choose `Run as > Java application`.
12. Now go to the dropdown next to the green play button, choose `Run configurations` and in the popup `DesktopLauncher` should be the selected tree item on the left, then navigate to the tab labelled `Arguments`.
13. On the `Arguments` tab, click on the `Workspace` button near the bottom that will assign the `Working directory`. Choose the `warsmash-desktop/assets` directory and push `OK` then `Close`.
14. Manually modify the contents of `core/assets/warsmash.ini` to locate game assets (i.e. Warcraft 3) on your computer from some other directory
    1. An out of date guide for how to do that can be found here: https://www.hiveworkshop.com/threads/overview-and-setup.331776/
    2. For Patch 1.22 (the last 2008 patch before the Activision merger) you can simply `CTRL+R` to do a `Find and Replace` on the example absolute path to Warcraft III to replace it with something else from your PC.
    3. For Patch 1.29 (the last 2018 patch before the Reforged announcement, when the game was still moddable) you can download a binary build of Warsmash from Hive Workshop and its sample config `warsmash.ini` will be intended for `Warcraft III: Patch 1.29.2` because that is the patch favored by the Hive developers building their own spinoff Warcraft III version to replace Reforged or whatever
    4. For Patch 1.32 (the last 2022 Reforged patch before they got PlaySide studios involved and starting pushing untested code into production and making Reforged even worse or whatever) you can use a combination of the `[Emulator]` config from the `1.29` build and the `[DataSources]` config from the out-of-date `core/assets/warsmashRF.ini` and then play a version of Warsmash that can emulate the Reforged graphics toggle based on which `w3mod` prefixes you specify

### From GNU/Linux Command Line
1. `git clone https://github.com/Retera/WarsmashModEngine.git`
2. If you are using `ubuntu` package managers and maybe also some others, the `sudo apt-get install` command to get OpenJDK 17 will install something poisoned with weird binaries that cause a crash for a reason unknown to me.
3. To workaround the OpenJDK 17 bug(s) for the time being, I typically download the Eclipse Temurin JDK 17 from their site. This can be downloaded as a ZIP and then unzipped to a folder.
4. `cd WarsmashModEngine` (wherever you cloned it)
5. `vim ./core/assets/warsmash.ini` (or any other of your favorite text editors) and update the file in the same way as described above for any other IDE, to locate your War3 install. You must use forward slash `'/'` instead of backward slash `'\'` for file paths on linux  in the INI file.
6. `JAVA_HOME=~/Downloads/temurinJDK17/bin ./gradlew desktop:runGame` should launch the game and play, given a Temurin folder unzipped to `~/Downloads/temurinJDK17`

## How to Build Release Binary Version
1. Clone the repo
2. Run ./gradlew desktop:runtime
3. Find "./desktop/build/image"
4. Inside the image folder, add any necessary game assets (i.e. Warcraft 3) and a relevant `warsmash.ini` file that describes where to find them
5. From inside the "image" folder, run `./bin/warsmash.bat` on Windows (or `./bin/warsmash.sh` on other systems)
6. Optionally use my other project, "Warsmsh Windows Wrapper", to make an EXE on Windows to kick off the .bat: https://github.com/Retera/WarsmashWindowsWrapper/tree/experimental

## Background and History
My current codebase is running on Java 17 (but only using Java 8 syntax) and the LibGDX game engine coupled with the port of the mdx-m3-viewer's engine. It contains:
- A transcription of only the relevant portions of this WebGL viewer necessary to load MDX models and W3X map data, and to display the models https://github.com/flowtsohg/mdx-m3-viewer
  - I changed several things about the viewer as of the time of writing, such as a naive implementation of Light objects that will support the Day/Night cycle of the game as well as support point lights for torches and other game objects. The memory leak that originally existed in the light system has been fixed as of Phase B.
  - After the development of this project began, there was code sharing between the MDLX parser in this repo and the one in the "conflictfixes" branch of ReteraModelStudio. Other users (tw1lac and flowtsohg) made contributions to the MDLX parser while it was sitting in that repo, and then I copied it back to this repo, so at this point this repo is including a few changes that were made by those users, but in an undocumented way. Hopefully I will clean this up and get the MDLX parser split from both of these projects off to its own repo. However, credits and thanks to them for their changes to that code (I think it was mostly flowtsohg)! My lack of the git history for those changes in this repo is not an attempt to remove citation/credits for their work, but is rather the result of laziness.
- A transcription of the HiveWE terrain rendering systems from this project, then modified to interface nicely with the ported viewers MDX rendering https://github.com/stijnherfst/HiveWE
  - I changed the orientation of the cliff meshes. Mine are rotated 90 degrees from HiveWE in order to more accurately reproduce the likeness of what we observe in the Warcraft III World Editor. At times, I also tinkered with the Ramp logic and at this point mine is not 1:1 the same as HiveWE, but there are still many issues with mine.
- The shaders GLSL code from both of the above projects, originally copied in almost their exact form but with a few necessary changes. As of Phase B all active shaders use `#version 330 core`, matching the OpenGL 3.3 requirement set at startup.
- Graphical Enhancements to viewer from this fork of it: https://github.com/d07RiV/wc3data
(By copying the above repo, I was able to add support for water waves, shadow on the edge of the map, shadow under buildings, UberSplat under buildings, the little shadow picture under units, and some earlier drafts of the unit selection mouse intersect code that I mostly replaced at this point. I had to heavily modify the Splat logic to make them mobile as this repo at the time of copying was only able to create the shadow in a static location and upload the single location to the graphics card. Theoretically I have found cases for Ramps in map terrain that are handled properly by this repo and not by HiveWE terrain rendering nor by my copy of it, but I never prioritized investigating those ramp problems further)
- I use Java-based blp-iio-plugin so I didnt need to bother porting the BLP parser from the model viewer BUT this one I am using has some issue on campaign art textures where the alpha isnt loaded, so NightElfCampaign3D_exp for example has render artifacts around palm leaves https://github.com/DrSuperGood/blp-iio-plugin
- I use an MPQ parser by the same guy as the above, DrSuperGood, but he didnt put it on github so I just included the sourcecode in the repo for now
- For loading Unit Data, I have two SLK parsers and two INI parsers which is gross. One set is copied from ReteraModelStudio and the other is transcribed from viewer. I am mostly using the ReteraModelStudio one where possible because I wrote it not as a copy of anything else from my own intuition years ago and the API interfaces better with my high level unit data API that I copied from ReteraModelStudio (https://github.com/Retera/ReterasModelStudio). Phase D will unify these.
- For loading map changeset unit data (Object Editor) I am using a transcription of the parsers written by PitzerMike for Grimoire/Widgetizer. Somebody linked me some ancient pastebin on the Hive at some point that included this C++ code so I wrote a Java port in my model editor repo and copied it to this repo and cleaned it up a bit
- For loading TGA image files such as building pathing, I use the same TGA parser included in ReteraModelStudio codebase. It was written by OgerLord long ago https://github.com/OgerLord/WcDataLibrary/blob/master/src/de/wc3data/image/TgaFile.java

## Legal Stuff
_NOTE: The following is not legal advice and is only back-of-the-hand speculation. In addition, Warsmash may contain repackaged code from other projects where indicated, and these other projects may be subject to the terms of other license agreements such as the GPL. The licenses for those projects should be clearly indicated when you review their code._

Earlier versions of Warsmash included a footnote suggesting that the official Warcraft 3 game might some day be able to copy components from Warsmash as a means to improve itself, because Warsmash was MIT licensed. It was brought to my attention that at least one of the dependencies of Warsmash was GPL licensed and more specifically that the exact terms of the GPL suggest that any project (i.e. Warsmash) that uses GPL code as a dependency must itself be GPL licensed in order to comply with the GPL terms. As such, had Activision actually copied code from Warsmash and placed that code into Reforged, they would have been at risk of legally creating a situation that required the whole of Reforged itself to become free software, perhaps, because Warsmash may have this obligation to be free software likewise despite an incorrect documentation/understanding of the matter in previous versions.

To eliminate any confusion, the license for Warsmash has been changed to the AGPL. See the license file for details.
