# GTNH Machine Worklist

Client-side machine work lists from an existing NEI autocrafting group, targeting GTNH 2.8.4 and NEI 2.8.44-GTNH.

## Development status

Under development. The Windows build and 21 automated tests pass. In GTNH 2.8.4, the worklist opens from a saved NEI group, displays machine batches and reusable inputs, and opens the selected machine recipe in NEI. Multiplayer and Linux/macOS builds still require validation.

## Use

Install the release JAR in a separate GTNH 2.8.4 instance's `mods` directory. Do not install the `-dev` or `-sources` JAR.

Open inventory, hover an existing NEI autocrafting group, and press **F10**. The key can be changed in Minecraft's Controls settings. Alternatively, after opening inventory once, use `/machineworklist` to list group numbers and `/machineworklist 16` to open one.

The default view lists machine operations still needed after accounting for inventory and hotbar. **All steps** includes crafting-table operations. Click a row to inspect full batch inputs and outputs, then **Open NEI recipe** to see the recipe's machine tier and settings. Escape returns to the previous view. **Missing inputs** lists outstanding external ingredients and missing reusable tools. Scroll to see more rows.

Ready counts are evaluated separately for each operation; two ready operations may compete for the same stock. The remaining chain itself allocates inventory globally. Chests, machine inventories, and network storage are not scanned. Plans capture the selected bookmark recipes when opened; reopen after changing the NEI group.

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
