# IaFTFC

IaFTFC integrates TerraFirmaCraft with Ice and Fire: Community Edition on
Minecraft 1.21.1, NeoForge 21.1.265, and Java 21.

## Required mods

- TerraFirmaCraft 4.2.12
- Ice and Fire: Community Edition 2.1.4
- Patchouli 1.21.1-93-NEOFORGE
- Uranus 2.4.1-bugfix for Minecraft 1.21.1 and NeoForge
- Jupiter 2.3.7 for Minecraft 1.21.1 and NeoForge

Uranus already contains Architectury 13.0.8, and Ice and Fire CE already
contains Integration 0.2. Do not install duplicate copies of those embedded
libraries.

Use the same versions on clients and dedicated servers. A TFC world is the
supported world type. Verify the actual loaded IDs and versions with
`/neoforge mods`; filenames alone are not evidence that a mod loaded.

## Development

Dependency artifacts are resolved from their published Maven artifacts and are
not included in the published mod JAR. Run `./gradlew compileJava` for an
intermediate check and `./gradlew build` for the completed build.
