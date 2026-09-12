# Final gameplay audit

Requested scope: retest the release, exercise the user flows and ensure the instructions are usable inside the game. This document separates executed checks from planned checks and records fixes before publication.

## Plan

1. Review entry, navigation, help, inventory accounting, chain transfer and cancellation paths.
2. Make help accessible directly from the queue and recipe details; explain controls, scope, stock and recovery in short topics.
3. Run the full automated suite and build the actual candidate.
4. Exercise the candidate in the separate Crafting Helper Test instance: help/setup, example, filters, recipe navigation, exact/half/clear stock and persistence, single and chained crafting, 2x2/3x3/backpack, blockers and recovery.
5. Inspect game logs and saved inventory, restore test inputs/bookmarks, then publish the verified build and update the validation record.

## Coverage record

Prior release evidence is retained in validation.md; it is not counted as a new live pass.

First candidate SHA-256: `0965f77bc69fa6d9dfe768d17545ef6ebc0cf47fed9597b6357c9310a0c1e17f`.

- Passed in the small 854×480 client: held-cursor entry refusal, occupied-grid refusal, recovery after returning the same 32 logs to inventory, named picker, help/topic navigation and wheel scrolling, empty Machines tab guidance, F exactly one plank batch and G whole-group scope from the Machines tab.
- The three-stage eight-fence request used 32 player logs, yielding 16 planks and 48 sticks before pausing at the 3×3 requirement and missing logs. The original request correctly reported 55 batches after the separate single batch.
- Opened the supported handheld backpack containing another 32 logs, six flint and ten cobblestone. Selected-chain G performed 12 more batches across planks and fences, completed exactly eight fences, and left 28 stored logs. This exercises recipe changes and shared materials across a real 3×3 backpack grid.
- Completed outputs remained reachable in Available stock. A negative record was rejected. Recording eight fences while holding eight credited eight, and the stock-help shortcut worked. H in a twelve-fence group credited ten total (eight physical plus two recorded), leaving two fences to make.
- The stock-plus-chain request exposed an intermittent cursor/grid transition after nine plank batches. After synchronization the real contents were correct: eight fences, 18 planks, 19 stored logs, original six stored flint/ten cobblestone and all unrelated player items unchanged. Both saved inventories matched after normal shutdown.
- A separate regression test reproduced the accounting defect: new physical target outputs displaced part of the fixed manual credit, causing a false unfinished chain.

Second candidate SHA-256: `a5da5b350a6a4714d89cd6b3185747b0e0f677764fb3ccbaaf8e6144182e3dad`. All 50 automated tests pass, including a failing-before/fixed-after external-target-stock test, consumed/produced record accounting, persistence and rollback after a failed save.

- The repaired help menu renders without the button seam. The stock topic explains the new automatic record updates and the physical-input boundary.
- A full client restart restored the twelve-fence group's recorded total of ten, with eight physical fences and two still to make.
- With all 36 player slots occupied (30 temporary stacks of 16 buckets plus the six real item stacks), both F and G refused crafting and explained the free-slot requirement. The backpack still held all 28 logs and unrelated supplies afterward. Clearing exactly 480 temporary buckets restored capacity.
- Repeating the previously failing case then completed all 17 batches (nine planks, six sticks, two fences) in the backpack. The result reported Chain complete. Stock read Recorded 12 / Inventory 10 / Credited 12, preserving two units of recorded external stock.
- Tab reached every stock action; Enter on Clear record removed the manual credit while retaining ten physical fences. The queue restored two remaining fences.
- Nine test logs supplied in player inventory then completed those two fences through all three recipes in a placed vanilla crafting table: 17 batches, Chain complete. The closed backpack's stored logs were excluded from that request.
- The eight-table example first named its missing logs. N opened the exact NEI recipe and Escape returned to its selected detail. NEI closed the original table; F refused that stale container with instructions to reopen it.
- With sixteen player logs and sixteen flint, the example completed through the player 2×2 grid: F made one table; G made the remaining seven. The queue then reported no remaining work.
- The machine group showed three machine operations, and ready-only retained the ready steel bending operation while overall missing inputs stayed unchanged. Its detail explained that tier/power/contents are not scanned. G on that machine-only request refused with a named manual-operation explanation.
- Maximizing and restoring the window worked. The larger layout revealed that queue buttons covered the missing-input heading; the final layout moves them into the queue column. Back from an NEI-closed container also needs to return to the world instead of displaying a stale table; that final UI correction is pending live verification.

The test client is separate from the production instance. Inventory, bookmarks, progress records and display settings were backed up before the fixture. Original backups must be restored after final verification and after the client has actually exited.

## Changes under test

- Six in-game help topics, accessible with H and a visible button in the picker, queue and details.
- Guidance explicitly distinguishes single batch, selected chain and whole-group scope, including hidden tabs/filters.
- Stock examples, recipe-tree setup, supported storage, disabled-action explanations and restart procedures remain available without leaving the game.
- Existing manual totals follow verified crafting output and consumed inputs, preserving off-inventory stock through a chain; unrecorded stock gets no invented record.
- Brief cursor/grid synchronization states yield for up to ten client ticks without submitting clicks. Persistent leftovers keep the real container open and report the result in local chat, avoiding GUI-close item disposal.
- Help-topic buttons are bounded to avoid a seam in Minecraft's button texture.
- Wide-screen queue buttons stay out of the missing-input pane. Returning to a container closed by NEI goes to the world, rather than showing a stale container.

## Coverage limits

The full automated suite covers pinned NEI math, fluids, metadata/NBT, reusable ingredients, shared stock, cycles, output capacity, bounded bursts and the stock regressions. This live pass is local Windows singleplayer. It does not establish multiplayer/server latency behavior, Linux/macOS rendered gameplay, every returned-tool/container recipe, worn/placed backpack variants, death or cancellation during a long active chain. Those remain explicit acceptance checks. Native pointer and extended-key injection is intermittent in this client; a source-verified shortcut is not counted as a live pass merely because it exists. The complete custom-group creation path in NEI and toggling its autocrafting setting still need reliable input coverage.
