# Beta.10 live audit - 2026-09-12

This is an execution checklist, not a claim that pending tests passed. The user has resumed computer-use authorization.

## Incident investigation

- [x] Correlate client reset/reconnect with Reizor normal server log, accounting for the server clock being three hours ahead of the client log.
- [x] Confirm beta.6 was the running client at the reported incident; newer staged builds were not loaded.
- [x] Check normal server log for restart/kick. On reconnect the server replaced the old session as still logged in; no corresponding packet-rate kick or restart is recorded.
- [x] Streamed the user-supplied 140,738,560-byte Forge log (2,426,541 lines, 119 midnight rollovers). Isolated the final day and both incident windows. Today contains one NetworkDispatcher read timeout at server 12:58:53; no exception at 15:12:48. World-save diagnostics continue every 45 seconds across both incident windows. No logged packet-rate kick, server restart, out-of-memory error or worklist exception was found in those windows. Twelve dispatcher errors predate the first recorded worklist client handshake, some by over 100 midnight rollovers. Root cause remains unresolved; this does not exclude a network/proxy failure or an indirect mod contribution.

## Release activation

- [x] Main profile stopped; beta.6 retained disabled and beta.10 enabled. SHA-256: 492b4869a05bdb10966afd0d705edff67108c83ba8424a8e77b85efb6f49d555.
- [x] Test profile has the same beta.10 JAR enabled.

## Pending live checks

Use the disposable test world for creative commands and staged inventories. Do not spawn items or modify shared server settings for testing.

- [ ] F10 resume: chain, detail, tab/filter/scroll; F10/Close exits directly; Escape backs one level; Shift+F10 group picker; restart persistence.
- [ ] Tinkers' station uses its own empty grid while a closed backpack grid is occupied; attached storage supplies ingredients.
- [ ] Backpack storage supplies ingredients; mirror/utility/grid slots are excluded from overflow; full player inventory can use ordinary storage.
- [ ] Tinkers' attached storage provides output workspace when player inventory is full; full-storage refusal is clear.
- [ ] Files/screwdrivers return with damage preserved and no ghost blocker; chained bulk tool recipes complete.
- [ ] Upstream rods are produced in large batches before long rods; exact targets/yields and space-pressure fallback; capture timings.
- [ ] Virtual programmed circuit is not missing; other physical tools remain required.
- [ ] Machine list ready first, then descending remaining runs within each section.
- [ ] Rendered Help and recipe blocker explanations are readable; navigation/stock regressions.
- [ ] Multiplayer login, real-stock worklist/crafting and post-crafting play; correlate fresh client/server logs. A short clean session cannot disprove an intermittent reset.

Record actual tested steps, inventory before/after, elapsed times, screenshots and limitations as work proceeds. Automated fixtures are not substitutes for these gameplay observations.

## Executed beta.10 observations

- Launched the exact beta.10 test profile and loaded disposable New World. Initial F10 before NEI inventory initialization showed no groups; returning to inventory initialized bookmarks and F10 then listed groups. Track this onboarding gap for repair.
- Group 16 Machines rendered ready steel bending (52 runs), then waiting wrought-iron bending (32), then waiting assembler (8). Wide layout has no control/missing-pane overlap.
- Selected steel-bending detail survived F10 close/reopen. Closing nested help with F10 returned directly to inventory; F10 from the world restored the underlying selected detail. Escape from help topic returned one level.
- Removed the sole physical integrated circuit with a local-world clear command (confirmed one item removed). F10 still restored steel bending at 52 ready runs and explained virtual configuration 1 as always available.
- Read rendered Crafting a chain and Containers and controls help: all lines fit at maximized size, explaining bulk intermediates, tool durability, exposed storage, overflow bounds and close/resume.

