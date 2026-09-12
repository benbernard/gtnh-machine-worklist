# Restore bulk crafting with returning tools

## Report and root causes

The user reported visible one-at-a-time crafting after beta.6 and suspected the tool-return wait. The main Prism profile was still running beta.6. Computer use remains paused.

The 64-batch NEI transfer path had not been removed. Two policies combined to make tool recipes slow:

1. Readiness treated every positive-factor ingredient as consumed on every run. Crafting tools such as files and screwdrivers can have a factor of one and return through `Item.getContainerItem`. One physical tool therefore capped readiness at one batch even when NEI could reuse it. The installed NEI 2.8.44 `DefaultOverlayHandler.calculateRecipeQuantity` explicitly exempts non-stackable container items from its ingredient quantity limit.
2. Beta.6 waited for a latency-dependent quiet period after every tool transfer, even with a clear cursor/grid. It then deferred the next transfer to another client tick. The delay applied once per one-batch transfer in the affected case.

## Changes

`CraftingToolUses` simulates at most 64 container returns on detached copies, with NEI's damage-sound suppression restored afterward. Non-stackable ingredients that return as the same item/tool type can be reused. Durability breakage limits the transfer; an ingredient that immediately becomes another item, such as a filled container becoming an empty bucket, remains consumed.

Readiness retains physical slot allocation, so a recipe needing two tools cannot use a single tool in both positions. It conservatively uses the shortest budget among all matching tool choices, because NEI can select any of them. Mixed reusable/consumed alternatives retain the original physical-count calculation. Tool budgets are cached per inventory slot within a calculation. Positive-factor returned-tool previews are capped at 64, matching the maximum submitted NEI transfer; subsequent transfers recalculate actual stock and damage. Machine and zero-factor reusable inputs retain their existing accounting, and source bookmark factors/quantities stay unchanged.

A clean cursor/grid on the next client tick completes the tool observation without a blanket inventory-quiet delay. If output verification succeeds, the same tick can start the next bounded crafting burst. Actual leftovers still use latency-aware quiet checks, at most two recognized-tool recoveries and a fixed deadline. Closing/changing the container, death or disconnect still stops transfers. Client observations are not server acknowledgments.

In-game help explains tool reuse, durability, the per-transfer preview bound, nearly broken spares and leftover recovery. Recipe details label recognized returned tools as reusable.

## Validation

The initial local build passed all 79 tests. Three new Forge/NEI fixtures cover one physical returning tool supporting a 64-batch chain transfer, unchanged inventory/damage/source factors, recalculation after a completed batch, durability limits, simultaneous tool slots, and consumed containers. Existing settlement tests now assert prompt clean-grid continuation at every latency while retaining late blocked-update, recovery, unknown-item and deadline checks.

The final build after the help update also passed all 79 tests. Compare the release JAR to the tested candidate allowing only version metadata; verify all three platform CI reports and artifact hashes before publishing. Evidence belongs in ignored `build/releases/beta8/`.

No gameplay or runtime speed measurement was performed. The new tests establish batch selection and wait-policy behavior, not live multiplayer/backpack throughput. Install the new release in closed profiles and stage it disabled for the running main profile.
