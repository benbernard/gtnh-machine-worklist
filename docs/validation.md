# Validation record

## Automated checks — 2026-09-05

GitHub Actions [run 34006808760](https://github.com/benbernard/gtnh-machine-worklist/actions/runs/34006808760), commit `f5b1e4b79b3169541ddaf3ca6c270a316e4c30c1`: downloaded reports independently confirm **29 tests, zero failures and zero skips** on Windows, Ubuntu and macOS. All three production JARs have SHA-256 `e9f80be678ecf94634ddbc0bff6d937cd1f4669a839a98b9fcd59961d5937e53`.

The tests cover finite global stock allocation, overlapping ingredient alternatives, owned intermediate/finished items, shared branches and batch surplus, multiple outputs, fluid containers and mB, reusable molds/circuits with configuration, cycles, repeated calculations and an 80-stage chain. Integration fixtures run the pinned NEI 2.8.44-GTNH calculator in a Forge classloader. Manual-completion cases cover exact quantities, upstream reduction, inventory overlap, rounding, clearing and persisted reload. Three backpack layout/overlay tests cover its 48 storage slots, nine crafting positions and cleanup/supply boundaries; live transfer evidence is recorded below.

A local Windows/JDK 25 `spotlessApply build` also passed all 29 tests after the backpack cleanup fix. `git diff --check` passed.

## Live GTNH 2.8.4 checks

Checks use the separate Prism test instance, disposable **New World**, and a read-only inventory/worklist check on the authorized **EMBU** server. No production world files or multiplayer inventory items were modified.

- Startup with the mod and F10 import of NEI group 16 passed.
- With no materials, the Steam Oven chain showed 16 assembler, 164 steel bending and 32 wrought-iron bending runs.
- With CI build `8b60fda`, adding 64 steel plates reduced steel bending to 100. Adding 64 steel ingots left readiness at zero until a configuration-1 programmed circuit was present; then exactly 64 steel bending runs became ready and sorted first.
- Recipe details showed full batch inputs and the reusable circuit. Open NEI recipe reached the bending recipe; Escape returned to the worklist.
- Client-command opening passed after opening inventory once to load bookmarks.
- With CI build `316542f`, entering exactly 8 completed Steam Ovens and saving with Enter reduced the queue to 8 assembler, 82 steel bending and 16 wrought-iron bending runs. Original F2 screenshots are on the [website](https://gtnh-mod.crabanddog.com/#screenshots).
- With CI build `0ec6d40`, the saved 8-oven completion survived restart. Machines showed 3 steps, Crafting 13 and All 16. The Crafting view included 8 Steam Ovens in 2 batches.
- Half remaining changed the completed total from 8 to 12, then to 16, rounding the final half to a whole batch. The missing pane became empty. Clear this entry reset completion to zero and restored the outstanding external inputs.
- The missing-input pane showed reusable circuits separately by configuration and scrolled independently. Ready only produced an empty queue while the overall missing-input pane stayed populated.

- With the keyboard-navigation build based on `bf33443` (SHA-256 `7269349d51e8a307d6ecddea309eed5ef198c259b2012eeeff4638f915ee412c`), `/machineworklist 19`, key 2 and Enter opened the resolved crafting-table recipe: 8 runs remaining, 4 ready, starting with 8 flint and 8 logs in player inventory. Pressing F returned to the 2x2 inventory grid and produced exactly one table, leaving 6 flint and 6 logs with an empty grid. [Original F2 evidence](evidence/player-crafting-transfer.png). Backpack storage was closed and not counted.

## Issues found during verification

- Missing-item tooltips were drawn before subsequent rows, allowing rows to cover them. Moved tooltip rendering after the pane and buttons.
- World waypoint labels obscured the completion editor. Applied the worklist's solid dark background and render-state setup to the editor.
- The initial hand-authored crafting-table test fixture used the vanilla four-plank recipe. GTNH's actual shaped recipe uses two flint and two logs. The invalid fixture produced Recipe unavailable; this is not evidence of a working crafting transfer. The corrected fixture also uses NEI column-major shaped-ingredient order and resolves successfully after restarting NEI.
- Native automation sometimes moves the displayed pointer without updating Minecraft's internal mouse position. Drag gestures delivered movement successfully. Unchanged screens after simple clicks are not counted as passing checks.

- Opening through `/machineworklist` constructed an uninitialized inventory GUI, while NEI ingredient checks require its Minecraft reference. Initialized the parent before creating the worklist; the local build and all 28 tests pass. A real player-grid transfer subsequently passed using the keyboard-enabled build.
- A command-given Adventure Backpack without NBT crashed its own `InventoryBackpack.saveToNBT` during an ordinary inventory click, before opening the worklist. The disposable test fixture was backed up and initialized with backpack NBT. This crash does not establish either success or failure of worklist transfer compatibility.

- A real backpack transfer initially produced one output but NEI swept all adjacent storage into player inventory during grid cleanup. Added a backpack-specific NEI overlay that restricts cleanup to the nine visible crafting positions, excludes hidden mirrors and utility slots as supply, and rejects ingredients mapped outside the grid. A Forge-classloader regression test checks these boundaries; the suite now has 29 tests. The corrected live transfer passed as described below.

## Final transfer and multiplayer checks

- The corrected handheld Adventure Backpack transfer used the local build of the `f5b1e4b` source, SHA-256 `5f7d25012fa15b96299f0cade888f9fd058c3e9f6389c15509597992cc5c8307`. Player inventory had two crafting tables and no flint/logs; backpack storage held 8 flint, 8 logs and 10 cobblestone. An eight-table group correctly showed six remaining and four ready, rather than double-counting mirrored storage.
- F10 opened the sole crafting group from the actual backpack GUI. Selecting Crafting, Enter, then F produced one table and returned to the backpack. Storage retained 6 flint, 6 logs and all 10 cobblestone; player tables increased to three and the crafting grid was empty. Reopening showed five remaining, three ready, and four flint/four logs missing. Original F2 captures: [before](evidence/backpack-before.png), [after](evidence/backpack-after.png).
- The completion editor rendered clearly at the small test window size: [original F2 capture](evidence/completion-small-window.png). Its background fix is visually verified. Tooltip layering is improved; exhaustive edge-of-window tooltip positioning is not verified.
- The exact final CI JAR above connected to the saved EMBU server without installing a server counterpart. `/machineworklist 16` imported its group. Machines showed three operations, Crafting twelve and All fifteen against the real inventory; overall missing inputs opened, and Escape returned to the unchanged inventory. No crafting or completion records were submitted on the server. The test client was closed afterward.

## Outstanding acceptance checks

- Complete a real one-batch transfer of a recipe requiring all three crafting columns. The tested table recipe fits 2x2, including when transferred through the backpack's 3x3 area.
- Verify occupied grid/cursor/full inventory guards in game, and worn/placed backpack variants. Handheld backpack storage accounting and cleanup passed.
- Check tooltip placement at window edges and broader screen sizes.
- Exercise death handling while the worklist is open on the latest build.
- Validate larger actual GTNH groups, fluids, chance outputs and unavailable handlers beyond synthetic fixtures.
- Verify a crafting transfer and completion persistence on multiplayer; the EMBU connection and read-only worklist check passed.
- Rendered gameplay has only been exercised on Windows. Linux/macOS CI verifies compilation, packaging and automated calculations, not native gameplay.

The development download is the final `f5b1e4b` CI JAR identified above. Compilation and synthetic integration tests do not establish full gameplay compatibility; the remaining acceptance checks are explicitly listed here.
