# Remaining gameplay validation

This is a test procedure, not a record of passed checks. Use the separate GTNH 2.8.4 Prism instance and disposable local world for item-giving commands.

## Recorded steam-oven group

The existing test-instance NEI group 16 targets 16 steam ovens. Its previously observed empty-inventory worklist contains 164 steel-plate bending runs, 32 wrought-iron-plate bending runs, and 16 assembler runs. These group numbers are local bookmarks, not built-in mod fixtures.

1. Open inventory once to load NEI. Hover the group and press F10, or use `/machineworklist 16`.
2. Verify the empty-inventory quantities against the selected NEI recipes. Check the Machines/All steps and readiness filters.
3. In the disposable world, run `/give @p gregtech:gt.metaitem.01 64 17305` to add 64 steel plates. Reopen the same group. The steel-plate bending work should decrease from 164 to 100 runs.
4. Add 64 steel ingots with `/give @p gregtech:gt.metaitem.01 64 11305`. The remaining steel-plate production stays at 100 runs. Its readiness must account for the missing programmed circuit.
5. Add the selected circuit configuration with `/give @p gregtech:gt.integrated_circuit 1 1`. Check that the steel bending operation can now run 64 batches with current inventory.
6. Inspect the circuit tooltip, open the selected NEI recipe, and use Escape to return. Confirm quantities and selection survive navigation.
7. Remove or consume test stock and reopen the plan. Confirm it recalculates instead of retaining a stale completion state.

## Display and chain coverage

- Open an empty machine view (the recorded group 14 consists of crafting recipes). Verify controls still work and All steps reveals the crafting work.
- Test a larger saved group, scroll through every row, and check the scroll indicator, full item tooltips, and detail navigation at small and large window sizes.
- Compare fluids in mB with NEI, including partial supply split among containers.
- Verify missing reusable molds/circuits appear once in Missing inputs and disappear when owned.
- Verify chance-output notes and external-input boundaries for cyclic or competing recipes are visible and understandable.
- Verify missing recipe handlers produce a clear unavailable state.

## Multiplayer

Connect using the test client to the intended GTNH 2.8.4 server with no server-side Machine Worklist installation. Do not use item-giving commands there. Confirm connection, selected-group import, inventory accounting, UI navigation, and absence of client/server errors. Record the server pack version and observed result without publishing its address or account details.

## Evidence

Record the tested JAR SHA-256, target group, starting inventory, expected/actual quantities, and any failures. The currently installed CI artifact is `8b60fda`, SHA-256 `911894b8697ff9f6216e524003778373e9e3d4b2473be5690264e1e29ab3bbda`. Automated tests and artifact installation alone do not prove these gameplay checks passed.
