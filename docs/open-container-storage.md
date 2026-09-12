# Open-container ingredient and output storage

## Scope

Use both ingredient supply and overflow space exposed by the currently open Adventure Backpack or Tinkers' Crafting Station. The selected NEI recipes and quantities remain authoritative. No computer use while the user is playing.

## Installed-container evidence

Inspected Adventure Backpack 1.3.13-GTNH and TConstruct 1.13.57-GTNH locally. Backpack slots 36–83 are real storage; 65, 66, 67, 73, 74, 75, 81, 82, 83 are also the real crafting grid. Only the other 39 storage slots are overflow destinations. Utility slots 84–89, hidden mirrors 90–98 and result 99 are excluded. The held hotbar item must stay in place so a handheld backpack remains open.

The station exposes attached inventories through ChestSlot, including side-specific insertion/extraction permissions. Use only those existing slot objects, deduplicated by inventory identity and underlying index. Exclude InventoryPlayer, InventoryCrafting, InventoryCraftResult and SlotCrafting from extra storage. Do not scan neighboring blocks or closed containers.

## Implementation

- Share exposed storage discovery between physical stock snapshots and the station's NEI ingredient overlay. Respect extraction permissions and copy snapshot stacks.
- Plan overflow on copies. NEI still fills from the player workspace and shift-clicks results there, so merely counting chest capacity is insufficient.
- When workspace is insufficient, move whole player stacks into eligible open storage with ordinary left clicks. Prefer existing crafted outputs, then main inventory, then other hotbar stacks. Never move the held slot. Merge compatible stacks first; respect NBT, item/slot limits and slot permissions. Conservatively require overflow destinations to permit extraction too, so relocated stock stays available to the chain.
- Require room for outputs and returned tools plus an initially empty player transfer slot. Preserve up to 64 batches per NEI call; no per-output loop or new blanket tool delay.
- Recheck active container, source, destination and cursor around moves. Restore a refused move to its empty original source when safe. Never drop, swap unrelated items, directly mutate production inventories or click a replacement container.
- Recalculate from combined player and exposed storage after each transfer. Preview never moves items; F/G starts actual redistribution.

## Verification

Six new Forge-loaded fixtures exercise extraction restrictions and duplicate slot views, the 39 backpack destinations, full-player 64-batch workspace with held-item preservation, multi-slot limits/NBT/tool-space rejection, refused destination recovery, and container closure with no further clicks. Existing station mapping tests now require attached ingredients at all three grid offsets. Existing chains, tool batching and settlement tests remain required.

Run spotlessApply, test and build; compare the tested candidate and tagged release except version metadata; require Windows/Linux/macOS CI, release and deployed asset hashes. Install normally only in closed profiles, staging disabled in a running main profile.

Live acceptance remains pending: backpack and Tinkers' attached-storage-only ingredients, full player inventory and overflow, the LV Cutting Machine tool chain, actual server rejection/latency, and rendered help. No new gameplay or speed measurements are claimed.
