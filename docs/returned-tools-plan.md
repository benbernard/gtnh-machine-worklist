# Returned tools during chained crafting

The user reports files/screwdrivers that remain visible in Adventure Backpack's crafting grid after a large LV Cutting Machine group. Clicking the tool makes it disappear into inventory. The report identifies the backpack; singleplayer versus multiplayer was not confirmed. Computer use remains paused at the user's request.

## Findings and change

Adventure Backpack 1.3.13-GTNH maintains nine visible storage slots, nine hidden crafting mirrors, and a result slot. Its `SlotCraftResult` copies storage to the mirror before crafting and copies returned ingredients back afterward. Ordinary slot clicks synchronize storage to mirrors again. These paths were inspected in the exact installed JAR, along with NEI 2.8.44-GTNH and the pinned Minecraft sources.

The old session waited only after it had already counted a successful transfer, and final or partial transfers could exit immediately. It did not retry returning tools. A stale grid could therefore stop a chain or prevent the result screen from opening.

Introduce a settlement phase after transfers containing reusable/container-return inputs or leaving the grid/cursor occupied. It runs once per client tick, independently of the fast transfer burst. It watches copied container slots and cursor, including backpack mirrors and tool NBT, and waits for a quiet interval scaled to reported latency. Late updates restart the quiet interval. Ordinary empty-grid recipes keep the fast bulk path.

If blocked after settling, refresh the backpack's derived mirror with its own `syncCraftMatrixWithInventory(true)` method, then shift-click only recognized returned tools in real input slots into player inventory. A recognized cursor tool may be placed into an empty player slot. Never click the result, hidden mirror or unrelated storage slots. Metadata-selected tool types must match, while durability changes are allowed; existing stacks move intact. Other items remain for manual inspection. Recovery is limited to two attempts and 200 client ticks total (about ten seconds at normal tick rate). Closing/changing the container, death or disconnection stops further work.

Verify outputs and update the finite chain/stock records only after settlement. This applies to first, intermediate, partial and final transfers. If settlement times out, the result explicitly marks this transfer unconfirmed and directs the player to inspect/reopen; it does not count guessed output. In-game help explains the wait and recovery. Stable client slots and a latency allowance are not proof of a server acknowledgment.

## Validation

71 automated checks: the existing 61, seven settlement state-machine tests, and three Forge/NEI fixtures. Added coverage exercises final-transfer waiting, late mirrors, bounded recovery, unknown-item refusal, fixed deadlines despite changing inventory, latency bounds, actual cleanup slot boundaries, metadata-selected tool types, unchanged tool damage/NBT, and detecting changed snapshot contents. Fixtures do not execute a real backpack transfer or its reflective synchronization method.

Live acceptance remains pending: the reported LV Cutting Machine group with GT files/screwdrivers in the supported backpack, exact tool durability/item preservation, cursor recovery, limited inventory space, multiplayer latency, cancellation during settlement, and completion/reopening of the final recipe. No new gameplay screenshots or live verification are claimed. Build and release the tested artifact, update closed profiles, and stage the update disabled for any running profile.
