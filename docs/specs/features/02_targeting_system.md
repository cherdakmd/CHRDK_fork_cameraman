# Feature Specification: Targeting System

## 1. Summary

The Targeting System dictates who or what the Cameraman should spectate at any given moment. It operates through various modes that can interact or fall back onto each other.

## 2. Targeting Modes

### 2.1 Manual Target

- **Command**: `/cameraman target <player|mob>`
- Forces the Cameraman to spectate a specific target immediately.

### 2.2 Newcomer Mode

- **Command**: `/cameraman newcomer <true|false>`
- **Behavior**: Automatically forces the Cameraman to spectate a player the moment they join the server.

### 2.3 Rotation Mode

- **Command**: `/cameraman rotation <true|false> [interval]`
- **Behavior**: Automatically cycles the spectated target among available online players.
- **Interval**: Time in seconds between switches (Default: 60s).
- **Condition**: Uses `/cameraman minrotationdistance <distance>` to avoid rotating to a target that is too close to the current location (Default: 5.0 blocks).

### 2.4 Mob Target Mode

- **Command**: `/cameraman mobtarget <true|false>`
- **Behavior**: When enabled, the plugin will target living entities (mobs) instead of players during rotation and target selection.

### 2.5 Auto Mob Target

- **Command**: `/cameraman automob <true|false> [delay]`
- **Behavior**: Acts as a fallback mechanism. If no players are available online to spectate, after the designated delay (Default: 60s), the plugin automatically switches to `Mob Target Mode`.

## 3. Related Systems

This logic works in conjunction with the **Spectate Perspectives** (which dictate _how_ the target is viewed) and **Scenic Mode** (which occurs when no valid entities are available or desired).
