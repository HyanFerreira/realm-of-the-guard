# Realm of the Guard

**Realm of the Guard** is a medieval progression and village-governance expansion for Minecraft Java Edition.

The project is designed around a simple fantasy: begin as an outsider, earn the trust of vanilla villagers, protect and organize their settlement, become its lord, unite multiple villages and eventually be crowned king.

Unlike colony simulators built around fixed blueprints and autonomous builders, Realm of the Guard preserves Minecraft's creative building loop. Players construct freely and register their own buildings through the **Domain Ledger**. The mod identifies each building by its bounds, required blocks and place in the settlement.

## Project status

Realm of the Guard is in pre-alpha foundation work. Version `0.1.0` now includes a developer-playable settlement slice: the Domain Ledger can register bell-anchored settlements, inspect nearby village infrastructure and persist records across restarts.

Target platform:

- Minecraft `1.21.11`
- Fabric Loader `0.19.3+`
- Java `21`
- Fabric API
- Yarn mappings

Planned ecosystem:

- [Guard Villagers](https://www.curseforge.com/minecraft/mc-mods/guard-villagers-fabric) provides guard entities and their native combat AI.
- [Rally of the Guard](https://github.com/HyanFerreira/rally-of-the-guard) provides guard ownership and tactical commands.
- Realm of the Guard provides settlements, buildings, legitimacy, government, economy, military hierarchy and royal progression.

Rally of the Guard and Guard Villagers are required dependencies. During workspace development, Gradle consumes the sibling `rally-of-the-guard` checkout as a composite build, so Realm always compiles against the current public Rally API.

## Design pillars

- **Build freely:** the player designs every building instead of selecting a fixed schematic.
- **Earn authority:** crowns symbolize legitimacy; they do not grant it by themselves.
- **Govern real villages:** vanilla villagers, professions, beds, workstations and bells remain relevant.
- **Command an organized army:** ranks, squads, captains and royal guards expand the tactical foundation of Rally of the Guard.
- **Prefer meaningful choices:** taxes, recruitment and decrees provide benefits with visible costs.
- **Grow in layers:** protector, lord and king are distinct stages with increasingly broad responsibilities.

## Documentation

- [Product and development roadmap](docs/ROADMAP.md)
- [Architecture and inter-mod integration](docs/ARCHITECTURE.md)
- [Foundation playtest guide](docs/PLAYTEST.md)

## Building

```shell
./gradlew build
```

On Windows:

```powershell
.\gradlew.bat build
```

Generated artifacts are placed in `build/libs`.

Development builds expect `realm-of-the-guard` and `rally-of-the-guard` to be sibling directories. Rally is substituted into Realm through Gradle's composite-build support using the stable coordinate `net.hfstack.rallyguard:rally-of-the-guard`.

## License

Realm of the Guard is available under the [CC0-1.0](LICENSE) license.
