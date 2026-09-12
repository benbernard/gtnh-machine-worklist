# First worklist in GTNH 2.8.4

[Download and illustrated guide](https://gtnh-mod.crabanddog.com/) · [Source releases](https://github.com/benbernard/gtnh-machine-worklist/releases)

## Install or update in Prism

1. Right-click your GTNH 2.8.4 instance and choose **Copy** to make a test instance.
2. Close that instance's game. Open **Edit → Mods** and filter for `worklist`.
3. On updates, uncheck the previous GTNH Machine Worklist entry. Use **Add File** to select the normal JAR, excluding `-dev` and `-sources` files. Keep exactly one version enabled.
4. Launch and enter a disposable world. Open survival inventory once to initialize NEI.

## Try the included example

Run `/machineworklist` to open the named group picker. Press **E** or choose **Example: 8 tables**. This plans a total of eight crafting tables using the pack's actual recipe; it changes neither bookmarks nor inventory. Existing tables reduce the remaining quantity.

The GTNH recipe uses two flint and two logs per table. If you own no tables, sixteen of each supplies the example. Obtain these materials normally. The mod never grants ingredients.

The Crafting tab opens automatically. Press **Enter** to view the recipe. **Craft 1 batch / F** runs it once. **Craft all ready / G** runs up to the previewed quantity, bounded by remaining work, physical ingredients and available output space. The result screen reports what completed. **Escape** closes the crafting container and cancels further batches.

Open a real crafting table or supported Adventure Backpack first and use **F10** for 3×3 recipes. Commands use the player inventory's 2×2 grid. Empty the grid and put away any cursor item before opening the worklist; if blocked, the explanation appears over the original container and its items stay in place.

## Build your own NEI group

1. Search NEI for your target, press **R** and choose the recipe you want. Favorite the ingredient recipes you want the tree to follow.
2. Hover the recipe's **heart / Favorite** control. Press **Shift + Bookmark** (**Shift+A** by default); the hotkey tooltip calls this **Save Recipe Tree**. This saves an autocrafting group containing that recipe and the favorite ingredient recipes it can link.
3. Use **Ctrl + mouse wheel** over the bookmarked target to change its quantity. A saved recipe tree already uses Crafting Chain mode. For an ordinary group, right-click its bracket to toggle that mode; the default ungrouped page uses its page header.
4. Open the appropriate inventory/crafting container and press **F10**. A hovered or sole group opens directly. Otherwise use the named picker. Close and reopen after changing NEI recipes or target quantities.

Group numbers are local to your bookmarks. `/machineworklist 16` only works if your current bookmark page contains autocrafting group 16.

## Keyboard access and explanations

- **Tab / Shift+Tab**: move between buttons; **Enter** activates the focused control. Unavailable actions also explain their reason through the keyboard.
- **1 / 2 / 3**: Machines / Crafting / All. Tabs show remaining recipe counts.
- **Arrows**: select or scroll rows. **Enter** opens a recipe or an upstream dependency.
- **I**: open the selected item's full information in a scrollable view. Circuit configuration is also included in item names.
- **N**: open the selected NEI recipe. **B**: full recipe status, or group selection from the queue.
- **R**: toggle ready-only. **M**: overall missing inputs at any window size.
- **C**: available output stock. **Escape**: back.

Recipe details name missing inputs and other active blockers with recovery steps. They also explain when ingredients or inventory space limit bulk crafting. Ready means physical ingredients match; it does not verify machines, voltage or power. Holding a crafting shortcut never queues repeated requests.

## Available stock is not a production counter

Enter the **total output still available to this chain**, including copies in inventory. The editor shows recorded, visible and effectively credited stock and confirms saves. For example, a record of 64 with 64 in inventory credits 64; 64 in a chest plus 32 in inventory should be recorded as 96.

Reduce or clear records after consuming the corresponding intermediates. Manual records reduce remaining work but cannot supply physical crafting ingredients. **H** adds half the remaining batches, rounded up; save zero or use **Clear record** to undo a record. Tab reaches the field and every action; **Page Up / Page Down** selects outputs; **?** opens help.

## Accessibility scope

The mod provides keyboard controls, visible focus, text statuses, scrollable explanations and bounded tooltips. Minecraft's native UI did not expose worklist controls through the tested Windows accessibility tree. Screen-reader support has not been established. The HTML guide is available for reading with browser accessibility tools.
