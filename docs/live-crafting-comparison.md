# Live Minecraft crafting comparison

Tested September 20, 2026, in GTNH 2.8.4 with NEI 2.8.44-GTNH, Adventure Backpack 1.3.13-GTNH and Java 25.0.1. These are fresh measurements of the multiplayer synchronization candidate, running inside the full Minecraft client and its local integrated server.

The candidate completed every timed trial correctly. On a crafting table, its median was **2.96 seconds**, compared with **1.54 seconds** for native NEI. The candidate took about 1.9 times as long, or 1.41 seconds more, for this workload. On the backpack, the candidate's median was **3.71 seconds**. Native NEI failed all three backpack trials, so it has no valid completion-time comparison there.

## Results

Each fresh world started with 216 iron ingots, an empty handheld Adventure Backpack and the same four reusable tools. The selected NEI group contained five recipes: plates, rods, bolts, screws and wire cutters. The target was 24 wire cutters, requiring 216 recipe runs.

| Container | Engine | Trial times, seconds | Median | Correct completions |
| --- | --- | --- | --- | --- |
| Crafting table | Native NEI | 1.908, 1.422, 1.543 | 1.543 s | 3/3 |
| Crafting table | Candidate Worklist | 3.108, 2.955, 2.906 | 2.955 s | 3/3 |
| Adventure Backpack | Native NEI | Failed, failed, failed | Unavailable | 0/3 |
| Adventure Backpack | Candidate Worklist | 3.913, 3.709, 3.421 | 3.709 s | 3/3 |

Every successful table run sent 331 crafting-window clicks. Every candidate backpack run sent 339. All were acknowledged as accepted, with no rejected or outstanding clicks when each measurement ended.

After normal world shutdown, both the saved player data and `level.dat` confirmed exactly 24 cutters, all four tools with the expected wear, and an empty backpack. Screenshots also showed an empty crafting grid. Saved tool damage matched across all successful trials: saw 4,800; hammer 38,400; file 48,000; screwdriver 9,600.

## Native NEI's backpack failure

| Trial | Clicks sent | Accepted | Rejected | Outstanding at 30 s | Saved cutters |
| --- | --- | --- | --- | --- | --- |
| 1 | 3,371 | 280 | 32 | 3,059 | 0 |
| 2 | 3,190 | 266 | 36 | 2,888 | 0 |
| 3 | 1,832 | 137 | 19 | 1,676 | 0 |

The first trial threw a `NullPointerException` in `DefaultOverlayHandler.moveIngredients`, line 209: a slot returned no stack when NEI read its stack size. The second and third trials stopped issuing clicks with unfinished inventories. All failed the saved-output check. The first two left items in backpack storage. These failures are excluded from the speed figures. The last acknowledgment time of a failed run is not its completion time.

This is direct evidence that native NEI's chain worker can misbehave with this backpack and workload. It does not identify which difference prevents the candidate's failure: the candidate also constrains backpack slots and runs transfers on the client thread, as well as waiting for confirmed inventory updates. Copying the native worker unchanged would carry over the observed failure path. It would not establish the cause of the delayed multiplayer kick.

## Live keyboard and GUI checks

A separate disposable world exercised the actual controls:

- F10 from the world opened a worklist labeled **Player 2x2**, backed by the active player container, window 0.
- Pressing G crafted the 120 recipe runs that fit 2x2, then paused plates and cutters with a grid-size explanation. It did not use the closed backpack.
- Opening the backpack and pressing F10 showed **Backpack 3x3**, backed by the exact active backpack container.
- Enter selected a recipe; N opened NEI; Escape returned to the real backpack GUI and restored its active container, window 1.
- F10, Escape to the group, and G completed the remaining 96 recipe runs. The saved result passed the same 24-cutter and tool-wear check.
- Completion left the real backpack GUI open with an empty cursor. The Worklist session had stopped and its packet observer was absent from the connection.
- Closing the backpack returned to the player container. F10 then opened a fresh **Player 2x2** worklist, without reusing the closed backpack.

## Measurement method and limits

The tests ran in the disposable Crafting Helper Test Prism profile. Engine order alternated across fresh world copies. Both engines used the same full client, selected recipes, starting inventory and passive packet timer. The candidate was installed for both paths. Native NEI was invoked through its own `AutoCraftingManager.runProcessing` entry point.

A test-only Java attachment invoked each engine on the client thread after the real crafting GUI was opened. The Worklist path invoked its Craft group handler. This measures the actual crafting engines, including their planning work, without human click timing. Separate keyboard tests covered the visible entry and return workflow.

The timer started at engine entry and ended at the later of engine stop or receipt of the final click acknowledgment. It polled every 5 ms and excluded a subsequent 500 ms observation interval. Its packet observer forwarded packets unchanged. Acknowledgment receipt is distinct from application to the client inventory. The candidate's own gate waits for application and a full refresh. Saved authoritative inventory was checked separately. Failed runs reached a 30-second observation limit and are reported as failures.

A lifecycle helper used Minecraft's normal world load/unload APIs and waited for the old server thread to exit between trials. After NEI's first backpack worker crashed, a test-only helper cleared that dead worker and restored its initial idle flags before the next independent NEI trial. No recipe, packet, inventory or production implementation was changed to make a timed trial pass.

These three repetitions per engine/container provide a local comparison under one workload. The trials used an integrated server, without remote network latency. They did not reproduce the reported kick 10-20 minutes after crafting, and they do not prove it is fixed. Cancellation and artificial packet-delay cases remain covered by the earlier automated fixtures rather than these live UI checks.

The earlier build and regression suite passed 92 tests, with zero failures, errors or skips. This live test pass required no further production-code changes. The candidate's existing confirmation waits remain in place despite their measured speed cost.

## Build and evidence

Candidate: `machineworklist-0.1.0-beta.13-codex-multiplayer-crafting-sync.1+bd28b55c20-dirty.jar`, based on `bd28b55`, with uncommitted synchronization fixes. SHA-256: `1f512ba80cab0795cc4aa527c470bc09a52c25e0b647ba17c04033f92f49d7f1`.

Local evidence is under `build/benchmarks/2026-09-20-sync/`: timer source, raw results, saved-inventory verification, runtime GUI checks, native screenshots, logs and the disposable worlds. The raw first failed NEI trial has a negative engine-duration sentinel because its worker never reported completion. It is excluded from all statistics.

The primary profile's mod files and worlds were unchanged. The test profile's previous JAR, bookmarks, options and Worklist/NEI configuration were restored after testing. The candidate has not been installed into the primary playing profile or published as a release.
