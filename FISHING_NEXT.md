# Fishing follow-up / TLM port

This revision keeps the fishing feature on the classic Forge GoalSelector path while matching the important TLM 1.21.1 behavior:

- FishingGoal searches for water using the TLM traversal order.
- Shore candidates are the AIR block above a solid shoreline block, not the shoreline block itself.
- FishingGoal must reach a valid shore position before casting.
- The hook packet carries the Chibi owner id to the client, matching TLM's MaidFishingHook packet behavior.
- Client-side hook movement disables vanilla lerpTo, matching TLM.
- The TLM animation wrapper now force-refreshes when fishing/swing/use state changes.
- `hold_mainhand:fishing` is played after the cast swing while the hook is alive.
- `swing_hand` is played by the dedicated swing controller during cast/retrieve.
- The hook renderer draws the bobber and fishing line using the TLM renderer path.

Only vanilla Fishing Rod behavior is included. TLM special-item compatibility is intentionally excluded.
