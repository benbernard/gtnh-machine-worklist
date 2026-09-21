# Multiplayer crafting synchronization

The September 20, 2026 audit found reproducible client synchronization defects. The fixes below address those defects. The intermittent disconnect 10-20 minutes after crafting has not been reproduced, so this is a candidate fix rather than proof of its cause.

## Source versions

The comparison used Machine Worklist main `bd28b55`, NEI `2.8.44-GTNH` (commit `24e46bb43e3820b5ee69fa786cec8b583ddee250`) and Adventure Backpack `1.3.13-GTNH` (commit `9557d1d3665c0c34e788a0bc4fa2fcb7b4fd7c39`). Those dependency versions match the installed GTNH 2.8.4 instance. The installed worklist was beta.13. The candidate has not been installed into that instance.

## What differs from NEI

| Area | Earlier worklist behavior | Candidate behavior |
| --- | --- | --- |
| Recipe transfer | Calls NEI's crafting implementation. Backpack/station subclasses constrain slot discovery | Keeps the same NEI calls and slot adapters, up to 64 runs per transfer |
| Completion | Observes acknowledgments on the network thread, then assumes one client tick applies inventory updates | Observes acknowledgments after vanilla applies them. Requests NEI's full inventory refresh and waits until both container contents and cursor updates are applied |
| Tool cleanup | Extra recovery clicks and a reflective write to the backpack crafting mirror | Uses only NEI's cleanup for crafting tools. Stops for manual inspection if the refreshed grid or cursor still contains items |
| End of request | Switches out of the real crafting GUI | Leaves the real GUI open, as native NEI does. Reports progress in chat |
| NEI recipe browsing | A plain worklist screen becomes NEI's return target | Opens NEI from the original real container, allowing NEI's normal return path to restore it |
| Storage overflow | Moves items and immediately crafts against the predicted inventory | Waits for confirmation and a full inventory refresh after moves, then recalculates before crafting |

NEI already supplies the transfer algorithm at runtime. Copying it into this project would create another implementation to maintain. The backpack and station subclasses remain because their grid geometry and storage boundaries differ from an ordinary crafting table.

Native NEI's worker does not provide this acknowledgment/refresh gate. The worklist keeps it because subsequent recipe choices and recorded stock depend on confirmed outputs. The additional round trip can reduce throughput on high-latency servers.

## Confirmed failure mechanisms

Vanilla sends a successful click acknowledgment before the resulting slot updates. Also, Minecraft's receive loop applies at most 1,001 queued packets per call. An acknowledgment observed by a Netty handler can still be waiting in the client queue when the old one-tick gate opens. Deterministic fixtures reproduced both cases.

NEI recipe screens temporarily replace the client's active container with a recipe container. Returning to a plain worklist did not restore the original container. That can make vanilla ignore real-window slot updates and omit its acknowledgment of a rejected transaction. Opening NEI from the real container preserves NEI's normal return target instead. A fixture now checks both an applied real-window slot update and vanilla's rejection reply.

The new packet observer wraps only inbound inventory responses immediately before NetworkManager. It delegates each original packet to vanilla before observing completion, preserves priority, and never serializes the wrapper onto the connection. Removing the observer does not discard responses already queued. A rejected transfer stops the worklist. Vanilla still handles its rejection protocol.

The refresh uses NEI's existing `REQUEST_CONTAINER` message. NEI's server handler sends full container contents through vanilla, followed by the cursor stack. The observer requires the original window ID, expected slot count and cursor update. This protocol has no request ID: the gate accepts a qualifying full update after all click acknowledgments are applied. It cannot distinguish the requested response from another full server update in that interval. Leaving the real GUI open and removing extra cleanup avoids acting on a supposedly finished hidden grid.

## Backpack without its GUI visible

A worklist can cover an open backpack while its container remains active. That is different from a closed backpack retained as a Java object. Starting a request now requires that the source is still the player's exact active container and that the current screen is that container or its own worklist. The real crafting GUI is displayed before any transfer.

F10 from the world and `/machineworklist` create a fresh player inventory, which provides a 2x2 grid. If a non-player container was left active without its GUI, entry first closes it through the normal client/server close path. The command queues its intent until chat has closed. It no longer initializes an inventory GUI while chat is still active. Matching numeric window IDs alone cannot authorize reuse of an old backpack.

## Second audit

The follow-up review covered every worklist GUI transition and crafting click path. It found and fixed two further problems: leaving a stale worklist could hide the GUI without closing its server window, and overflow moves could feed the next recipe before server confirmation. It also added player/connection identity checks, input guards during a request, and observer cleanup if startup fails. A competing click after a batch is sealed invalidates the request. It cannot be credited as worklist output.

No persistent timer or background crafting worker was found in the worklist. Closing/changing the container, losing the player/connection, rejection or timeout ends the session and removes its observer. This does not identify the cause of the delayed kick. Vanilla transaction rejection by itself is not a disconnect explanation.

## Validation

The regression fixtures use the pinned Minecraft/Forge/NEI classes, a real NetworkManager receive queue and real vanilla container packet handlers. Rendering is stubbed for GUI lifecycle tests. They cover delayed slot responses, a queue exceeding two receive ticks, wrong-window/size snapshots, rejection, observer removal with queued packets, recipe return, world/command entry, stale containers, timeout and cancellation. Existing planning, slot-boundary, tool-durability and storage tests remain part of the full suite.

Validation on September 20, 2026: `./gradlew.bat spotlessApply test build --console=plain` passed on Windows with Java 25. The full suite ran 92 tests, with zero failures, errors or skips. Formatting and Checkstyle checks passed. The packaged JAR includes the new observer and excludes removed recovery classes and test fixtures.

Candidate JAR: `machineworklist-0.1.0-beta.13-codex-multiplayer-crafting-sync.1+bd28b55c20-dirty.jar`. SHA-256: `1f512ba80cab0795cc4aa527c470bc09a52c25e0b647ba17c04033f92f49d7f1`. This is a local candidate built from the uncommitted changes on `codex/multiplayer-crafting-sync`, based on `bd28b55`.

The candidate was tested in the full GTNH 2.8.4 client with its integrated server. Three table trials per engine produced identical correct saved outputs: native NEI median 1.54 seconds, candidate median 2.96 seconds. Three candidate backpack trials passed at a median 3.71 seconds. Native NEI failed all three backpack trials with rejected transactions and incomplete output. A separate live keyboard test verified world entry, 2x2 grid limits, backpack entry, NEI recipe return, crafting completion and F10 after closing the backpack. See [the live comparison](live-crafting-comparison.md) for the method, individual results and limitations.

No live remote multiplayer session or prolonged post-crafting connection test was performed for this candidate. The intermittent delayed kick remains unverified.

## Primary source references

- [NEI transfer and cleanup](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.44-GTNH/src/main/java/codechicken/nei/recipe/DefaultOverlayHandler.java)
- [NEI native crafting worker](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.44-GTNH/src/main/java/codechicken/nei/recipe/AutoCraftingManager.java)
- [NEI recipe-screen return](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.44-GTNH/src/main/java/codechicken/nei/recipe/GuiRecipe.java)
- [NEI refresh request](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.44-GTNH/src/main/java/codechicken/nei/NEICPH.java) and [server response](https://github.com/GTNewHorizons/NotEnoughItems/blob/2.8.44-GTNH/src/main/java/codechicken/nei/NEISPH.java)
