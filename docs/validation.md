# Validation record

## Automated checks

Windows, JDK 25, `gradlew.bat spotlessApply build`: passes, 26 tests, no failures or skips, including manual completion.

Five manual-completion integration cases verify upstream reduction, inventory overlap without double counting, batch rounding, exact partial output counts, clearing/undo, saved-progress reload, fluid mB, and shared branch allocation. The completion editor and death-screen fix still need live gameplay validation; they are newer than the CI artifact tested below.

GitHub Actions [run 33991055598](https://github.com/benbernard/gtnh-machine-worklist/actions/runs/33991055598), commit `8b60fda4c546a4316443834d99aff80190acf2f7`: the build-and-test steps passed on `windows-latest`, `ubuntu-latest`, and `macos-latest`. Downloaded reports independently confirm 21 tests, zero failures, and zero skipped tests on each platform. All three production JARs have the same SHA-256: `911894b8697ff9f6216e524003778373e9e3d4b2473be5690264e1e29ab3bbda`.

Seven stock-allocation tests exercise finite quantities, overlapping alternatives, reusable molds, fluid-sized quantities, and integer limits. Fourteen integration fixtures run the actual pinned NEI 2.8.44-GTNH chain calculator in a Forge classloader. They cover owned intermediates and finished targets, shared branches, phantom bookmark stock, repeated calculations, coproducts, multiple targets sharing batch surplus, finite fluid containers, stock split across slots, an 80-stage chain cut at an owned intermediate, missing reusable tools, cyclic recipes requiring external starting material, and consumed and reusable ingredients with required metadata/NBT configuration.

### Latest cross-platform verification

GitHub Actions [run 33993150298](https://github.com/benbernard/gtnh-machine-worklist/actions/runs/33993150298), commit `316542f3db3489b83619c4dd76e8fdcf4cf78a85`: downloaded reports confirm 26 tests, zero failures, and zero skips on Windows, Ubuntu, and macOS. All three production `machineworklist-316542f.jar` artifacts have SHA-256 `6532b16ee4c2dc2426c9a4727c00e9e79f132beceaa5ce64d97d896f6ffed5f1`. This includes the manual completion implementation and death-screen fix; it does not validate their rendered UI.

On the next remote gameplay attempt, Minecraft window capture failed twice with `window capture timed out: timed out waiting on channel`, including after refreshing the returned window selection. Native input was stopped according to the Computer Use recovery instructions. The user is away; no physical interaction is assumed or required for automated build checks. Fresh gameplay screenshots and remaining interactive checks are still outstanding.

## Live GTNH 2.8.4 check

Testing uses a separate Prism instance and a disposable local world. No production world or multiplayer server has been modified.

- The client starts with the mod installed.
- F10 over a saved NEI autocrafting group opens its worklist.
- A steam-oven chain with an empty material inventory shows 16 assembler runs for high-pressure boiler tanks, 164 steel-plate bending runs, and 32 wrought-iron-plate bending runs.
- All-steps mode includes four crafting batches producing 16 steam ovens.
- The steel-plate detail shows 164 steel ingots and a reusable programmed circuit.
- Opening the recipe reaches the NEI bending-machine recipe; Escape returns to the worklist.
- Command opening works after NEI bookmarks have loaded.

### CI artifact inventory checks (2026-09-05)

Rechecked the installed `8b60fda` CI artifact in the disposable New World, using saved group 16. With no materials, the machine queue showed 16 assembler runs, 164 steel-plate bending runs, and 32 wrought-iron-plate bending runs. Adding 64 steel plates reduced only the steel bending quantity to 100. Adding 64 steel ingots kept 100 runs remaining and readiness at zero without the circuit. Adding one programmed circuit with configuration 1 made exactly 64 steel bending runs ready and sorted that operation first.

The test player subsequently died to a stray while the screen was open. The worklist detected the lost inventory and returned to 164 steel bending runs, but the death screen was only visible after closing the worklist. Further local testing should use a protected creative player. Automated pointer movement is currently unreliable in both the worklist and Minecraft's native respawn menu; button/filter checks are not recorded as passing. Keyboard navigation and client commands worked.

## Outstanding checks

- Recheck empty-list rendering, tooltips, scroll indicators, and recipe notes after installing the latest build.
- Broaden inventory-change checks beyond the verified steam-oven plate/ingot/circuit sequence.
- Check death handling while the worklist is open.
- Validate larger actual GTNH groups, fluids, probabilistic outputs, reusable tools, cycles and unavailable handlers.
- Check smaller screen layouts and scrolling.
- Validate a multiplayer connection with no server counterpart.
- Native gameplay has only been exercised on Windows. The CI matrix verifies compilation, packaging, and the automated calculation tests, not rendered gameplay on Linux/macOS.

Local Ubuntu WSL was detected but cannot start because virtualization is disabled; Linux build validation therefore used GitHub Actions. No Windows system configuration was changed.

Compilation and synthetic integration tests do not establish full gameplay or cross-platform compatibility.
