# Feature Specification: Cameraman Core & Persistence

## 1. Summary

The core system handles managing who is the Cameraman, applying the appropriate game modes, and ensuring that settings persist across server restarts or player reconnections.

## 2. Mechanics

### 2.1 Managing the Cameraman

- **Command**: `/cameraman set <player>`
  - Action: Sets the target player as the Cameraman.
  - State: Puts them into Spectator mode.
- **Command**: `/cameraman unset`
  - Action: Removes the Cameraman status.
  - State: Returns them to Survival mode.

### 2.2 Global Settings Configuration

- **Command**: `/cameraman showmessage <true|false>`
  - Toggles all informational chat messages and automatic status updates for the cameraman (e.g., "Moving to...", "Now spectating...", "Auto-switched to...", "Mob Target Mode set to: true").
  - Regardless of who executes a `/cameraman` settings command, the notification broadcasted to the cameraman respects this flag.
  - Default: `true`.

### 2.3 Persistence & Restoration

- All settings, including the current Cameraman UUID, are saved to `config.yml`.
- If the server restarts, the plugin reads `config.yml` on enable and restores the Cameraman.
- If the Cameraman disconnects:
  - The plugin pauses cameraman operations gracefully.
- If the Cameraman reconnects:
  - The plugin resumes operations automatically using saved states.

### 2.4 Permissions

- **Node**: `cameraman.use`
- **Default**: `OP`
- Controls access to all `/cameraman` commands.
