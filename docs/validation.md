# Validation record

## Beta readiness pass — 2026-09-11

All new interactive checks below were performed through computer use in the separate **Crafting Helper Test** Prism instance and disposable **New World**. Builds/package generation used Gradle; the repository's existing CI runs its automated suite on pushes. No production world or server inventory was changed.

- Feature commit `61bd9a8` passed [Windows, Ubuntu and macOS CI](https://github.com/benbernard/gtnh-machine-worklist/actions/runs/34661029188). The suite now contains 33 checks, including bulk output yield, shared capacity, NBT identity, unstackable outputs and large counts. The final beta also reserves space for returned tools/containers.
- Installed the normal JAR through Prism's Mods panel after disabling the old version. Startup and disposable-world loading passed.
- The new named picker, first-run help and eight-table example opened in the small 854×480 game viewport. A crafting-only plan selected Crafting automatically. Existing tables reduced the example's remaining quantity.
- With three tables and 16 flint/16 logs, **F** crafted exactly one table. Inventory became four tables, 14 flint and 14 logs, with an empty cursor/grid. [Original F2 capture](evidence/beta-one-batch.png).
- **G** then previewed and crafted the four remaining table batches, despite enough ingredients for seven. Inventory became eight tables, six flint and six logs. The queue reported no work remaining. [Original F2 capture](evidence/beta-bulk-remaining.png).
- Against an existing piston chain, six remaining logs supplied six batches yielding twelve planks. Bulk crafting completed 6/6, then disabled further crafting and explained that one more log was required for one batch. **B** opened the complete status in a separate readable screen.
- Guard build based on `61bd9a8` (SHA-256 `aec9ae674c848321458a36a73d5213f7c5af76a8d32084e6fbf4d0af72cfa072`) kept the inventory open when F10 was pressed with logs on the cursor. It explained where to place them. With logs in the 2×2 grid, it instead asked to empty the grid/result. Moving the logs back cleared the explanation and allowed opening. [Original F2 capture](evidence/beta-occupied-grid.png).
- Available-stock editing rejected `-1` without changing the recorded zero. **H** saved eight of sixteen remaining pistons, with Recorded 8 / Inventory 0 / Credited 8 and adjacent save feedback. Tab visibly focused Back. Clicking Clear record restored zero and sixteen remaining. Saving eight again prepared the restart check.
- Enter, F, G, B, C, H, Tab and Escape were exercised. The Windows automation did not reliably deliver extended arrow/Delete keys to Minecraft; those shortcuts are implemented but are not counted as live passes.
- The local HTML guide was inspected through the connected Chrome extension at a 390px phone viewport. The controls list stacked without horizontal overflow, navigation anchors worked, and the accessibility tree exposed the semantic definition list.

The GUI-container entry guard fixes a subtle item-preservation issue: Minecraft closes and clears the old crafting GUI when another screen is displayed. The guard now explains an occupied cursor/grid over that original GUI instead of opening a replacement screen first.

### Remaining beta acceptance coverage

The following are not claimed as newly verified: a true three-column transfer; a full-inventory guard in gameplay; returned-tool crafting; bulk crafting in backpack variants; cancellation mid-request; death during crafting; multiplayer transfers/persistence; and Linux/macOS rendered gameplay. Earlier single-batch handheld-backpack and read-only multiplayer results below remain historical evidence. Complex fluids, chance outputs and cycles have synthetic coverage, not exhaustive live pack coverage. Screen-reader operation of the native Minecraft controls has not been established.

The release is a **beta**, with these boundaries visible to users. The final release entry identifies its version and downloadable artifact; historical build hashes below refer only to the checks described beside them.

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

## Outstanding acceptance checks from the earlier development pass

- Complete a real one-batch transfer of a recipe requiring all three crafting columns. The tested table recipe fits 2x2, including when transferred through the backpack's 3x3 area.
- Occupied grid/cursor preservation is now checked above. Full inventory and worn/placed backpack variants remain open; handheld storage accounting and cleanup passed on the earlier build.
- Check tooltip placement at window edges and broader screen sizes.
- Exercise death handling while the worklist is open on the latest build.
- Validate larger actual GTNH groups, fluids, chance outputs and unavailable handlers beyond synthetic fixtures.
- Verify a crafting transfer and completion persistence on multiplayer; the EMBU connection and read-only worklist check passed.
- Rendered gameplay has only been exercised on Windows. Linux/macOS CI verifies compilation, packaging and automated calculations, not native gameplay.

The earlier development download was the `f5b1e4b` CI JAR identified above. Compilation and synthetic integration tests do not establish full gameplay compatibility; use the current release and its coverage notes.
