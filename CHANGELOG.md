# Changelog

## 0.1.0-beta.6

- Wait for tool-return and inventory updates before advancing or completing a crafting request, including first, partial and final transfers. Defer output/stock verification until settlement; ordinary empty-grid recipes retain fast bulk transfers.
- Refresh Adventure Backpack's hidden crafting mirror through its own synchronization method and make bounded attempts to return recognized tools from real input slots or the cursor. Preserve tool stacks and metadata-selected types; never click the result or unrelated storage during recovery.
- Explain the wait, required inventory space and timeout recovery in game. Add tests for late updates, recovery/deadline bounds, tool identity and slot boundaries. Live returned-tool gameplay remains unverified while computer use is paused.

## 0.1.0-beta.5

- F10 resumes the last unchanged group and its recipe, tab, filter, keyboard selection and scroll positions; navigation saves per world/server across client restarts.
- F10 or Close exits the entire worklist from recipe details, stock, help and status without backing out. Escape/Back remains single-level navigation; details have a separate Back to queue button.
- Shift+F10 opens the group picker. F10 from the world creates a fresh player inventory; reopening always reimports the plan and checks current inventory/container state.
- Changed or missing groups fall back to the picker. Completed recipe details return to the queue; malformed navigation files do not block use.
- Recognize Tinkers' Crafting Station grid slots separately from attached chest storage, which no longer causes a false occupied-grid warning. Resolve the shifted 3×3 grid for every preview/transfer and restrict supplies to the player inventory used by the plan.
- Use the currently active container when opening, reject stale container references, identify the grid in the queue and make backpack entry warnings specific to its crafting area.

## 0.1.0-beta.4

- Add six in-game help topics and a visible Help button in the picker, queue and recipe details. Explain setup, controls, chain scope, stock and recovery; show the visible line range on text pages.
- Preserve recorded off-inventory stock while verified crafting adds outputs or consumes tracked intermediates. Persist the corrected total and keep records unchanged if saving fails.
- Allow brief backpack cursor/grid synchronization to settle between transfers. Persistent leftovers keep the real container open, with a result in local chat, so changing screens cannot discard them.
- Add regression coverage for external target stock, consumed/produced records, persistence and failed-save rollback.
- Return to the world when NEI has closed the original container, and keep wide-screen queue controls clear of the missing-input heading.

## 0.1.0-beta.3

- Craft chain (G) now makes selected upstream crafting ingredients and continues into downstream recipes, recalculating from inventory after each transfer.
- Add Craft group (G) to advance all selected crafting recipes from the queue. Machine operations remain manual; a partially finished chain reports its blockers and can be resumed with a new request.
- Keep Craft 1 batch (F), NEI's fast bulk transfers, inventory/output checks, backpack slot boundaries and cancellation between bursts.
- Bound every recipe to its original remaining demand, preserve selected-branch scope and existing stock, and stop instead of repeatedly replenishing consumed outputs.
- Add nine integration checks for multi-stage chains, shared stock/surplus, isolated recipe requests, manual stock, machine boundaries, blocked branches and finite execution.

## 0.1.0-beta.2

- Speed up Craft all ready by using NEI's bulk grid transfers instead of one batch every two ticks.
- Recheck readiness and output between transfers of up to 64 batches; yield between short bursts so closing the container stops further work.
- Preserve the previewed request limit, single-batch action and backpack slot restrictions. Count verified partial output before reporting an interrupted transfer.
- In the local singleplayer oak-plank benchmark, 64/256/349 batches completed in about 31/113/157 ms, previously about 6.6/25.8/35.2 seconds. Timing methods and remaining compatibility limits are documented in the validation record.

## 0.1.0-beta.1

- Keep Craft 1 batch and add Craft all ready, with batch/output previews and limits from remaining work, ingredients and output space.
- Recheck the container and prerequisites before every batch; closing the container stops further work. Report completed batches and stopping reasons.
- Explain unavailable actions on screen. Keep an occupied cursor or crafting grid in its original container when opening is blocked.
- Add a keyboard-accessible named group picker, first-run help, and an eight-table example using the pack's actual recipe.
- Select the Crafting tab for crafting-only groups; show tab counts and useful empty-state messages.
- Show readable upstream recipe labels and allow opening them. Include circuit configuration in item names.
- Bound item tooltips to the window and provide scrollable item information through the keyboard.
- Add Tab navigation and focus outlines. Clarify available-stock accounting and show save feedback beside the field.

See [validation](docs/validation.md) for the exact tested builds and remaining platform/integration coverage.
