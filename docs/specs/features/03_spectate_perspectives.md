# Feature Specification: Spectate Perspectives

## 1. Summary

When a target is selected, "Perspectives" control the spatial relationship between the Cameraman's camera and the target entity.

## 2. Mode Toggles

### 2.1 Player Spectate Mode

- **Command**: `/cameraman spectatemode <true|false> [perspective]`
- Enables/disables spectating players. When `true`, enables continuous tracking. When `false`, the cameraman simply teleports to the initial perspective start point.

### 2.2 Mob Spectate Mode

- **Command**: `/cameraman mobspectatemode <true|false> [perspective]`
- Enables/disables spectating mobs. Includes an option (`mobnightvision`) to apply a Night Vision effect based on a light level threshold.

### 2.3 Smooth Teleport

- **Command**: `/cameraman teleportsmooth <true|false> [duration]`
- When switching targets, interpolates the camera position visually. Duration scales based on `teleportSmoothSpeed` up to the minimum duration.

## 3. Available Perspectives

1. **POV**: First-person view (standard Minecraft Spectator behavior).
2. **BEHIND**: Fixed position behind target (`distance`, `height`).
3. **FRONT**: Fixed position in front of target (`distance`, `height`).
4. **ORBIT**: Circles around target (`orbitSpeed`, `orbitDirection`, `distance`, `height`).
5. **DYNAMIC**: Spring-arm, delayed tracking effect smoothing out target movements (`dynamicSmoothness`, `distance`, `height`). Includes `_BEHIND`, `_FRONT`, `_POV`.
6. **FLYBY**: Continuous sweeping motion looping back and forth (`flybyDuration`).
7. **CRANE**: Vertical boom movement while holding horizontal distance (`craneHeightMin`, `craneHeightMax`, `craneDuration`).
8. **MOVE**: Linear motion along a defined directional vector (`moveDirection`, `moveSpeed`).
9. **FIX**: Strictly static tripod shot at a location.
10. **RANDOM**: Randomly chooses one of the active perspectives based on the active target type's list (`randomPlayerPerspectives`, `randomMobPerspectives`, `randomScenicPerspectives`).
