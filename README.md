# SecondBrainEngine

A serverside fabric lib/mod that combines [Automatone](https://github.com/sailex428/Automatone) (fork of Automatone for SecondBrain mod), which handles pathfinding and basic world interactions, with a server-side adaptation of [AltoClef](https://github.com/MiranCZ/altoclef).
AltoClef provides the high-level task framework that manages tasks and task chains.
In addition, the system uses Carpet to spawn fake `ServerPlayerEntity` instances.
These fake players behave like normal players and can be directed through the task framework.

## Usage

A basic usage example can be found in the [Otomaton TestMod](src/testmod/java/me/sailex/otomaton/Otomaton.java).

## License

This project is licensed under the LGPL-3.0.
Includes code from the AltoClef and Automatone projects (LGPL-3.0).  
Original authors are listed in their respective repositories.
