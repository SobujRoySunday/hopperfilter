# Contributing to Hopper Filter Setup

Thanks for helping out! Bug reports, feature ideas and pull requests are all welcome.

## Reporting bugs and suggesting features

Open an [issue](https://github.com/SobujRoySunday/hopperfilter/issues/new/choose) and pick the matching template.
For bugs, please include your Minecraft, Fabric Loader, Fabric API and mod versions, plus
`logs/latest.log` if anything crashed.

## Development setup

1. Install **JDK 25**.
2. Fork and clone the repository.
3. Open the folder in IntelliJ IDEA (recommended) or VS Code with the Java extensions. Gradle sets up the rest.
4. Run `./gradlew runClient` to start a development client with the mod loaded.

The source code lives in `src/main/java/com/hopperfilter/`:

| File | What it does |
|---|---|
| `HopperFilterMod.java` | Mod entry point; registers the right-click handler. |
| `HopperInteraction.java` | Handles hopper clicks: saving layouts, collecting items, filling the hopper. |
| `FilterConfig.java` | Loads, live-reloads and saves `config/hopperfilter.json`. |

## Building for different Minecraft versions

Each supported Minecraft line has a file in `versions/`:

```properties
# versions/26.3.properties
minecraft_version=26.3
minecraft_dependency=~26.3
fabric_api_version=0.166.2+26.3
```

- `./gradlew build -Pmc=26.2` builds against a specific line. Without `-Pmc`, the default in `gradle.properties` is used.
- **To add a new Minecraft version**, add a `versions/<line>.properties` file. Look up the Fabric API version
  on [fabricmc.net/develop](https://fabricmc.net/develop). CI picks the new file up automatically.
- Before opening a pull request, make sure the build passes for every line (CI checks this too).

## Pull requests

- Keep each pull request focused on one change, and describe what it does and how you tested it in game.
- Follow the existing code style: tabs for indentation and short comments that explain *why*. `.editorconfig` handles the basics.
- Add a line under `## [Unreleased]` in `CHANGELOG.md` for anything players will notice.

## Releasing (maintainers)

1. Update `mod_version` in `gradle.properties`.
2. In `CHANGELOG.md`, rename `## [Unreleased]` to the new version and add a fresh `## [Unreleased]` above it.
3. Commit, then tag and push:

   ```sh
   git tag v1.1.0
   git push origin main --tags
   ```

GitHub Actions builds a jar for every Minecraft line in `versions/` and publishes them as a GitHub Release.
The tag must match `mod_version`, or the release step fails.
