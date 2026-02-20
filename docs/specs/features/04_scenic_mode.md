# Feature Specification: Scenic Mode

## 1. Summary

Scenic Mode allows for "Targetless" cinematic shots where the camera moves independently based on a static anchor point in the world instead of following an entity.

## 2. Mechanics

### 2.1 Manual Scenic Mode

- **Command**: `/cameraman scenic <true|false> [perspective]`
- When enabled, the Cameraman initiates a cinematic shot using their _current physical location_ as the anchor point.
- Usable perspectives typically include: `ORBIT`, `FLYBY`, `CRANE`, `MOVE`, and `FIX`.

### 2.2 Auto Scenic

- **Command**: `/cameraman autoscenic <true|false> [perspective]`
- **Behavior**: An automatic fallback sequence.
- **Trigger**: Activates if a new target is found but `spectateMode` is `false`.
- **Result**: The Cameraman teleports to the target but immediately switches into Scenic Mode anchored around that location instead of locking onto the entity.

## 3. Interaction with Randomization

- Scenic Mode perspectives can be randomized using the `RANDOM` perspective type.
- The list of allowed perspectives is defined in the configuration array `randomScenicPerspectives` (e.g., ORBIT, FLYBY, MOVE, CRANE, FIX).
