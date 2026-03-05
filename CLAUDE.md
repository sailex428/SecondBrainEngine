# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SecondBrainEngine is a server-side Fabric mod that combines Automatone (a Baritone fork for server-side pathfinding) with an adaptation of AltoClef (high-level task framework). It uses Carpet to spawn fake `ServerPlayerEntity` instances that can be directed through a priority-based task chain system. No client mod is required.

## Build Commands

```bash
./gradlew build              # Full build (compile, remap, package)
./gradlew remapJar           # Remap main JAR for production
./gradlew testmodJar         # Build the testmod JAR
./gradlew remapTestmodJar    # Remap testmod JAR
```

There are no unit tests — the testmod at `src/testmod/` is used for manual in-game testing.

## Multi-Version Support (Stonecutter)

The mod targets **5 Minecraft versions**: 1.20.1, 1.21.1, 1.21.8, 1.21.10, 1.21.11. Stonecutter preprocesses source files at build time using conditional comments:

```java
//? >=1.21.10 {
/*version-specific code here
*///?} elif >=1.21.1 {
/*alternative code
*///?} else {
fallback code
//?}
```

The active (VCS) version is **1.20.1** — this is the version that appears uncommented in source. Version-specific properties live in `versions/<version>/gradle.properties`. Java 17 is used for 1.20.1/1.21.1, Java 21 for 1.21.8+.

Cross-version API differences are abstracted through wrapper classes in `multiversion/` (e.g., `EntityVer`, `CommandVer`, `DamageSourceVer`).

## Architecture

### Core Flow

```
NPCSpawner (Carpet) → creates fake ServerPlayerEntity
    → BaritoneAPI.getProvider().getBaritone(npc) → IBaritone instance
    → new AltoClefController(baritone) → initializes all systems
    → controller.runUserTask(task) → queues task in UserTaskChain
    → TaskRunner picks highest-priority TaskChain each tick
    → Task.onTick() returns subtasks hierarchically
    → Automatone handles pathfinding/movement/block interaction
```

### Key Packages (`me.sailex.*`)

- **`altoclef/`** — AltoClef adaptation: task framework, chains, commands, trackers, crafting, resource collection (~520 files)
- **`automatone/`** — Baritone fork: pathfinding, behaviors, processes, world caching (~100 files)
- **`common/`** — Shared interfaces (`ServerTickable`)

### AltoClefController (`altoclef/AltoClefController.java`)

Central hub that orchestrates everything. Created per-NPC with an `IBaritone` instance. Manages:
- **TaskRunner** — selects and executes the highest-priority TaskChain each tick
- **9 TaskChains** — UserTaskChain, FoodChain, MobDefenseChain, MLGBucketFallChain, UnstuckChain, WorldSurvivalChain, etc.
- **Trackers** — EntityTracker, ItemStorageTracker, CraftingRecipeTracker, etc. (lazy-evaluated, recalculate only when dirty)
- **BotBehaviour** — state stack (push/pop) for scoped behavior overrides (item protection, block avoidance)
- **InputControls / SlotHandler / PlayerExtraController** — low-level player interaction

### Task System

**Task** (`tasksystem/Task.java`): Abstract base with lifecycle `onStart() → onTick() → onStop()`. `onTick()` returns the next subtask (hierarchical composition) or null. ~170 task implementations across 17 categories in `tasks/` (construction, container, entity, movement, resources, speedrun, slot, etc.).

**TaskChain** (`tasksystem/TaskChain.java`): Groups related tasks with a `getPriority()` float. TaskRunner picks the chain with the highest priority each tick.

**TaskCatalogue** (`TaskCatalogue.java`): Registry mapping 100+ resource types to Task instances. Use `getItemTask(Item, quantity)` for dynamic resource collection.

### Command System

`commandsystem/CommandExecutor` registers commands by name. Commands are invoked with `@` prefix and can be chained with semicolons: `@get diamond; @goto 0 0 0`. 18+ commands in `commands/`.

### Automatone (Baritone)

`IBaritone` provides pathfinding behaviors and processes (Mine, GetToBlock, Follow, Build, Farm, Fish, Explore). Accessed via `BaritoneAPI.getProvider().getBaritone(playerEntity)`.

### Mixins

16 mixins in `mixins.secondbrainengine.json` targeting inventory management, damage/death handling, block/entity interactions, and command source modifications.

## Key Patterns

- **Event bus** (`automatone/eventbus/EventBus.java`): Publish-subscribe with thread-safe locking. `EventBus.publish(event)` / `EventBus.subscribe(Class, Consumer)`.
- **Tracker lazy evaluation**: Trackers call `ensureUpdated()` which only recalculates if the tracker is marked dirty.
- **BotBehaviour stack**: Push/pop scoped state for temporary behavior changes (e.g., temporarily allowing certain items to be used).
