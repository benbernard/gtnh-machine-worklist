# Bulk chain scheduling

The previous scheduler sorted consumers first. With some rods already available, it selected long rods immediately, even while the remaining rod recipe could produce a larger batch. This policy was separate from the old returning-tool readiness limit fixed in beta.8.

Order the selected pending dependency graph producers first. Recalculate after every verified transfer; continue producing a needed intermediate before consuming partial supplies. Each NEI transfer remains limited to 64 recipe runs, with readiness, tool durability and usable output space checked normally. Yield determines item count: 64 runs yielding two rods produce two stacks for 64 long rods. Never pad a small requested target to a full stack.

A blocked producer is a preference, not a prerequisite lock. Continue checking consumers and independent branches, allowing downstream crafting to free space. Manual machines stay manual. Cycle fallback remains deterministic and the original finite per-recipe execution budget is unchanged.

Three regression fixtures use the actual NEI chain math: two existing rods plus 126 ingredients produce 64 rods, 62 rods, then 64 long rods; a two-rod yield produces 128 rods before 64 long rods; a one-stack intermediate-space limit produces 64 rods, 32 long rods, then repeats. Existing exact-target, shared-input, machine-pause and budget tests remain required.

Validation uses local and three-platform CI builds, candidate/release and published-asset hashes. No computer use or live timings while the user is playing. Main-profile releases are staged disabled if Minecraft is running.
