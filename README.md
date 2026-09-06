# GTNH Machine Worklist

Client-side machine work lists from an existing NEI autocrafting group, targeting GTNH 2.8.4 and NEI 2.8.44-GTNH.

## Development status

Under development. The latest Windows, Linux, and macOS builds pass all 26 automated tests, including manual completion, and produce identical mod JARs. Live GTNH 2.8.4 checks verify selected-group import, inventory deduction, circuit-dependent readiness, and NEI recipe navigation. Manual completion, the death-screen fix, and multiplayer behavior still require gameplay validation.

## Use

Install the release JAR in a separate GTNH 2.8.4 instance's `mods` directory. Do not install the `-dev` or `-sources` JAR.

Open inventory, hover an existing NEI autocrafting group, and press **F10**. The key can be changed in Minecraft's Controls settings. Alternatively, after opening inventory once, use `/machineworklist` to list group numbers and `/machineworklist 16` to open one.

The default view lists machine operations still needed after accounting for inventory and hotbar. **All steps** includes crafting-table operations. Click a row to inspect full batch inputs and outputs, then **Open NEI recipe** to see the recipe's machine tier and settings. Escape returns to the previous view. **Missing inputs** lists outstanding external ingredients and missing reusable tools. Scroll to see more rows.

Ready counts are evaluated separately for each operation; two ready operations may compete for the same stock. The remaining chain itself allocates inventory globally. Chests, machine inventories, and network storage are not scanned. Plans capture the selected bookmark recipes when opened; reopen after changing the NEI group.

**Record completed** (or **C** while viewing the worklist) lets you enter an exact completed output total or complete half the remaining output, rounded up to whole recipe batches. Counts are items, or mB for fluids. For example, recording 50 completed plates against a 100-plate requirement leaves 50 to make and reduces the upstream chain. Use Previous/Next (or Page Up/Page Down) to select an output, enter a count and press Enter to save, or clear an entry to undo it. Outputs remain editable even when no work remains.

Manual totals mean completed outputs **still available for this chain**, including copies in your inventory; the larger of manual stock and matching visible inventory is used, so picking up recorded items does not count them twice. These are not lifetime production counters: reduce or clear an intermediate's entry after consuming it in a later step. Record coproducts separately, especially chance outputs. Readiness still requires real inventory. Progress is saved locally per world/server and group snapshot; changing the group's recipe choices or quantities starts a fresh record.

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

## Updated worklist UI (source build)

The current source adds separate Machines, Crafting, and All tabs. At wide GUI sizes an independently scrollable Overall missing inputs pane stays visible across tabs and recipe details; smaller windows use a Missing inputs view. The pane always covers the complete group, regardless of tab or readiness filter.

Crafting recipe details have Craft 1 batch via NEI. Open the worklist with F10 from the real crafting-table GUI for 3x3 recipes (the inventory grid only supports recipes that fit). NEI autocrafting must be enabled. The current container must still be open, with an empty grid, empty cursor, a free inventory slot, and matching inputs. The button returns to that container and asks NEI to craft one recipe batch; it does not run machines or automatically execute the entire group. Reopen the group to see resulting inventory changes. This new UI/action still needs live verification; the website download and screenshots are the older 316542f build.

### Adventure's Backpack crafting

The current source supports Adventure Backpack 1.3.13-GTNH through its existing NEI overlay. Open the backpack GUI, hover the NEI group, press F10, select Crafting, and open a recipe. Craft 1 batch via NEI uses the backpack's 3x3 crafting area. Leave its bottom-right nine storage slots empty before starting, keep the cursor empty, and leave a free player-inventory slot. Other backpack storage may remain occupied. Opening from the command instead uses the player inventory grid, not a closed backpack.

While opened from that backpack, remaining quantities, readiness, missing inputs, and manual completion account for player inventory plus its 48 storage slots. Hidden crafting mirror slots and result/tool/fluid slots are excluded. This does not scan closed backpacks or tank fluids. The compatibility targets the layout shipped in GTNH 2.8.4 and still needs a live item-transfer check.
