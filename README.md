# GTNH Machine Worklist

[Download and usage guide](https://gtnh-mod.crabanddog.com/) · [First-run walkthrough](docs/quick-start.md) · [Changelog](CHANGELOG.md) · [Releases](https://github.com/benbernard/gtnh-machine-worklist/releases)

Client-side machine work lists from an existing NEI autocrafting group, targeting GTNH 2.8.4 and NEI 2.8.44-GTNH.

## Development status

Beta for GTNH 2.8.4. The automated suite has 88 checks, with CI builds for Windows, Linux, and macOS. Beta.10 prefers bulk intermediates before downstream recipes. Beta.9 adds ingredients and output overflow from open backpack/station storage while preserving bulk transfers. Beta.8 restores bulk readiness for returning crafting tools and removes blanket quiet waits when the grid/cursor is clear. Beta.7 treats reusable programmed circuits as virtual settings and sorts ready machine operations first, largest remaining run count first within ready/waiting groups. Beta.6 adds bounded returned-tool cleanup and waits for inventory to settle before advancing a chain. Beta.5 added remembered navigation, direct Close controls and Tinkers' crafting-station slot handling. Live testing of these new paths is deferred at the user's request. Earlier beta.4 gameplay tests covered multi-stage chains in player, table and backpack grids. See the [returned-tool batching fix](docs/returned-tool-batching.md), [virtual circuit and ordering plan](docs/virtual-circuits-and-machine-order.md), [returned-tool plan](docs/returned-tools-plan.md), [navigation and container plan](docs/remember-position-plan.md), [earlier gameplay audit](docs/final-gameplay-audit.md) and [validation record](docs/validation.md) for exact coverage.

## Use

Install the release JAR in a separate GTNH 2.8.4 instance's `mods` directory. Do not install the `-dev` or `-sources` JAR.

Press **F10** to resume the last unchanged crafting group on the current NEI page, with its recipe detail, tab, ready filter and scroll position. **F10 or Close** exits the whole worklist from any of its screens; **Escape or Back** goes back one level. **Shift+F10** opens the group picker. Positions save per world/server and survive restarts. A changed or missing group opens the picker; a completed recipe returns to its queue. From the world, F10 uses a fresh player inventory; open a real crafting table/backpack first to use its grid and storage. The key can be rebound in Controls.

With no remembered position, F10 opens a hovered or sole NEI autocrafting group, or offers the named picker. After NEI initializes, `/machineworklist` opens the picker and `/machineworklist 16` opens that group directly. The picker includes help and an eight-table example without changing bookmarks. Reopening recalculates inventory and never resumes an old crafting request. Save quantity edits with Enter before closing the stock editor.

**H / Help** in the picker, queue or recipe details opens six in-game topics: getting started, reading the queue, crafting a chain, available stock, blockers and controls. Choose a topic with its button or number key. Text pages show the visible line range and support wheel/arrows, Page Up/Down and Home/End. In the stock editor, **H** keeps its Add half remaining action and **?** opens stock help.

Empty the crafting grid and cursor before opening the worklist; blocked entry explains the requirement over the original container so its items stay in place. Pages with multiple groups offer keyboard selection instead of requiring hover.

The queue header names the active grid. A **Tinkers' Crafting Station** uses its own 3×3 grid even if a closed backpack contains crafting ingredients. Attached chest storage can remain occupied; only the station's grid/result and the held cursor block entry. Worklist crafting uses player inventory/hotbar and the attached storage slots exposed by that open station, respecting their extraction permissions.

**Machines** lists machine operations still needed after accounting for visible inventory. **Crafting** lists crafting-grid recipes; **All** combines them. Crafting-only groups open on Crafting, and tabs show counts. Open a row to inspect inputs, outputs, readable upstream recipes and active blockers. **NEI recipe** shows tier and settings. Escape returns. **Overall missing inputs** covers the whole group; smaller windows use a separate view.

Ready counts are evaluated separately for each operation; two ready operations may compete for the same stock. The remaining chain itself allocates inventory globally. Only storage slots exposed by an open supported backpack or Tinkers' station are included; other chests, machines and network storage are not scanned. Plans capture the selected bookmark recipes when opened; reopen after changing the NEI group.

Keyboard navigation: **Tab / Shift+Tab** focuses buttons; **Enter** activates. **1/2/3** selects tabs; **Up/Down** selects rows; Enter opens recipes or upstream dependencies. **F** crafts one batch of the selected recipe. **G** crafts its selected upstream chain from a recipe detail, or advances the whole group's crafting recipes from the queue. **B** explains recipe status, **I** opens full item information, **N** opens NEI, **R** toggles ready-only, **M** shows missing inputs, **C** opens available stock and **Escape** returns. Holding a crafting key does not repeat its request.

**Available stock** (or **C** while viewing the worklist) lets you enter an exact completed output total or complete half the remaining output, rounded up to whole recipe batches. Counts are items, or mB for fluids. For example, recording 50 completed plates against a 100-plate requirement leaves 50 to make and reduces the upstream chain. Use Previous/Next (or Page Up/Page Down) to select an output, enter a count and press Enter to save, or clear an entry to undo it. Outputs remain editable even when no work remains.

Manual totals mean completed outputs **still available for this chain**, including copies in your inventory; the larger of manual stock and matching visible inventory is used, so picking up recorded items does not count them twice. Verified worklist crafting updates existing records for consumed inputs and produced outputs. Update records yourself after producing or consuming items elsewhere. These are not lifetime production counters. Record coproducts separately, especially chance outputs. Readiness still requires real inventory. Progress is saved locally per world/server and group snapshot; changing the group's recipe choices or quantities starts a fresh record.

Recipe notes flag GregTech chance outputs and matching recipes that NEI leaves unlinked. Chance-output quantities assume success and may need repeated batches. Cycles use NEI's external-input boundary and require starting material; the worklist does not optimize a recycling loop.

See [validation notes](docs/validation.md) for tested cases and outstanding checks.

## Requirements and acceptance plan

- Import the selected NEI autocrafting group, retaining its recipe choices and target quantities. Collapsing a UI section must not hide work.
- Account for player inventory/hotbar once across the entire chain, including existing intermediates and finished outputs.
- Display remaining machine operations, batch counts, inputs, outputs, readiness, and dependencies. Keep crafting-table operations in dependency calculations.
- Handle shared ingredients, batch surplus, multiple outputs, fluids, reusable ingredients, alternative ingredients, and cycles explicitly.
- Provide an understandable scrollable display and recipe navigation; refresh when inventory changes.
- Work on multiplayer clients without requiring a server installation.
- Produce the same portable mod JAR on Windows, Linux, and macOS, using the checked-in Gradle wrappers and pinned build dependencies.
- Verify complex chains with automated tests and the separate GTNH test instance; record evidence and remaining gaps rather than treating a successful compilation as gameplay verification.

## Build

Install a JDK 25 and set JAVA_HOME to it. The GTNH build tools provision compilation toolchains as needed. The mod will target Java 8 bytecode for use with the pack's Java 8 and modern-Java distributions.

Windows: `gradlew.bat build`

Linux/macOS: `./gradlew build`

Do not commit Minecraft files, account data, worlds, or launcher configuration. Test using a separate GTNH 2.8.4 instance.

## Worklist UI and crafting

The current source adds separate Machines, Crafting, and All tabs. Machine rows show ready operations before waiting ones, with largest remaining run counts first within each group. Partially ready operations count as ready; ordering uses total remaining runs, not only the runs currently ready. This also orders machine rows in All and refreshes after inventory or recorded-stock changes. At wide GUI sizes an independently scrollable Overall missing inputs pane stays visible across tabs and recipe details; smaller windows use a Missing inputs view. The pane always covers the complete group, regardless of tab or readiness filter.

Reusable Programmed / Programmable Circuits are virtual machine settings: they never block readiness or appear as missing. Details show their configuration number and explain setting the machine's ghost circuit slot. Real reusable tools/molds and consumed circuit ingredients still require physical items.

Recipe details have **Craft chain (G)**, and crafting recipes also have **Craft 1 batch (F)**. The queue has **Craft group (G)**. A chain request makes the selected upstream ingredients and then continues toward the remaining target; a group request advances all saved crafting recipes, regardless of the visible tab or filter. It uses only the group's chosen NEI recipes. Machines remain manual, although the worklist can craft their prerequisites.

Open the worklist with F10 from the real crafting-table GUI for 3x3 recipes (the inventory grid only supports recipes that fit). NEI autocrafting must be enabled. The current container must still be open, with an empty grid, empty cursor, usable transfer/output space, and matching inputs for the next recipe. Each transfer submits up to 64 batches, then recalculates the chain from inventory, checks prerequisites and verifies output. Short bursts allow Escape to stop further transfers. The result reports completed batches across recipes and whether the chain completed or paused. A chain may make partial progress before it needs machine outputs, more ingredients, a different grid or additional space; fix the reported blocker and start a new request. Recorded stock elsewhere does not act as physical crafting supply.

### Adventure's Backpack crafting

Files, screwdrivers and other reusable ingredients may remain visible briefly after a transfer. While **Returning crafting tools** is shown, the worklist waits for inventory updates and makes bounded attempts to return recognized tools to player inventory. Leave usable space in player inventory or the open supported storage. Recovery never clicks the result slot. If it times out, inspect the cursor/grid and reopen the actual backpack if necessary, then reopen the worklist to recalculate stock. Escape cancels further work during the wait. Tool-using recipes can submit up to 64 batches using the same returning tool, bounded by durability and physical tool slots. A nearly broken matching spare conservatively limits the transfer. Clean tool transfers resume on the next client tick; only actual leftovers need the longer recovery wait. Ordinary recipes retain fast bulk transfers.

The current source supports Adventure Backpack 1.3.13-GTNH through a dedicated NEI overlay that limits grid cleanup to its nine crafting slots. Open the backpack GUI and press F10 to resume your chain, or Shift+F10 to pick another group. Select Crafting and open a recipe. Both crafting actions use the backpack's 3x3 crafting area. Leave its bottom-right nine storage slots empty before starting, keep the cursor empty, and leave usable player or ordinary storage space. Other backpack storage may remain occupied. Opening from the command instead uses the player inventory grid.

Open backpack storage and Tinkers' attached storage provide **both ingredients and output overflow**. When player space is insufficient, starting F/G moves whole player stacks into that open storage to make room for NEI transfers, outputs and returned tools. Existing crafted outputs move first when possible; the held hotbar item stays put. Preview does not move anything. Slot restrictions, stack limits and NBT are respected; backpack grid/mirror/utility slots are never overflow destinations. If both inventories are full or storage refuses the items, crafting pauses with a space/transfer explanation. Transfers remain bulk NEI calls of up to 64 batches. See the [implementation and validation record](docs/open-container-storage.md).

While opened from that backpack, remaining quantities, readiness, missing inputs, and manual completion account for player inventory plus its 48 storage slots. Hidden crafting mirror slots and result/tool/fluid slots are excluded. This does not scan closed backpacks or tank fluids. The compatibility targets the layout shipped in GTNH 2.8.4 and has passed a live handheld-backpack transfer, including storage-only ingredients and preservation of unrelated stored items.

Chain crafting prefers bulk intermediate production before switching downstream: for example, make two stacks of rods, then combine one stack of long rods. Each NEI call still handles at most 64 recipe runs; recipe yield can produce more than 64 items. It makes only the remaining requested quantity. Materials, tool durability and available space can shorten a batch; blocked producers allow ready consumers or independent branches to run.
