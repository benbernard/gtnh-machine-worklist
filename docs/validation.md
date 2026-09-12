# Validation record

## Final gameplay and in-game help for 0.1.0-beta.4 — 2026-09-12

[Published beta.4](https://github.com/benbernard/gtnh-machine-worklist/releases/tag/0.1.0-beta.4) passed [tagged CI](https://github.com/benbernard/gtnh-machine-worklist/actions/runs/34676641666). All three platform JARs, the local build and the installed test-instance JAR match: 87,955 bytes, SHA-256 `1271b358903a94bb672cc553328bcff8cf7bf3710eaa643f8f77dd6d634f0be5`. Every executable entry matches the final gameplay candidate after normalizing only version metadata. The exact release also passed startup, disposable-world loading, saved-plan reopening and NEI recipe navigation.

All 50 automated tests passed locally and on Windows, Ubuntu and macOS. This pass found and fixed external-stock accounting during chains, brief backpack synchronization states, stale-container Back navigation and overlapping wide-layout controls. A regression test first reproduced the stock defect and now passes, including persistence and save-failure rollback.

Live Windows GTNH 2.8.4 checks exercised single batches and multi-recipe chains through the player 2×2 grid, placed crafting table and handheld backpack; full-inventory refusal/recovery; held-cursor and occupied-grid preservation; exact/half/clear stock and restart persistence; filters and machine-operation explanations; the eight-table example; NEI recipe navigation; and disabling/re-enabling NEI autocrafting through its documented menu. Six in-game help topics cover setup, queue interpretation, F/G scope, stock, blockers and controls, with readable small-window text and visible scroll ranges.

The [final gameplay audit](final-gameplay-audit.md) records the three candidates, failures and fixes, screenshots, exact saved counts and remaining acceptance scope. Both saved inventories matched expected quantities after normal exits. Original test inventories, bookmarks, progress and settings were restored and verified. Multiplayer crafting, returned-tool recipes, cancellation/death mid-chain, custom NEI group creation from scratch and native Linux/macOS gameplay remain unverified live; this is a beta release.

## Recipe chains for 0.1.0-beta.3 — 2026-09-11

All 47 automated checks and the local build passed. Live player-grid tests exercised log → plank → stick chains through the actual G action, both from a selected downstream recipe and from the group queue. Existing outputs reduced later requests, exhausted raw materials produced a partial-progress message, and supplying the missing logs allowed the chain to resume to exactly 200 sticks. F still performed exactly one batch. Both saved inventories matched expected consumption and outputs, with unrelated items unchanged.

See the [chain implementation plan and evidence](chained-crafting-plan.md) for the tested candidate checksum, per-request results and exact coverage limits. Backpack chains, returned tools, cancellation mid-chain and multiplayer remain unverified in live beta.3 gameplay. Earlier speed and beta-readiness evidence below applies to the named prior revisions.

## Faster crafting for 0.1.0-beta.2 — 2026-09-11

The current source uses NEI bulk transfers, with readiness/output checks between calls and short client-tick bursts. All 38 automated tests and packaging passed locally. Through the real player-inventory worklist, 64/256/349 batches completed in 30.5/113.2/156.6 ms; the one-batch F action remained exact. Both saved inventories matched all expected outputs and consumed inputs. The original test inventory, bookmarks and published beta JAR were restored afterward.

These are instrumented singleplayer session timings, including scheduling and Worklist checks, excluding explicit server acknowledgment. The built JAR contains no instrumentation. See [the analysis, implementation plan and validation details](fast-crafting-plan.md) for the exact gameplay-tested candidate hash, original screenshots, measurement boundaries and remaining backpack/multiplayer/cancellation acceptance scope. Release artifacts and CI results are attached to [0.1.0-beta.2](https://github.com/benbernard/gtnh-machine-worklist/releases/tag/0.1.0-beta.2). The earlier release evidence below describes beta.1.

## Beta readiness pass — 2026-09-11

### Published artifact and final smoke check

[0.1.0-beta.1](https://github.com/benbernard/gtnh-machine-worklist/releases/tag/0.1.0-beta.1) is published from `e127cdbde62e054db24779ba3edc1ef94803b42d`. [Tagged CI](https://github.com/benbernard/gtnh-machine-worklist/actions/runs/34662896448) passed on all three platforms. The three downloaded production JARs and the locally packaged gameplay-tested JAR match exactly: 71,927 bytes, SHA-256 `24dcb0a0711b8311134aadb525651c27d8df614b975d7e242cfd4b20421dc184`.

The exact release JAR started and reopened the disposable world. Eight saved pistons remained recorded after the full restart; previously crafted tables and planks were still present. One stick batch completed 1/1, reducing the bulk preview from six to five batches; bulk then completed 5/5. Pressing F afterward explained the precise two-plank shortage. [Final bulk result](evidence/beta-release-bulk-result.png).

Mouse-wheel selection plus Enter opened the Steam Oven group. A waiting assembler explicitly named six missing steel casings and a reusable configuration-6 programmed circuit, with readable text at the small viewport. [Machine blocker capture](evidence/beta-machine-blockers.png). These checks supplement the earlier pass below; unverified cases remain listed explicitly.

The updated public guide was observed after the Gitea deployment. Desktop/390px layouts, navigation, keyboard skip-to-download and all image assets were checked. Chrome's automation blocked the separate plain-text checksum and health URLs (`ERR_BLOCKED_BY_CLIENT`); those browser checks are not claimed as passing. A subsequent HTTP artifact check downloaded the public JAR and matched the installed beta and all three CI artifacts to the SHA-256 above; `/healthz` returned `machineworklist-site ok`, and the checksum file matched. This was an artifact/deployment check, separate from computer-use gameplay. CopyParty remained unauthenticated, so release evidence is published in this repository and the guide.

### Recovered final compatibility checks

The audit was interrupted after these checks and before recording them here. Recovery reviewed the original tool actions and screenshots, including the selected recipe before each transfer, rather than relying only on the session's narrative. All used the exact beta.1 JAR identified above.

- Handheld Adventure Backpack: one plank batch followed by five bulk batches converted its six stored logs into twelve planks. All six flint and ten unrelated cobblestone remained in storage, and the nine crafting positions were empty. [Original F2 result](evidence/beta-backpack-bulk.png).
- The selected Oak Fence recipe filled all nine crafting slots: six sticks and three planks per fence. From the backpack, one batch consumed those quantities, with unrelated storage preserved and the crafting area empty afterward. [Original F2 result](evidence/beta-backpack-three-column.png).
- The same ready fence recipe was refused from the player's 2x2 grid, with a visible instruction to open a 3x3 crafting table or supported backpack. [Original F2 blocker](evidence/beta-two-by-two-blocker.png).
- Opening the recipe in NEI closed the original backpack container. The worklist then explained that it must be reopened before crafting; a stale container was not used.
- A placed vanilla crafting table also completed one batch of the same full nine-slot fence recipe. The source-session screenshots show the selected fence recipe and the completed 1/1 result. This closes the earlier three-column transfer gap; it does not establish every backpack variant.
- During recovery, clicking the readable upstream Oak Planks dependency opened its recipe and missing-log explanation. A normal save/exit and restart restored the readable 854x480 client viewport after fullscreen testing had left the window's rendering scale mismatched. Fullscreen transitions remain an environment limitation, not a passed compatibility check.

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

### Recovery-session full-inventory check

The recovery pass independently tested the exact installed beta.1 through Computer Use. The eight-table example had seven tables, six flint and sixteen logs, with all 36 player slots occupied (27 temporary stacks of sixteen buckets plus the existing hotbar). The empty cursor and 2x2 grid isolated output capacity from the other guards. Both F and G refused the ready recipe and opened **Why crafting is unavailable**, explaining: **Player inventory is full. Free a slot for crafting transfers and returned tools.** [Blocker](evidence/beta-full-inventory-blocker.png) · [Unchanged inventory after both attempts](evidence/beta-full-inventory-preserved.png).

The buckets were removed, preserving the original items. Reopening the same example enabled both actions and previewed one remaining batch. F completed 1/1: tables increased from seven to eight, flint decreased from six to four, and logs from sixteen to fourteen, with an empty grid. [Original F2 recovery result](evidence/beta-capacity-recovery.png). This establishes refusal, item preservation and recovery after freeing space; partially full capacity and returned-tool recipes remain separate live acceptance cases.

### Remaining beta acceptance coverage

The following are not claimed as newly verified: partial output-capacity limits in gameplay; returned-tool crafting; worn/placed backpack variants; cancellation mid-request; death during crafting; multiplayer transfers/persistence; and Linux/macOS rendered gameplay. The NEI-disabled guard and the entire custom-group creation walkthrough still require a dependable live input pass; the included example path passed. Handheld backpack bulk crafting, true three-column transfers and the full-inventory guard passed above. Earlier read-only multiplayer results below remain historical evidence. Complex fluids, chance outputs and cycles have synthetic coverage, not exhaustive live pack coverage. Screen-reader operation of the native Minecraft controls has not been established.

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

- The earlier three-column gap is closed by the beta fence transfers above in both a handheld backpack and a placed crafting table.
- Occupied grid/cursor preservation and full-inventory refusal/recovery are checked above. Worn/placed backpack variants remain open; handheld storage accounting and cleanup also passed on beta.1.
- Check tooltip placement at window edges and broader screen sizes.
- Exercise death handling while the worklist is open on the latest build.
- Validate larger actual GTNH groups, fluids, chance outputs and unavailable handlers beyond synthetic fixtures.
- Verify a crafting transfer and completion persistence on multiplayer; the EMBU connection and read-only worklist check passed.
- Rendered gameplay has only been exercised on Windows. Linux/macOS CI verifies compilation, packaging and automated calculations, not native gameplay.

The earlier development download was the `f5b1e4b` CI JAR identified above. Compilation and synthetic integration tests do not establish full gameplay compatibility; use the current release and its coverage notes.
