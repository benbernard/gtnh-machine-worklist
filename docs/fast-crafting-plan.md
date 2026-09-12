# Faster bulk crafting

## Findings

The beta.1 executor hard-codes a two-client-tick delay and calls NEI with multiplier 1. At 20 ticks/sec this limits it to about 10 batches/sec, matching the 64/256/349-batch gameplay measurements. Each batch also repeats the full chain calculation, readiness checks, ingredient transfer, output pickup and grid cleanup.

The pinned NEI 2.8.44-GTNH engine uses the same `RecipeHandlerRef.craft(gui, multiplier)` API with multipliers up to 64. Its default overlay fills the grid with multiple batches of ingredients and shift-clicks the output, then refills if necessary. Thus it amortizes both inventory clicks and planning overhead. Its autonomous manager runs continuously on a worker thread; adopting that entire manager would also change recipe scope and bypass this mod's backpack-specific cleanup.

The NEI benchmark measured the full native client task through direct API calls, excluding the UI shortcut and explicit server-acknowledgment waits: 64 batches in 8.4–10.8 ms, 256 in 31.3 ms, 349 in 42.5 ms. Saved item counts matched. Worklist's earlier results were visual elapsed times of about 6.6/25.8/35.2 seconds; these are different timing endpoints.

## Implementation

- Reuse NEI's bulk API for only the user-selected recipe, capped at 64 batches, remaining requested work and freshly checked readiness/output capacity.
- Remove the fixed two-tick delay. Recalculate and verify between bulk transfers.
- Run on the client thread, yielding after an 8 ms budget or eight transfers. The time bound is checked between calls; an individual NEI call cannot be interrupted.
- Preserve current-container, live-player, cursor, grid, recipe-fit, input, output-space and NEI-busy checks. Preserve the dedicated backpack overlay and slot boundaries.
- Check actual output growth after each transfer. Count verified partial production before stopping; stop on fractional, excess or missing output.
- Keep Craft 1 batch exact and retain cancellation when the container closes.

## Validation

- Automated checks for burst yielding, finite work even with a frozen clock, immediate stop, verified partial progress and invalid output changes.
- Run the existing calculation, output-capacity and backpack-boundary tests and package the Java 8 mod JAR.
- In the disposable GTNH test world, compare 64/256/349 batches and verify saved output/ingredient totals. Exercise the one-batch action and cancellation/blocker paths.
- Record actual coverage and any remaining live backpack/multiplayer gaps. Restore fixture inventory and bookmarks. The initial implementation pass did not publish; the subsequent release request promotes this change to 0.1.0-beta.2.

## Results — September 11, 2026

Implemented and packaged locally. `spotlessApply test build` passed all 38 tests. The candidate JAR is 74,037 bytes, SHA-256 `63c37f89b4afaa0eaab3d5dc6c5091e6f3f7175bd7471b31243f692ab9848c33`, built from `747d1bf` plus the working-tree changes described above.

Live tests used the same disposable GTNH 2.8.4 singleplayer world and oak-log recipe, starting the actions through the actual worklist's G/F keys. A temporary passive Java agent timed session construction through entry to `finish`, including the GUI return, client-tick scheduling, readiness recalculation and output verification. The timer excludes result-screen rendering and an explicit server-acknowledgment wait. It does not change the request or crafting behavior and is absent from the built mod JAR.

| Batches | Old beta visual elapsed time | Updated session elapsed time | NEI calls |
|---:|---:|---:|---:|
| 64 | about 6.6 s | 30.526 ms | 1 |
| 256 | about 25.8 s | 113.206 ms | 4 |
| 349 | about 35.2 s | 156.610 ms | 6 |
| 1, F action | — | 9.202 ms | 1 |

Every result screen reported the exact requested count, and reopening each group showed no remaining work. The bulk calls never exceeded multiplier 64; F used multiplier 1. Larger runs achieved about 2,200–2,260 batches/sec. The earlier NEI engine-only results were faster still: Worklist retains readiness calculations, verification and deliberate yields between bursts. Old/new timing endpoints differ, and the small set of singleplayer runs is not a multiplayer throughput guarantee. Published evidence: [64 batches](evidence/fast-crafting-64.png), [256 batches](evidence/fast-crafting-256.png), [349 batches](evidence/fast-crafting-349.png), [one batch](evidence/fast-crafting-one.png), and [timing/call records](evidence/fast-crafting-results.json).

After normal exit, both saved inventory locations contained exactly 1,346 planks and 176 logs: 6 + 2 × (64 + 256 + 349 + 1) planks, and 14 + 832 - (64 + 256 + 349 + 1) logs. Unrelated inventory counts matched. Original inventory tags and bookmark bytes were restored, as was the published beta JAR in the test instance. The candidate remains in `build/libs/`; no release was published. Raw logs, screenshots, probe source and inventory accounting are retained locally under ignored `build/benchmarks/2026-09-11-fast-worklist/`.

Remaining acceptance scope: live multiplayer and returned-tool behavior remain unverified. The existing backpack layout/output-capacity tests passed, but live backpack transfers and mid-run Escape/blocker scenarios were not repeated with this candidate. Automated burst tests verify yielding and immediate stop when the session refuses another transfer; the existing live-player/container/readiness checks remain in the path before every transfer. The 8 ms budget is a bound between calls, not a hard limit on one expensive NEI operation.
