# Acknowledged tool cleanup

## Source comparison

The main Prism GTNH 2.8.4 profile uses NotEnoughItems 2.8.44-GTNH, not JEI, and Adventure Backpack 1.3.13-GTNH. NEI's `DefaultOverlayHandler.craft` fills a batch, shift-clicks the output and clears ingredients using ordinary vanilla window clicks. The backpack overlay inherits that cleanup. Our overlays already use the same algorithm with explicit real grid/storage boundaries.

The additional delay was in our wrapper: after receiving transfer acknowledgments it waited another 3–40 client ticks for the entire container to stop changing before recovering leftovers. At a reported 100 ms ping this was six ticks (about 300 ms at 20 TPS), restarting on unrelated inventory changes. This is a code-derived delay, not a measured overall speedup.

## Implementation

1. Keep the existing gate for every submitted click, including NEI cleanup, and the following client update tick. Rejected or missing acknowledgments still stop the session.
2. After that gate, refresh Adventure Backpack's derived crafting matrix from its real storage using the backpack's own synchronization method. Do this before deciding whether anything remains, so a stale mirror does not cause a redundant recovery transaction.
3. Continue immediately when the cursor/grid are clear. Otherwise recover only recognized returned tools from actual input slots or the cursor, without another ping-based quiet interval. Storage, result and hidden mirror slots never qualify for recovery clicks.
4. Await acknowledgment of any recovery before proceeding. Keep the two-attempt limit, observation timeout, unknown-item refusal, container cancellation and output verification.
5. Keep up to 64 recipe runs per transfer and existing bulk intermediate ordering. This change does not pipeline unconfirmed transfers like native NEI's worker: an earlier live trace demonstrated a rejected stale-tool click when recovery raced server updates.

## Validation

Settlement tests cover immediate confirmed cleanup, mirror-only completion, unknown blockers, fixed deadlines and bounded recovery. Forge/NEI fixtures check that cleared real tool slots leave no recovery candidates even when mirror/storage slots still contain tools, plus existing tool durability, metadata and bulk-chain tests. Transaction tests retain delayed, missing and rejected acknowledgment coverage.

Build/test results and any live timings should be recorded separately. Automated tests do not establish multiplayer timing or reproduce the user's intermittent connection resets.

Local validation on 2026-09-12: spotlessApply test build passed, including all 91 tests. No new live NEI-versus-worklist timing is claimed for this change.
