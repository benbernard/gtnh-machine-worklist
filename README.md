# GTNH Machine Worklist

[Download and usage guide](https://gtnh-mod.crabanddog.com/) · [First-run walkthrough](docs/quick-start.md) · [Changelog](CHANGELOG.md) · [Releases](https://github.com/benbernard/gtnh-machine-worklist/releases)

Client-side machine work lists from an existing NEI autocrafting group, targeting GTNH 2.8.4 and NEI 2.8.44-GTNH.

## Development status

Beta for GTNH 2.8.4. Windows, Linux, and macOS CI exercise 33 automated checks. Live Windows gameplay verifies one-batch and bulk player-grid crafting, remaining-demand and ingredient limits, occupied-cursor/grid preservation, named group selection, visible keyboard focus, and available-stock editing. Earlier builds also passed a handheld Adventure Backpack transfer and a client-only EMBU connection. Three-column transfers, full-inventory handling, multiplayer crafting and wider platform gameplay still need live acceptance checks. See [validation](docs/validation.md) for exact revisions and coverage; a successful build is not a claim of full pack compatibility.

## Use

Install the release JAR in a separate GTNH 2.8.4 instance's `mods` directory. Do not install the `-dev` or `-sources` JAR.

Open inventory and press **F10** to open a hovered or sole NEI autocrafting group, or choose a named group. The key can be changed in Minecraft's Controls settings. After opening inventory once, `/machineworklist` opens the picker and `/machineworklist 16` opens that group directly. The picker includes first-run help and an example targeting eight crafting tables without changing bookmarks.

Empty the crafting grid and cursor before opening the worklist; blocked entry explains the requirement over the original container so its items stay in place. Pages with multiple groups offer keyboard selection instead of requiring hover.

**Machines** lists machine operations still needed after accounting for visible inventory. **Crafting** lists crafting-grid recipes; **All** combines them. Crafting-only groups open on Crafting, and tabs show counts. Open a row to inspect inputs, outputs, readable upstream recipes and active blockers. **NEI recipe** shows tier and settings. Escape returns. **Overall missing inputs** covers the whole group; smaller windows use a separate view.

Ready counts are evaluated separately for each operation; two ready operations may compete for the same stock. The remaining chain itself allocates inventory globally. Chests, machine inventories, and network storage are not scanned. Plans capture the selected bookmark recipes when opened; reopen after changing the NEI group.

Keyboard navigation: **Tab / Shift+Tab** focuses buttons; **Enter** activates. **1/2/3** selects tabs; **Up/Down** selects rows; Enter opens recipes or upstream dependencies. **F** crafts one batch; **G** crafts all ready up to the previewed quantity. **B** explains recipe status, **I** opens full item information, **N** opens NEI, **R** toggles ready-only, **M** shows missing inputs, **C** opens available stock and **Escape** returns. Holding a crafting key does not repeat its request.

**Available stock** (or **C** while viewing the worklist) lets you enter an exact completed output total or complete half the remaining output, rounded up to whole recipe batches. Counts are items, or mB for fluids. For example, recording 50 completed plates against a 100-plate requirement leaves 50 to make and reduces the upstream chain. Use Previous/Next (or Page Up/Page Down) to select an output, enter a count and press Enter to save, or clear an entry to undo it. Outputs remain editable even when no work remains.

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

Crafting recipe details have **Craft 1 batch** and **Craft all ready**. Open the worklist with F10 from the real crafting-table GUI for 3x3 recipes (the inventory grid only supports recipes that fit). NEI autocrafting must be enabled. The current container must still be open, with an empty grid, empty cursor, a free inventory slot, and matching inputs. The buttons return to that container and ask NEI to craft one batch or the previewed ready quantity, checking prerequisites before each batch. A result screen reports completed batches and the stopping reason, then returns to the refreshed worklist. Machines remain manual and bulk crafting applies only to the selected recipe.

### Adventure's Backpack crafting

The current source supports Adventure Backpack 1.3.13-GTNH through a dedicated NEI overlay that limits grid cleanup to its nine crafting slots. Open the backpack GUI, hover the NEI group, press F10, select Crafting, and open a recipe. Both crafting actions use the backpack's 3x3 crafting area. Leave its bottom-right nine storage slots empty before starting, keep the cursor empty, and leave a free player-inventory slot. Other backpack storage may remain occupied. Opening from the command instead uses the player inventory grid, not a closed backpack.

While opened from that backpack, remaining quantities, readiness, missing inputs, and manual completion account for player inventory plus its 48 storage slots. Hidden crafting mirror slots and result/tool/fluid slots are excluded. This does not scan closed backpacks or tank fluids. The compatibility targets the layout shipped in GTNH 2.8.4 and has passed a live handheld-backpack transfer, including storage-only ingredients and preservation of unrelated stored items.
