# Virtual circuit settings and machine queue order

## Requested behavior

- Reusable Programmed / Programmable Circuits represent a virtual machine setting. They must never appear as missing or prevent readiness.
- Show ready machine operations before waiting ones. Within each group, show the largest remaining run count first, including partially ready operations.
- Keep the configuration visible and explain it in game. Preserve the chosen NEI recipes, target quantities, saved progress, navigation and crafting-chain execution.

## Analysis and implementation

The installed GTNH 2.8.4 GregTech `5.09.51.482` JAR contains `gregtech.common.items.ItemIntegratedCircuit`. Its constructor supplies `integrated_circuit` to `GTGenericItem`, which registers `gt.integrated_circuit` under GregTech's namespace. `GhostCircuitItemStackHandler` represents configuration choices using that item. This was inspected locally without opening or controlling Minecraft.

`VirtualInputs` recognizes only zero-factor ingredient nodes with the exact registry identity `gregtech:gt.integrated_circuit`. These nodes stay in the captured plan and recipe details, preserving configuration, recipe identity and saved progress. Readiness and missing-input explanations skip them. Consumed circuits, reusable tools/molds and similarly named items from other mods still require physical inventory. NEI already excludes zero-factor ingredients from dependency demand, so its chosen recipe graph does not need to change.

`WorklistOrder` sorts a display copy by ready status, then remaining runs descending, then machine name; exact ties retain their prior order. In All, it replaces only machine rows, retaining crafting rows in their existing positions. It runs after tab/ready filtering on every refresh. `WorklistPlan.calculate` and crafting-chain scheduling retain their previous order. Remaining runs reflect visible inventory and recorded available outputs, rather than original demand, output count or currently ready runs.

Recipe details label the circuit's configuration as virtual and always available, and explain setting the machine's ghost circuit slot. In-game Reading the queue help and the written guide explain both changes. Machine power, tier and actual configuration remain manual checks.

## Validation plan and results

Five new Forge/NEI fixtures cover all configurations 0–24 without a physical circuit; physical materials and reusable tools still missing; consumed circuit metadata and an identical-name item in another namespace; ready/partial/waiting ordering, ready-only filtering, long run counts and source-order preservation; and refreshing after inventory/manual-stock changes while retaining crafting order. Fixtures use the real pinned NEI calculator with isolated registered test items, not a loaded GregTech machine registry.

The first run exposed a fixture registration error: Forge prepended `minecraft:` because no active mod container exists in the isolated loader. The fixture now registers after namespace resolution and asserts the exact resulting identity. This did not change the production classifier.

Local `spotlessApply test build` passed all 76 tests with zero failures, errors or skips. Release verification will compare the tagged release to the tested candidate allowing only version metadata, and verify all three CI artifacts before publication. Record release checks and installation state in ignored `build/releases/beta7/` evidence.

Computer use remains paused at the user's request. No new gameplay, machine operation, backpack transfer or multiplayer result is claimed. The latest previously released beta.6 was enabled in the closed main Prism profile first, as requested; retain older disabled JARs and verify any later profile update against the published checksum.
