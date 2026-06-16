# Modular Drones

A Minecraft mod for Fabric and NeoForge, supporting 1.21.1, 1.21.11, and 26.1.2. Build your own personal drone out of regular blocks, then carry it around as a small item. Equip it, and it'll hover behind you, helping out based on the blocks it's made of — fighting off enemies, mining alongside you, picking up loot, lighting your way, and more.

**[Documentation & Getting Started →](https://kesslercascade.github.io/modular-drones/)**

## About

Modular Drones is a fork of rearth's [Buddy Drones](https://github.com/rearth/Buddy-Drones), originally created for the CurseForge "The Future" modjam. This project has since taken on its own direction with new features, fixes, and improvements beyond the original.

## Building

Requires Java 21 (Java 25 for the 21.6.2 branch). Build with the Gradle wrapper from the repo root:

```
./gradlew build
```

Platform-specific builds:

```
./gradlew :fabric:build
./gradlew :neoforge:build
```

To launch a dev client:

```
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

## License

[CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/)
