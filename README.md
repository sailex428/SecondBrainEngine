# SecondBrainEngine

A serverside fabric lib/mod that combines [Automatone](https://github.com/sailex428/Automatone) (fork of Automatone for SecondBrain mod), which handles pathfinding and basic world interactions, with a server-side adaptation of [AltoClef](https://github.com/MiranCZ/altoclef).
AltoClef provides the high-level task framework that manages tasks and task chains.
In addition, the system uses Carpet to spawn fake `ServerPlayerEntity` instances.
These fake players behave like normal players and can be directed through the task framework.

## Usage

Below a basic example of usage for spawning a NPC, initializing a Controller, and then executing a Task.

 ```java
 GameProfile npcProfile = new GameProfile(UUID.randomUUID(), "minusaura");
 
 NPCSpawner.spawn(npcProfile, server, spawnPos, npcEntity -> {
    IBaritone automatone = BaritoneAPI.getProvider().getBaritone(npcEntity);
    
    AltoClefController controller = new AltoClefController(automatone);
    
    controller.runUserTask(new PlaceBlockNearbyTask(Blocks.CRAFTING_TABLE, Blocks.FURNACE));
 });
 ```
