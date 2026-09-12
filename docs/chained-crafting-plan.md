# Crafting selected recipe chains

## Request and scope

Extend the NEI integration beyond repeating one recipe. A recipe detail's **Craft chain (G)** should make its selected crafting dependencies and then its remaining target. The queue's **Craft group (G)** should advance every selected crafting recipe in the group. **Craft 1 batch (F)** retains its exact single-recipe behavior. Recipe choices remain the user's saved NEI choices; machine operations remain manual.

## Findings

The pinned NEI 2.8.44-GTNH `AutoCraftingManager` uses `RecipeChainIterator` to recalculate outstanding recipe work from inventory, transfer ready recipes in chunks up to 64, refresh inventory, and repeat while progress is possible. Worklist already uses NEI's `RecipeChainMath` for dependencies and inventory accounting, but its session fixes one recipe ID. Calling NEI's manager directly would lose Worklist's backpack slot boundaries, output checks and client-tick cancellation behavior.

## Implementation

1. Create a detached request from the selected recipe's existing dependency links, or the whole group. Preserve recorded stock and exclude unrelated branches from a recipe request.
2. Recalculate the request after every transfer. Choose an available crafting step, allowing its new outputs to unlock downstream recipes. Prefer consumers before refilling intermediates to conserve inventory space.
3. Retain the 64-batch transfer limit, bounded client-tick bursts, container/player/input/output guards, and verified output accounting. Freeze per-recipe execution ceilings at request start so cycles or changing stock cannot create endless work.
4. Report completed batches across recipes and explain machine, input, grid or capacity blockers when further progress is impossible. Never report a partially completed chain as complete.
5. Update buttons, keyboard actions and in-game help with the new scope and a preview of the next eligible recipe.

## Validation

Exercise actual pinned NEI calculations with multi-stage chains, shared branches, owned intermediates, partially ready chains, selected-branch isolation, manual stock, machine boundaries and bounded cycles. Keep existing burst, inventory capacity and backpack-boundary checks. Build the mod and test an actual multi-stage chain through the in-game action in the disposable test instance. Record the exact validation scope before publishing the next beta.

## Results — September 11, 2026 (September 12 UTC)

All **47 automated checks**, Checkstyle and packaging passed. Nine new tests exercise the actual pinned NEI chain math and the Worklist scheduler with deterministic recipe metadata and simulated physical transfers; they do not simulate the live NEI overlay itself. They cover three-stage completion, shared branches and batch surplus, selected-branch isolation, partially owned output batches, reduced downstream demand, manual stock without physical inputs, machine boundaries/resumption, a blocked branch and finite execution when finished outputs disappear.

The candidate JAR was 81,509 bytes, SHA-256 `f46029db735f0aee883c008eaf56f4b19fb31a82d47414d3732c24d453d231b2`, built from `0f2bea0` plus this implementation. Actual keyboard actions in the disposable GTNH 2.8.4 **Crafting Helper Test / New World** exercised the pack's log → plank → stick recipes through the player grid:

| Action | Observed result |
|---|---|
| Select the 64-stick target with logs but no planks | F unavailable; G enabled and previewed the upstream plank recipe. G completed 32 plank batches plus 32 stick batches. |
| Craft group toward 96 sticks, from the Machines tab | Existing 64 sticks reduced demand. G completed 16 plank batches plus 16 stick batches, leaving 16 logs. |
| Craft group toward 200 sticks with only those 16 logs | Completed 32 batches across both recipes, reaching 128 sticks, then paused with explicit missing-plank/log explanations. |
| Supply 36 more logs, then press F on planks | Exactly one batch completed, leaving 35 logs and two planks. |
| Resume Craft group | Used the two existing planks, then completed 35 plank batches and 36 stick batches. Result: 71 batches across two recipes, chain complete, no remaining work. |

After the test world saved, **both saved inventory locations contained exactly 200 sticks, zero logs and zero planks**, matching 100 supplied logs and 200 recipe executions across the five requests. Unrelated inventory identities, NBT and counts matched their original backups. Original inventory and bookmark bytes were restored. No timing or transfer instrumentation was used. Raw fixture scripts, backups and inventory evidence remain in ignored `build/benchmarks/2026-09-12-chain/`.

Published evidence: [selected-chain preview](evidence/chain-selected-preview.png), [selected-chain completion](evidence/chain-selected-complete.png), [group completion](evidence/chain-group-complete.png), [partial-progress result](evidence/chain-paused.png), [one-batch result](evidence/chain-one-batch.png), [resumed completion](evidence/chain-resumed-complete.png), [final inventory](evidence/chain-final-inventory.png), and [saved-count verification](evidence/chain-verified-counts.json).

Remaining live coverage: chains in Adventure Backpack or other 3×3 containers, returned tools, cancellation mid-chain, multiplayer and native Linux/macOS gameplay. Existing backpack boundaries, output capacity and burst-yield tests passed. Windows input automation still did not reliably deliver extended arrow keys or pointer movement; wheel selection and ordinary keyboard shortcuts worked. Those automation limitations are not counted as additional gameplay passes.
