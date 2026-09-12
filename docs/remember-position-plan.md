# Resume the last worklist

F10 should resume the last valid chain, including the selected recipe and position, instead of opening a new picker every time. Shift+F10 deliberately chooses another group. F10 from a worklist closes it without backing out of the selected recipe; F10 from the world uses a fresh player inventory. Existing container-entry and crafting checks still apply.

Remember tab, ready filter, missing-input view, selected recipe identity, list/detail scroll, missing-pane scroll and keyboard selection. Store navigation locally per world/server so restarting the client can resume an unchanged group on the current NEI page. Reimport current bookmarks and recalculate physical inventory on every reopening. Never retain or restore a container, inventory snapshot or active crafting request.

If the group changed, moved to another NEI page or was removed, open the picker. If the selected recipe completed, return to its queue with positions clamped to the remaining rows. Malformed or unwritable navigation files must not prevent using the mod.

## Tinkers' crafting-station report

The user reported an occupied-grid warning at a Tinkers' table and found an item in their backpack grid. The current version checked every occupied non-player slot, incorrectly treating attached station storage as crafting slots. Inspection of the installed TConstruct 1.13.57-GTNH JAR confirms the station uses an InventoryCrafting subclass, a SlotCrafting result, and chest-dependent grid offsets of 0, 122 or 134 pixels. The NEI station overlay adjusts that offset during transfer; its preview may still use an earlier/default offset.

Use actual grid/result types for entry checks, preserve attached storage, and map the station's nine input slots explicitly for both previews and transfers. Supplies remain player inventory/hotbar, matching the planner's scope. Only the currently active container may be used; navigation stores no container or inventory. The queue names the grid, and warnings identify the backpack when applicable. The reported closed-backpack preference has not been reproduced live; active-container checks prevent stale references from being selected.

## Validation scope

61 automated checks cover persistence, malformed state, world isolation, recipe/quantity identity, inventory-independent navigation identity, station storage versus grid/result occupancy, rejection of closed containers, all three station grid offsets, exact mapping/cleanup bounds, and player-only station supplies. Grid fixtures use Minecraft containers/inventories and the real NEI overlay engine, with layouts derived from the installed TConstruct JAR; they do not run the full TConstruct client or a server transfer.

The user explicitly stopped computer use while playing Minecraft. No beta.5 GUI/gameplay checks were performed. Live queue/detail scrolling, F10 close/reopen, Shift+F10, world entry, restart, stock/help Close and table/backpack transitions remain pending, as do live station transfers. Earlier beta.4 evidence is not proof of those new paths.

Build and publish the checked artifact. Keep the running production profile untouched until its client is closed; stage the update separately if necessary. No gameplay inventory, bookmarks or stock fixtures were modified in this pass.
