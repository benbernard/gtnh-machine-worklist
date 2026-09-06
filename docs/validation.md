# Validation record

## Automated checks — 2026-09-05

GitHub Actions [run 34005305238](https://github.com/benbernard/gtnh-machine-worklist/actions/runs/34005305238), commit `bf334432be147ce9299072aac29777b88e6f13e4`: downloaded reports independently confirm **28 tests, zero failures and zero skips** on Windows, Ubuntu and macOS. All three production JARs have SHA-256 `bef0a753df3847d8d0b9898b5b94a3bd93e91d03be7ec3cf3cce85e1cf4d3771`.

The tests cover finite global stock allocation, overlapping ingredient alternatives, owned intermediate/finished items, shared branches and batch surplus, multiple outputs, fluid containers and mB, reusable molds/circuits with configuration, cycles, repeated calculations and an 80-stage chain. Integration fixtures run the pinned NEI 2.8.44-GTNH calculator in a Forge classloader. Manual-completion cases cover exact quantities, upstream reduction, inventory overlap, rounding, clearing and persisted reload. Two backpack layout tests cover its 48 storage slots and nine crafting positions; they do not execute the live backpack container or NEI transfer.

A local Windows/JDK 25 `spotlessApply build` also passed all 28 tests after the tooltip draw-order and completion-editor background fixes. These rendering fixes require a visual recheck. `git diff --check` passed.

## Live GTNH 2.8.4 checks

All checks use the separate Prism test instance and disposable **New World**. No production world or multiplayer inventory was modified.

- Startup with the mod and F10 import of NEI group 16 passed.
- With no materials, the Steam Oven chain showed 16 assembler, 164 steel bending and 32 wrought-iron bending runs.
- With CI build `8b60fda`, adding 64 steel plates reduced steel bending to 100. Adding 64 steel ingots left readiness at zero until a configuration-1 programmed circuit was present; then exactly 64 steel bending runs became ready and sorted first.
- Recipe details showed full batch inputs and the reusable circuit. Open NEI recipe reached the bending recipe; Escape returned to the worklist.
- Client-command opening passed after opening inventory once to load bookmarks.
- With CI build `316542f`, entering exactly 8 completed Steam Ovens and saving with Enter reduced the queue to 8 assembler, 82 steel bending and 16 wrought-iron bending runs. Original F2 screenshots are on the [website](https://machineworklist.crabanddog.com/#screenshots).
- With CI build `0ec6d40`, the saved 8-oven completion survived restart. Machines showed 3 steps, Crafting 13 and All 16. The Crafting view included 8 Steam Ovens in 2 batches.
- Half remaining changed the completed total from 8 to 12, then to 16, rounding the final half to a whole batch. The missing pane became empty. Clear this entry reset completion to zero and restored the outstanding external inputs.
- The missing-input pane showed reusable circuits separately by configuration and scrolled independently. Ready only produced an empty queue while the overall missing-input pane stayed populated.

## Issues found during verification

- Missing-item tooltips were drawn before subsequent rows, allowing rows to cover them. Moved tooltip rendering after the pane and buttons.
- World waypoint labels obscured the completion editor. Applied the worklist's solid dark background and render-state setup to the editor.
- The initial hand-authored crafting-table test fixture used the vanilla four-plank recipe. GTNH's actual shaped recipe uses two flint and two logs. The invalid fixture produced Recipe unavailable; this is not evidence of a working crafting transfer. A corrected fixture is being tested.
- Native automation sometimes moves the displayed pointer without updating Minecraft's internal mouse position. Drag gestures delivered movement successfully. Unchanged screens after simple clicks are not counted as passing checks.

- Opening through `/machineworklist` constructed an uninitialized inventory GUI, while NEI ingredient checks require its Minecraft reference. Initialized the parent before creating the worklist; the local build and all 28 tests pass. Live transfer verification remains outstanding.
- A command-given Adventure Backpack without NBT crashed its own `InventoryBackpack.saveToNBT` during an ordinary inventory click, before opening the worklist. The disposable test fixture was backed up and initialized with backpack NBT. This crash does not establish either success or failure of worklist transfer compatibility.

## Outstanding acceptance checks

- Complete a real one-batch NEI transfer from both player/crafting-table and Adventure Backpack grids, checking before/after quantities and container return.
- Verify backpack storage counts once and occupied grid/cursor/full inventory guards in game.
- Recheck the tooltip and completion-editor display fixes visually, including a small window.
- Exercise death handling while the worklist is open on the latest build.
- Validate larger actual GTNH groups, fluids, chance outputs and unavailable handlers beyond synthetic fixtures.
- Connect to the authorized EMBU server with no server counterpart and check normal inventory/worklist behavior.
- Rendered gameplay has only been exercised on Windows. Linux/macOS CI verifies compilation, packaging and automated calculations, not native gameplay.

The website download remains `316542f`, SHA-256 `6532b16ee4c2dc2426c9a4727c00e9e79f132beceaa5ce64d97d896f6ffed5f1`. New crafting/backpack features are source-build work until their live checks are complete. Compilation and synthetic integration tests do not establish full gameplay compatibility.
