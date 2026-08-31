# TLM 1.21.1 -> Forge 1.21 port status

This project contains the **first phase** of a port of TouhouLittleMaid's 1.21.1 NeoForge animation/model core into the supplied Forge 1.21 project.

## Included in this phase

- TLM custom `geckolib3` animation/model core vendored into the project.
- TLM Molang core and the minimal physics support used by the animation stack.
- TLM-style animation priority/state handling for the Chibi entity.
- TLM-style animatable wrapper (`GeckoChibiEntity`) and replaced-entity renderer.
- TLM-style loading of the existing Chibi `guga.geo.json` and `guga.animation.json` resources.
- Forge substitutions for the NeoForge-only renderer event integration used by the TLM renderer.
- Attribution/license files for the vendored TLM code.

## Deliberately not included yet

- TLM held-item layer (`GeckoLayerMaidHeld` and related locator/item rendering).
- Special-item integrations (SlashBlade, CarryOn, etc.).
- TLM maid-specific gameplay systems, AI, inventory, model-pack systems, and compatibility modules.

The next phase is intended to port only the TLM held-item/locator transform path for **vanilla Minecraft items**. Special-item renderers are intentionally out of scope.

## Build verification

A full Gradle compile could not be executed in this environment because the required Gradle distribution/dependencies are not cached and outbound network access is unavailable. The port was checked statically for remaining NeoForge imports in the vendored core; no `net.neoforged` imports remain there.

Before merging into a production branch, run the project's normal Forge 1.21 Gradle build locally and fix any mappings/API errors reported by the actual ForgeGradle toolchain.