- Ready-only filtering retained the single ready bending row and survived F10 reopening. Automated Shift+F10 injected F10 but did not establish the modifier; keep that shortcut pending rather than counting it as passed.
- Normal client shutdown saved the world. Prepared isolated inventory/bookmark fixture with backups: full player inventory, iron file/hammer, 128 stored iron ingots, one occupied real backpack grid slot; groups 90/91/92 for rods, long rods, fences. No shared-server changes. The next launch displayed a black world with an OpenGL 1286 warning; rendered user interface remains responsive. Investigating display recovery before relying on spatial interaction.
- Display recovery: F11 fullscreen produced an all-black capture; returning to windowed restored HUD but not world rendering. Native F3+T input toggled debug/chat instead of proving a resource reload, so that chord is not counted. Closed the test client normally (world-save log 13:32:19), temporarily set only its options.txt fboEnable=false, and relaunched. The original options backup is preserved for restoration.
- Corrected the black-world diagnosis: setting local time to day restored world rendering immediately. The temporary framebuffer setting has been restored to true; no renderer fix is justified.
- Placed/opened the backpack. F10 correctly warned about its genuinely occupied bottom-right crafting grid, then correctly warned about the carried cursor item during cleanup. Moving that item into ordinary storage cleared the blockers and F10 resumed the remembered group/filter.
- Controlled group91 resolved 128 rod runs then 64 long-rod runs, with backpack-only iron counted and no missing external inputs. G crafted 64 rods, then stopped at 13:43:10 with Tool return did not settle. At stop, 32 rods remained in each outer grid slot, the hammer had returned to player inventory, and 64 iron remained in storage. The staged file had only 25,600 durability (64 uses), so its exhaustion explains the early producer switch; it does not establish a producer-order bug. Manually placing the hammer between the rods produced the correct long-rod output.
- Preserved failed-run logs and saved NBT under ignored build/validation/2026-09-12-beta10-live/stalled-pass. Added local active-request and backpack-transfer diagnostics; 88 tests/build pass. Test-only diagnostic JAR installed for the next run, with tools staged at 102,400 durability. Main profile remains published beta.10.
- Diagnostic pass1: two 64-run rod transfers at 13:51:20/21, then a 64-run long-rod transfer at 13:51:22. Logged pre-output grid was correct (65=64 rods, 66=hammer, 67=64 rods, 99=long rod result), NEI returned true, but settlement stopped after 11,799 ms with only 128 verified rod batches. Closing/reopening retained the leftovers. Manual single output pickup succeeded; saved NBT then confirmed exactly 1 long rod, 126 rods and returned hammer. No automatic long rods were produced. Added before/after cleanup diagnostics for next run; tests/build pass again. No pacing/behavior change yet.
- Diagnostic pass2: expanded trace at 14:04:04-06 shows the client predicts 64 + 64 rods, then all 64 long rods, and successful cleanup with two empty player slots. The later authoritative inventory contains rods again and no long rods. This rules out the immediate output-space hypothesis and establishes client/server disagreement; transaction-level diagnostics are being added only for the disposable integrated-server world.
- Diagnostic pass3: local-only packet trace proves rejected transactions 27 and 53 (file shift-cleanup after each successful 64-rod craft), then 67 (picking up the hammer before placing it for long rods). The server drops subsequent queued clicks until the normal rejection acknowledgment. Tool-state comparison is the next diagnostic; no behavior change has been made.
- Passive trace pass4 reproduces the failure with all diagnostic display-name calls performed on copies. File cleanup transaction 24 is correct (25,600 damage), but recovery transaction 27 uses a stale 0-damage file; likewise 50 is correct (51,200) while recovery 53 uses 25,600. The hammer pickup mismatch is the synthetic fixture: client has normal Sharpness I while the staged server tool lacks it. Corrected hammer fixture metadata. Implemented vanilla acknowledgment tracking before settlement/recovery/next batch; retains 64-run bulk transfers and stops explicitly on server rejection. Awaiting build and live verification.
- Acknowledgment fix live pass: 14:26:05/06 two 64-rod transfers, then 14:26:07 one 64-long-rod transfer; completed 192 verified runs across 2 recipes in 2,936 ms, zero rejected acknowledgments. Saved NBT in ack-pass1 confirms exactly 64 long rods, no rods/ingots, no grid leftovers; file damage 51,200 and hammer damage 25,600. Full inventory was supported through ordinary backpack storage. Prepared station+attached chest fixture with region backup before-station-region.mca; all supplies in chest, player full, closed backpack grid occupied.
- Station live pass at 14:39:24-27: cold F10 restored group91 before any manual inventory opening. Player 2x2 context did not count closed-backpack supplies. Opening the station with an occupied closed backpack grid succeeded; context showed attached storage, 64 rod runs ready. G completed 192 runs in 2,949 ms, using two 64-rod transfers then 64 long rods. Player began full; attached chest supplied iron and accepted stashed inventory. Grid ended empty, both tools returned, worklist reported no remaining work. Revised blocked-work help renders all 23 wrapped lines maximized; small-window wheel scrolling works. Native End/Page Down input did not establish those shortcuts, so do not count them as live passes.

## Candidate validation and remaining limits

- 91 automated tests passed locally and on Windows, Linux and macOS (PR #12 CI run 34720750099). The three platform runtime JARs are byte-identical. Temporary packet tracing was removed; only compact active-crafting start/batch/completion logging remains.
- Original disposable-world level, player, region, bookmarks and options were restored after preserving the live-test evidence in ignored build/validation. Main profile was not used for fixtures.
- Multiplayer candidate login succeeded at 14:49. Cold F10 listed existing groups; the equipped Adventure Backpack opened normally and F10 counted its exposed storage. One real stick recipe run completed at 14:51:04 in 283 ms. Stored oak planks decreased from 4 to 2, player sticks increased from 4 to 6, and the crafting grid was empty. Closing and reopening the backpack retained the result; F10 restored the selected detail with one fewer remaining run.
- The checkbox list above intentionally retains combined checks that are only partly verified. Live passes cover F10 detail/filter resume and direct close, virtual circuits, machine ordering, both container ingredient/overflow paths, valid file and hammer returns, producer-first bulk chains, cold initialization and help rendering. Exact scroll-position resume, Shift+F10, End/Page Down, full-storage refusal, and screwdriver-specific recovery were not all established by this session's native inputs. Existing automated coverage is not a substitute for those uncompleted live variants.
- No conclusion that the delayed connection resets are fixed follows from the inventory acknowledgment repair or a short successful multiplayer session.
