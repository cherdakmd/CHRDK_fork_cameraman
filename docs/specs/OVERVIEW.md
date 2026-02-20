# Cinematic Cameraman - Project Overview

## 1. Description

A Minecraft Paper plugin (`whole-minecraft-cameraman`) that allows a designated player to act as a "Cinematic Cameraman", automatically spectating players and entities with various cinematic camera perspectives.

## 2. Goals

- Provide hands-off, cinematic viewing of a Minecraft server.
- Feature various intelligent targeting modes (players, mobs, locations).
- Offer extensive and smooth camera perspectives (orbit, flyby, dynamic, crane, etc.).
- Ensure persistence of settings across server restarts.

## 3. Technology Stack

- **Platform**: Minecraft Paper API (`1.20`)
- **Language**: Java
- **Build Tool**: Maven

## 4. Architecture Principles for AI-DD

To maintain and extend this plugin using AI Spec-Driven Development:

1. **Spec First**: Any new feature or modification must first be documented in the `docs/specs/` directory before implementation.
2. **Single Source of Truth**: The `config.yml` provides the default values, while `docs/specs/` explains the intended behavior.
3. **No Unapproved State Changes**: Persistent states (like Cameraman UUID, modes) must be managed carefully and robustly to survive server restarts.
4. **Modularity**: Camera perspectives are isolated behaviors and should be easily extensible by adding new `Shot` implementations.

## 5. Specification Structure

- `features/01_cameraman_core.md`: Core mechanics, assignment, and persistence.
- `features/02_targeting_system.md`: Logic for selecting targets (Newcomer, Rotation, Mob).
- `features/03_spectate_perspectives.md`: Camera behavior and positioning rules.
- `features/04_scenic_mode.md`: Targetless cinematic viewpoints.
