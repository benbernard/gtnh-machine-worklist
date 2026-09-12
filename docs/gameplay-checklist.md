# Remaining gameplay validation

This is a test procedure, not a record of passed checks. Use the separate GTNH 2.8.4 Prism instance and disposable local world for item-giving commands.

The [validation record](validation.md) now includes beta.1 handheld-backpack bulk crafting, full nine-slot fence transfers in backpack and placed table, incompatible-2x2 refusal, and full-inventory refusal/recovery. Retain these procedures for regression testing; do not treat them as still-unverified results. Remaining live coverage includes partially full output capacity, returned tools, NEI disabled, custom NEI group creation from scratch, mid-request cancellation/death, worn/placed backpacks, multiplayer and native Linux/macOS gameplay.

## Recorded steam-oven group

The existing test-instance NEI group 16 targets 16 steam ovens. Its previously observed empty-inventory worklist contains 164 steel-plate bending runs, 32 wrought-iron-plate bending runs, and 16 assembler runs. These group numbers are local bookmarks, not built-in mod fixtures.

1. Open inventory once to load NEI. Hover the group and press F10, or use `/machineworklist 16`.
2. Verify the empty-inventory quantities against the selected NEI recipes. Check the Machines/Crafting/All and readiness filters.
3. In the disposable world, run `/give @p gregtech:gt.metaitem.01 64 17305` to add 64 steel plates. Reopen the same group. The steel-plate bending work should decrease from 164 to 100 runs.
4. Add 64 steel ingots with `/give @p gregtech:gt.metaitem.01 64 11305`. The remaining steel-plate production stays at 100 runs. Its readiness must account for the missing programmed circuit.
5. Add the selected circuit configuration with `/give @p gregtech:gt.integrated_circuit 1 1`. Check that the steel bending operation can now run 64 batches with current inventory.
6. Inspect the circuit tooltip, open the selected NEI recipe, and use Escape to return. Confirm quantities and selection survive navigation.
7. Remove or consume test stock and reopen the plan. Confirm it recalculates instead of retaining a stale completion state.

## Display and chain coverage

- Open an empty machine view (the recorded group 14 consists of crafting recipes). Verify controls still work and Crafting reveals the crafting work.
- Test a larger saved group, scroll through every row, and check the scroll indicator, full item tooltips, and detail navigation at small and large window sizes.
- Compare fluids in mB with NEI, including partial supply split among containers.
- Verify missing reusable molds/circuits appear once in Missing inputs and disappear when owned.
- Verify chance-output notes and external-input boundaries for cyclic or competing recipes are visible and understandable.
- Verify missing recipe handlers produce a clear unavailable state.

## Manual completion

- From a recipe detail, open Available stock (or press C). Set an exact output total and verify the queue and upstream input quantities shrink.
- Complete half the remaining output; verify rounding to whole recipe batches, including odd batch counts and recipes producing multiple items per batch.
- Pick up already-recorded output and confirm inventory does not count it twice. Inventory exceeding the recorded total should still count in full.
- Close/reopen the worklist and restart the client; verify the same world/server and unchanged group restore progress. Another world or changed recipe selection must not inherit it.
- Set an output fully complete, then find it again in the completion editor and clear it. Verify the operation returns.
- Check fluid entries use mB. Record coproducts separately and check chance outputs are not automatically invented.
- Check the editor at small and large window sizes, including errors, long item names, and Previous/Next navigation.

## Multiplayer connection

Connect using the test client to the intended GTNH 2.8.4 server with no server-side Machine Worklist installation. Do not use item-giving commands there. Confirm connection, selected-group import, inventory accounting, UI navigation, and absence of client/server errors. Record the server pack version and observed result without publishing its address or account details.

## Evidence

Record the tested JAR SHA-256, target group, starting inventory, expected/actual quantities, and any failures. See validation.md for the tested revisions and artifact hashes. Automated tests and artifact installation alone do not prove these gameplay checks passed.

## Tabs, overall inputs pane, and NEI crafting

- Compare Machines, Crafting, and All against one group; confirm tab filters do not change overall missing inputs.
- Scroll the queue and missing pane independently. Resize below the split-pane threshold and back, including while showing missing inputs.
- Open from a crafting table using F10. Select a ready shaped/shapeless recipe and craft exactly one batch through NEI. Verify item consumption and output, then reopen and check remaining runs.
- Verify disabled crafting with an incompatible 2x2 grid, stale/closed container, full inventory, held cursor stack, occupied crafting grid, missing inputs, or NEI autocrafting disabled.
- Verify the action never runs a machine recipe and does not apply manual off-inventory stock as available crafting input.

## Adventure Backpack 1.3.13-GTNH

- Open F10 from the backpack GUI with materials in ordinary storage and bottom-right 3x3 empty; verify Craft 1 batch via NEI consumes the correct inputs and yields one batch.
- Compare readiness using player stock only, backpack stock only, and mixed stock; verify hidden mirror stacks are never counted twice.
- Verify filled crafting slots disable the action without clearing contents. Unrelated tool/fluid/storage slots must not block it.
- Move intermediate outputs into the open backpack and verify queue/missing-pane reduction; compare manual totals without double counting.
- Close or replace the backpack container and verify stale actions are disabled; test the unchanged vanilla crafting-table path.
