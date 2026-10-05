# TLM Held Item Port (Forge 1.21)

This build adds the normal vanilla held-item path from TLM 1.21.1's `GeckoLayerMaidHeld` to the Chibi TLM renderer.

## Included
- TLM locator hierarchy lookup via `AnimatedGeoModel`
- `RenderUtils.prepMatrixForLocator(...)`
- Vanilla `ItemInHandRenderer`
- `ItemDisplayContext.THIRD_PERSON_RIGHT_HAND`
- `ItemDisplayContext.THIRD_PERSON_LEFT_HAND`
- TLM's normal `-0.0625, -0.1` translation and `-90` degree X rotation

## Intentionally excluded
- SlashBlade compatibility
- Carry On compatibility
- Gun compatibility
- Any special-item renderer

The old GeckoLib 4 `BlockAndItemGeoLayer` is not attached to `TlmChibiRenderer`; the active renderer is the TLM-style renderer registered in `ModEventBusEvents`.


## Fishing animation port (2026-09-21)
- Added a synced `ChibiEntity.hasFishingHook()` state.
- `FishingGoal` turns it on when the hook is spawned and clears it when the hook ends/stops.
- `ChibiAnimationManager` now mirrors TLM 1.21 `predicateMainhandHold` behavior with `hold_mainhand:fishing`.
- The Guga animation file now contains TLM's default `hold_mainhand:fishing` pose.
- Retrieval clears the fishing pose before triggering the normal `swing_hand` animation.


## Fishing rod cast-model fix
The vanilla 1.21.1 fishing_rod model has a `cast` override to `fishing_rod_cast`. Chibi bypasses that override for `Items.FISHING_ROD` and renders the base baked model directly, because the custom ProjectFishingHookRenderer owns the actual bobber/line rendering.
