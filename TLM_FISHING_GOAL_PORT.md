# TLM Fishing Goal port

## Source mapping

TLM 1.21.1 does not contain a class literally named `FishingGoal`. Its fishing
AI is split between:

- `TaskFishing`
- `MaidRideFindWaterTask`
- `DefaultFishingType` / `IFishingType`

This project keeps the classic Forge `GoalSelector` architecture and ports the
behavior into `net.mac.projectmod.goal.FishingGoal`.

## Ported behavior

- GoalSelector-based fishing AI (`MOVE` + `LOOK` flags)
- vanilla fishing-rod requirement
- TLM-style search radius: 6 blocks horizontally, 3 vertically
- TLM ring-style destination search
- restriction/home-area check
- navigation to a nearby dry shore block
- TLM-style check delay (100 ticks) between fishing cycles
- vanilla fishing bobber throw sound
- `ProjectFishingHook` used as the project's Forge hook implementation
- existing `hold_mainhand:fishing` animation state remains driven by
  `ChibiEntity.hasFishingHook()`

## Deliberate project-specific differences

- TLM 1.21.1 uses Brain/Behavior tasks; Chibi uses `GoalSelector` as requested.
- TLM 1.21.1 can register additional fishing types/compatibility. This port is
  intentionally vanilla-focused and equips only `Items.FISHING_ROD` from the
  Chibi inventory.
- `ProjectFishingHook` is retained because it already owns vanilla fishing loot,
  bobbing, bite timing, inventory insertion, and the client fishing-line renderer.
