# Validation record

## Automated checks

Windows, JDK 25, `gradlew.bat spotlessApply build`: passes, 21 tests, no failures or skips.

Seven stock-allocation tests exercise finite quantities, overlapping alternatives, reusable molds, fluid-sized quantities, and integer limits. Fourteen integration fixtures run the actual pinned NEI 2.8.44-GTNH chain calculator in a Forge classloader. They cover owned intermediates and finished targets, shared branches, phantom bookmark stock, repeated calculations, coproducts, multiple targets sharing batch surplus, finite fluid containers, stock split across slots, an 80-stage chain cut at an owned intermediate, missing reusable tools, cyclic recipes requiring external starting material, and consumed and reusable ingredients with required metadata/NBT configuration.

## Live GTNH 2.8.4 check

Testing uses a separate Prism instance and a disposable local world. No production world or multiplayer server has been modified.

- The client starts with the mod installed.
- F10 over a saved NEI autocrafting group opens its worklist.
- A steam-oven chain with an empty material inventory shows 16 assembler runs for high-pressure boiler tanks, 164 steel-plate bending runs, and 32 wrought-iron-plate bending runs.
- All-steps mode includes four crafting batches producing 16 steam ovens.
- The steel-plate detail shows 164 steel ingots and a reusable programmed circuit.
- Opening the recipe reaches the NEI bending-machine recipe; Escape returns to the worklist.
- Command opening works after NEI bookmarks have loaded.

## Outstanding checks

- Recheck empty-list rendering, tooltips, scroll indicators, and recipe notes after installing the latest build.
- Verify in-game inventory changes reduce the expected machine batches and change readiness.
- Validate larger actual GTNH groups, fluids, probabilistic outputs, reusable tools, cycles and unavailable handlers.
- Check smaller screen layouts and scrolling.
- Validate a multiplayer connection with no server counterpart.
- Run the checked-in Linux/macOS/Windows CI matrix after GitHub write authentication is available. Only Windows has been executed so far.
- Local Ubuntu WSL was detected, but cannot start because virtualization is disabled; no Windows system configuration was changed.

Compilation and synthetic integration tests do not establish full gameplay or cross-platform compatibility.
