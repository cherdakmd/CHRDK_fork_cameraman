package xyz.shibomb.cameraman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CameramanCommand implements CommandExecutor, TabCompleter {

    private CameramanManager manager;
    private Messages messages;
    private final OperatorRegistry operators;
    private final CameramanMenu menu;

    public CameramanCommand(OperatorRegistry operators, CameramanMenu menu) {
        this.operators = operators;
        this.manager = operators.getPrimaryManager();
        this.messages = manager.getMessages();
        this.menu = menu;
    }

    private static final Map<String, String> RUSSIAN_SUBCOMMANDS = Map.ofEntries(
            Map.entry("назначить", "set"),
            Map.entry("снять", "unset"),
            Map.entry("цель", "target"),
            Map.entry("сбросить", "clear"),
            Map.entry("новички", "newcomer"),
            Map.entry("ротация", "rotation"),
            Map.entry("минрасстояние", "minrotationdistance"),
            Map.entry("мобцель", "mobtarget"),
            Map.entry("автомобы", "automob"),
            Map.entry("плавныйтелепорт", "teleportsmooth"),
            Map.entry("наблюдение", "spectatemode"),
            Map.entry("наблюдениемобов", "mobspectatemode"),
            Map.entry("ночноезрениемобов", "mobnightvision"),
            Map.entry("сообщения", "showmessage"),
            Map.entry("порогночногозрения", "nightvisionthreshold"),
            Map.entry("дистанция", "distance"),
            Map.entry("высота", "height"),
            Map.entry("скоростьорбиты", "orbitspeed"),
            Map.entry("направлениеорбиты", "orbitdirection"),
            Map.entry("плавность", "smoothness"),
            Map.entry("длительностьпролёта", "flybyduration"),
            Map.entry("длительностькрана", "craneduration"),
            Map.entry("минвысотакрана", "craneheightmin"),
            Map.entry("максвысотакрана", "craneheightmax"),
            Map.entry("случайныйракурс", "random"),
            Map.entry("пейзаж", "scenic"),
            Map.entry("автопейзаж", "autoscenic"),
            Map.entry("скоростьдвижения", "movespeed"),
            Map.entry("направлениедвижения", "movedirection"),
            Map.entry("меню", "menu"),
            Map.entry("профиль", "profile"),
            Map.entry("операторы", "operators"),
            Map.entry("траектория", "path"),
            Map.entry("сцена", "scene"),
            Map.entry("пауза", "pause"),
            Map.entry("дальше", "next"),
            Map.entry("статус", "status"),
            Map.entry("инфоэкран", "hud"));

    private static String normalizeSubCommand(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return RUSSIAN_SUBCOMMANDS.getOrDefault(normalized, normalized);
    }

    @Override
    public synchronized List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        this.manager = operators.getManagerFor(sender);
        this.messages = manager.getMessages();
        if (!command.testPermissionSilent(sender)) {
            return List.of();
        }

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>(Arrays.asList(
                    "set", "unset", "target", "clear", "newcomer", "rotation", "minrotationdistance",
                    "mobtarget", "automob", "teleportsmooth", "spectatemode", "mobspectatemode",
                    "mobnightvision", "showmessage", "nightvisionthreshold", "distance", "height",
                    "orbitspeed", "orbitdirection", "smoothness", "flybyduration", "craneduration",
                    "craneheightmin", "craneheightmax", "random", "scenic", "autoscenic", "movespeed",
                    "movedirection", "menu", "profile", "operators", "path", "scene", "pause", "next", "status", "hud"));
            subcommands.addAll(RUSSIAN_SUBCOMMANDS.keySet());
            return matching(subcommands, args[0]);
        }

        if (args.length < 2) {
            return List.of();
        }

        String subcommand = normalizeSubCommand(args[0]);
        int argumentIndex = args.length - 1;
        String current = args[args.length - 1];

        if (subcommand.equals("profile") && argumentIndex == 1) {
            return matching(List.of("save", "load", "delete", "list"), current);
        }
        if (subcommand.equals("unset") && argumentIndex == 1) {
            return matching(operators.getOperatorNames(), current);
        }
        if (subcommand.equals("path") && argumentIndex == 1) {
            return matching(List.of("add", "play", "list", "delete", "stop", "export", "import", "record"), current);
        }
        if (subcommand.equals("path") && argumentIndex == 2) {
            if (args[1].equalsIgnoreCase("record")) {
                List<String> choices = new ArrayList<>(manager.getCameraRouteNames());
                choices.add("stop");
                return matching(choices, current);
            }
            return matching(manager.getCameraRouteNames(), current);
        }
        if (subcommand.equals("scene") && argumentIndex == 1) {
            return matching(List.of("add", "play", "list", "delete"), current);
        }
        if (subcommand.equals("scene") && argumentIndex == 2 && args[1].equalsIgnoreCase("play")) {
            return matching(manager.getCameraSceneNames(), current);
        }
        if (subcommand.equals("scene") && argumentIndex == 3 && args[1].equalsIgnoreCase("add")) {
            return matching(manager.getCameraRouteNames(), current);
        }
        if (subcommand.equals("profile") && argumentIndex == 2 && args.length >= 2
                && (args[1].equalsIgnoreCase("load") || args[1].equalsIgnoreCase("delete"))) {
            return matching(manager.getProfileNames(), current);
        }

        if ((subcommand.equals("set") || subcommand.equals("target")) && argumentIndex == 1) {
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), current);
        }

        if (argumentIndex == 1) {
            if (List.of("newcomer", "mobtarget", "automob", "teleportsmooth", "mobnightvision", "showmessage", "hud")
                    .contains(subcommand)) {
                return matching(List.of("true", "false"), current);
            }
            if (subcommand.equals("rotation")) {
                return matching(List.of("true", "false"), current);
            }
            if (subcommand.equals("spectatemode") || subcommand.equals("mobspectatemode")) {
                return matching(List.of("true", "false", "RANDOM"), current);
            }
            if (subcommand.equals("scenic") || subcommand.equals("autoscenic")) {
                return matching(List.of("true", "false"), current);
            }
            if (subcommand.equals("random")) {
                return matching(List.of("player", "mob", "scenic"), current);
            }
            if (subcommand.equals("orbitdirection")) {
                return matching(List.of("LEFT", "RIGHT", "RANDOM"), current);
            }
        }

        if (argumentIndex == 2) {
            if (subcommand.equals("spectatemode") || subcommand.equals("mobspectatemode")
                    || subcommand.equals("scenic") || subcommand.equals("autoscenic")) {
                return matching(Arrays.stream(SpectatePerspective.values()).map(Enum::name).toList(), current);
            }
            if (subcommand.equals("random")) {
                return matching(Arrays.stream(SpectatePerspective.values())
                        .filter(perspective -> perspective != SpectatePerspective.RANDOM)
                        .map(Enum::name).toList(), current);
            }
        }

        return List.of();
    }

    private List<String> matching(List<String> candidates, String prefix) {
        String normalizedPrefix = prefix.toLowerCase(Locale.ROOT);
        return candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Boolean parseBooleanArgument(CommandSender sender, String value) {
        if (!InputValidators.isBoolean(value)) {
            sender.sendMessage(messages.get("invalid-boolean"));
            return null;
        }
        return Boolean.parseBoolean(value);
    }

    private boolean isValidSpectateMode(String value) {
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")
                || value.equalsIgnoreCase("RANDOM");
    }

    private SpectatePerspective parsePerspectiveArgument(CommandSender sender, String value) {
        try {
            return SpectatePerspective.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            sender.sendMessage(messages.get("invalid-perspective",
                    Arrays.stream(SpectatePerspective.values()).map(Enum::name).collect(java.util.stream.Collectors.joining(", "))));
            return null;
        }
    }

    private Long parsePositiveLongArgument(CommandSender sender, String value) {
        if (!InputValidators.isPositiveInteger(value)) {
            sender.sendMessage(messages.get("invalid-integer"));
            return null;
        }
        return Long.parseLong(value);
    }

    private boolean isPositiveFiniteNumber(String value) {
        return InputValidators.isPositiveFiniteNumber(value);
    }

    private boolean isFiniteNumberOrRange(String value) {
        return InputValidators.isFiniteNumberOrRange(value);
    }

    @Override
    public synchronized boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        this.manager = operators.getManagerFor(sender);
        this.messages = manager.getMessages();
        if (args.length == 0) {
            return false;
        }

        String subCommand = normalizeSubCommand(args[0]);

        switch (subCommand) {
            case "menu":
                if (sender instanceof Player player) {
                    menu.open(player, manager);
                } else {
                    sender.sendMessage(messages.get("menu-player-only"));
                }
                break;

            case "status":
                sender.sendMessage(manager.getCameraStatusMessage());
                manager.getCameraDiagnostics().forEach(sender::sendMessage);
                break;

            case "hud": {
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-hud"));
                    return true;
                }
                Boolean enabled = parseBooleanArgument(sender, args[1]);
                if (enabled == null) return true;
                manager.setCameraHud(enabled);
                sender.sendMessage(messages.get("camera-hud-toggle", enabled));
                break;
            }

            case "path": {
                if (args.length < 2) {
                    sender.sendMessage(messages.get("path-usage"));
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("record")) {
                    if (args.length < 3) {
                        sender.sendMessage(messages.get("path-usage"));
                        return true;
                    }
                    if (args[2].equalsIgnoreCase("stop")) {
                        boolean recording = manager.isRecordingCameraRoute();
                        boolean saved = manager.stopCameraRouteRecording();
                        sender.sendMessage(messages.get(!recording ? "path-record-not-active"
                                : saved ? "path-record-saved" : "path-record-empty"));
                        return true;
                    }
                    long interval = 10L;
                    if (args.length >= 4) {
                        Long parsed = parsePositiveLongArgument(sender, args[3]);
                        if (parsed == null) return true;
                        interval = parsed;
                    }
                    sender.sendMessage(messages.get(manager.startCameraRouteRecording(args[2], interval)
                            ? "path-record-started" : "path-record-invalid"));
                    return true;
                }
                if (action.equals("list")) {
                    List<String> names = manager.getCameraRouteNames();
                    sender.sendMessage(names.isEmpty() ? messages.get("path-empty")
                            : messages.get("path-list", String.join(", ", names)));
                    return true;
                }
                if (action.equals("stop")) {
                    sender.sendMessage(messages.get(manager.stopPathPlayback() ? "path-stopped" : "path-not-playing"));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(messages.get("path-usage"));
                    return true;
                }
                String routeName = args[2];
                switch (action) {
                    case "add" -> {
                        if (!(sender instanceof Player)) sender.sendMessage(messages.get("menu-player-only"));
                        else sender.sendMessage(messages.get(manager.addCameraRoutePoint(routeName)
                                ? "path-point-added" : "path-invalid"));
                    }
                    case "play" -> {
                        long ticks = 40L;
                        if (args.length >= 4) {
                            Long parsed = parsePositiveLongArgument(sender, args[3]);
                            if (parsed == null) return true;
                            ticks = parsed;
                        }
                        sender.sendMessage(messages.get(manager.playCameraRoute(routeName, ticks)
                                ? "path-started" : "path-not-found"));
                    }
                    case "delete" -> sender.sendMessage(messages.get(manager.deleteCameraRoute(routeName)
                            ? "path-deleted" : "path-not-found"));
                    case "export" -> sender.sendMessage(messages.get(manager.exportCameraRoute(routeName)
                            ? "path-exported" : "path-export-failed"));
                    case "import" -> sender.sendMessage(messages.get(manager.importCameraRoute(routeName)
                            ? "path-imported" : "path-import-failed"));
                    default -> sender.sendMessage(messages.get("path-usage"));
                }
                break;
            }

            case "scene": {
                if (args.length < 2) {
                    sender.sendMessage(messages.get("scene-usage"));
                    return true;
                }
                String action = args[1].toLowerCase(Locale.ROOT);
                if (action.equals("list")) {
                    List<String> names = manager.getCameraSceneNames();
                    sender.sendMessage(names.isEmpty() ? messages.get("scene-empty")
                            : messages.get("scene-list", String.join(", ", names)));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(messages.get("scene-usage"));
                    return true;
                }
                String sceneName = args[2];
                switch (action) {
                    case "add" -> {
                        if (args.length < 4) sender.sendMessage(messages.get("scene-usage"));
                        else sender.sendMessage(messages.get(manager.addRouteToScene(sceneName, args[3])
                                ? "scene-route-added" : "scene-route-invalid"));
                    }
                    case "play" -> {
                        long ticks = 40L;
                        if (args.length >= 4) {
                            Long parsed = parsePositiveLongArgument(sender, args[3]);
                            if (parsed == null) return true;
                            ticks = parsed;
                        }
                        sender.sendMessage(messages.get(manager.playCameraScene(sceneName, ticks)
                                ? "scene-started" : "scene-invalid"));
                    }
                    case "delete" -> sender.sendMessage(messages.get(manager.deleteCameraScene(sceneName)
                            ? "scene-deleted" : "scene-not-found"));
                    default -> sender.sendMessage(messages.get("scene-usage"));
                }
                break;
            }

            case "pause":
                sender.sendMessage(messages.get(manager.toggleCameraPause() ? "camera-paused" : "camera-resumed"));
                break;

            case "next":
                if (manager.advanceTarget()) {
                    String targetName = manager.getCurrentTargetDisplayName();
                    sender.sendMessage(messages.get("target-advanced", targetName == null ? "?" : targetName));
                } else {
                    sender.sendMessage(messages.get("menu-no-targets"));
                }
                break;

            case "profile":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("profile-usage"));
                    return true;
                }
                String profileAction = args[1].toLowerCase(Locale.ROOT);
                if (profileAction.equals("list")) {
                    List<String> profiles = manager.getProfileNames();
                    sender.sendMessage(profiles.isEmpty()
                            ? messages.get("profile-empty")
                            : messages.get("profile-list", String.join(", ", profiles)));
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(messages.get("profile-usage"));
                    return true;
                }
                String profileName = args[2];
                switch (profileAction) {
                    case "save" -> sender.sendMessage(manager.saveProfile(profileName)
                            ? messages.get("profile-saved", profileName)
                            : messages.get("profile-invalid-name"));
                    case "load" -> sender.sendMessage(manager.loadProfile(profileName)
                            ? messages.get("profile-loaded", profileName)
                            : messages.get("profile-not-found", profileName));
                    case "delete" -> sender.sendMessage(manager.deleteProfile(profileName)
                            ? messages.get("profile-deleted", profileName)
                            : messages.get("profile-not-found", profileName));
                    default -> sender.sendMessage(messages.get("profile-usage"));
                }
                break;

            case "set":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-set"));
                    return true;
                }
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    sender.sendMessage(messages.get("player-not-found"));
                    return true;
                }
                operators.addOperator(player);
                sender.sendMessage(messages.get("operator-added", player.getName()));
                break;

            case "unset":
                UUID operatorId = null;
                if (args.length >= 2) {
                    try {
                        operatorId = UUID.fromString(args[1]);
                    } catch (IllegalArgumentException ignored) {
                        Player operatorPlayer = Bukkit.getPlayer(args[1]);
                        if (operatorPlayer == null) {
                            sender.sendMessage(messages.get("player-not-found"));
                            return true;
                        }
                        operatorId = operatorPlayer.getUniqueId();
                    }
                } else if (sender instanceof Player playerSender) {
                    if (!operators.isOperator(playerSender.getUniqueId())) {
                        sender.sendMessage(messages.get("not-camera-operator"));
                        return true;
                    }
                    operatorId = playerSender.getUniqueId();
                } else {
                    operatorId = operators.getPrimaryOperatorId();
                }
                if (operatorId == null || !operators.removeOperator(operatorId)) {
                    sender.sendMessage(messages.get("no-operators"));
                } else {
                    sender.sendMessage(messages.get("operator-removed"));
                }
                break;

            case "operators":
                List<String> operatorNames = operators.getOperatorNames();
                sender.sendMessage(operatorNames.isEmpty()
                        ? messages.get("no-operators")
                        : messages.get("operators-list", String.join(", ", operatorNames)));
                break;

            case "target":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-target"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(messages.get("player-not-found"));
                    return true;
                }
                manager.setTarget(target);
                sender.sendMessage(messages.get("target-set", target.getName()));
                break;

            case "clear":
                manager.clearTarget();
                break;

            case "newcomer":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-newcomer"));
                    return true;
                }
                Boolean newcomerModeArg = parseBooleanArgument(sender, args[1]);
                if (newcomerModeArg == null) return true;
                boolean newcomerMode = newcomerModeArg;
                manager.setNewcomerMode(newcomerMode);
                sender.sendMessage(messages.get("newcomer-mode", newcomerMode));
                break;

            case "rotation":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-rotation"));
                    return true;
                }
                Boolean rotEnabledArg = parseBooleanArgument(sender, args[1]);
                if (rotEnabledArg == null) return true;
                boolean rotEnabled = rotEnabledArg;
                long interval = 60L;
                if (args.length >= 3) {
                    Long parsedInterval = parsePositiveLongArgument(sender, args[2]);
                    if (parsedInterval == null) return true;
                    interval = parsedInterval;
                }
                manager.setRotationMode(rotEnabled, interval);
                sender.sendMessage(messages.get("rotation-mode", rotEnabled)); // Changed to rotEnabled for consistency
                break;

            case "minrotationdistance":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-min-distance"));
                    return true;
                }
                try {
                    double dist = Double.parseDouble(args[1]);
                    if (!Double.isFinite(dist) || dist < 0) {
                        sender.sendMessage(messages.get("distance-negative"));
                        return true;
                    }
                    manager.setMinRotationDistance(dist);
                    sender.sendMessage(messages.get("min-distance-set", dist));
                } catch (NumberFormatException e) {
                    sender.sendMessage(messages.get("invalid-distance"));
                }
                break;

            case "mobtarget":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-mobtarget"));
                    return true;
                }
                Boolean mobTargetModeArg = parseBooleanArgument(sender, args[1]);
                if (mobTargetModeArg == null) return true;
                boolean mobTargetMode = mobTargetModeArg;
                manager.setMobTargetMode(mobTargetMode);
                sender.sendMessage(messages.get("mob-target-mode", mobTargetMode));
                break;

            case "automob":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-automob"));
                    return true;
                }
                Boolean autoMobTargetArg = parseBooleanArgument(sender, args[1]);
                if (autoMobTargetArg == null) return true;
                boolean autoMobTarget = autoMobTargetArg;
                long delay = 60L;
                if (args.length >= 3) {
                    Long parsedDelay = parsePositiveLongArgument(sender, args[2]);
                    if (parsedDelay == null) return true;
                    delay = parsedDelay;
                }
                manager.setAutoMobTarget(autoMobTarget, delay);
                sender.sendMessage(messages.get("auto-mob-target", autoMobTarget));
                break;

            case "teleportsmooth":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-teleport-smooth"));
                    return true;
                }
                Boolean teleportSmoothArg = parseBooleanArgument(sender, args[1]);
                if (teleportSmoothArg == null) return true;
                boolean teleportSmooth = teleportSmoothArg;
                long duration = 3L;
                if (args.length >= 3) {
                    Long parsedDuration = parsePositiveLongArgument(sender, args[2]);
                    if (parsedDuration == null) return true;
                    duration = parsedDuration;
                }
                manager.setTeleportSmooth(teleportSmooth, duration);
                sender.sendMessage(messages.get("teleport-smooth", teleportSmooth));
                break;

            case "spectatemode":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-spectate-mode"));
                    return true;
                }
                String spectateArg = args[1];
                if (!isValidSpectateMode(spectateArg)) {
                    sender.sendMessage(messages.get("invalid-spectate-state"));
                    return true;
                }
                SpectatePerspective playerPerspective = null;
                if (args.length >= 3) {
                    playerPerspective = parsePerspectiveArgument(sender, args[2]);
                    if (playerPerspective == null) return true;
                }
                manager.setSpectateMode(spectateArg);
                if (playerPerspective != null) {
                    manager.setSpectatePerspective(playerPerspective);
                }
                sender.sendMessage(playerPerspective == null
                        ? messages.get("spectate-player-state", spectateArg)
                        : messages.get("spectate-player-state-perspective", spectateArg, playerPerspective));
                break;

            case "mobspectatemode":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-mob-spectate-mode"));
                    return true;
                }
                String mobSpectateArg = args[1];
                if (!isValidSpectateMode(mobSpectateArg)) {
                    sender.sendMessage(messages.get("invalid-spectate-state"));
                    return true;
                }
                SpectatePerspective mobPerspective = null;
                if (args.length >= 3) {
                    mobPerspective = parsePerspectiveArgument(sender, args[2]);
                    if (mobPerspective == null) return true;
                }
                manager.setMobSpectateMode(mobSpectateArg);
                if (mobPerspective != null) {
                    manager.setMobSpectatePerspective(mobPerspective);
                }
                sender.sendMessage(mobPerspective == null
                        ? messages.get("spectate-mob-state", mobSpectateArg)
                        : messages.get("spectate-mob-state-perspective", mobSpectateArg, mobPerspective));
                break;

            case "mobnightvision":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-mob-night-vision"));
                    return true;
                }
                Boolean mobNightVisionArg = parseBooleanArgument(sender, args[1]);
                if (mobNightVisionArg == null) return true;
                boolean mobNightVision = mobNightVisionArg;
                manager.setMobNightVision(mobNightVision);
                sender.sendMessage(messages.get("mob-night-vision", mobNightVision));
                break;
            case "showmessage":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-show-message"));
                    return true;
                }
                Boolean showMessageArg = parseBooleanArgument(sender, args[1]);
                if (showMessageArg == null) return true;
                boolean showMessage = showMessageArg;
                manager.setShowMessage(showMessage);
                sender.sendMessage(messages.get("show-message", showMessage));
                break;
            case "nightvisionthreshold":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-night-vision-threshold"));
                    return true;
                }
                try {
                    int threshold = Integer.parseInt(args[1]);
                    if (threshold < 0 || threshold > 15) {
                        sender.sendMessage(messages.get("threshold-invalid"));
                        return true;
                    }
                    manager.setNightVisionThreshold(threshold);
                    sender.sendMessage(messages.get("threshold-set", threshold));
                } catch (NumberFormatException e) {
                    sender.sendMessage(messages.get("invalid-number"));
                }
                break;

            case "distance":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-distance"));
                    return true;
                }
                String distance = args[1];
                // Optional: Validate format here, but manager handles parsing safely
                if (!isFiniteNumberOrRange(distance)) {
                    sender.sendMessage(messages.get("invalid-range", "3.0-6.0"));
                    return true;
                }
                manager.setSpectateDistance(distance);
                sender.sendMessage(messages.get("distance-value", distance));
                break;

            case "height":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-height"));
                    return true;
                }
                String height = args[1];
                if (!isFiniteNumberOrRange(height)) {
                    sender.sendMessage(messages.get("invalid-range", "1.0-3.0"));
                    return true;
                }
                manager.setSpectateHeight(height);
                sender.sendMessage(messages.get("height-value", height));
                break;

            case "orbitspeed":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-orbit-speed"));
                    return true;
                }
                String speed = args[1];
                if (!isFiniteNumberOrRange(speed)) {
                    sender.sendMessage(messages.get("invalid-range", "0.5-2.0"));
                    return true;
                }
                manager.setOrbitSpeed(speed);
                sender.sendMessage(messages.get("orbit-speed-value", speed));
                break;

            case "orbitdirection":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-orbit-direction"));
                    return true;
                }
                String direction = args[1].toUpperCase(Locale.ROOT);
                if (direction.equals("LEFT") || direction.equals("RIGHT") || direction.equals("RANDOM")) {
                    manager.setOrbitDirection(direction);
                    sender.sendMessage(messages.get("orbit-direction-value", direction));
                } else {
                    sender.sendMessage(messages.get("invalid-orbit-direction"));
                }
                break;

            case "smoothness":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-smoothness"));
                    return true;
                }
                try {
                    double smoothness = Double.parseDouble(args[1]);
                    if (!Double.isFinite(smoothness) || smoothness < 0.01 || smoothness > 1.0) {
                        sender.sendMessage(messages.get("smoothness-range"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(messages.get("invalid-smoothness"));
                    return true;
                }
                manager.setDynamicSmoothness(args[1]);
                break;

            case "flybyduration":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-flyby-duration"));
                    return true;
                }
                if (!isPositiveFiniteNumber(args[1])) {
                    sender.sendMessage(messages.get("invalid-flyby-duration"));
                    return true;
                }
                manager.setFlybyDuration(args[1]);
                break;

            case "craneduration":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-crane-duration"));
                    return true;
                }
                if (!isPositiveFiniteNumber(args[1])) {
                    sender.sendMessage(messages.get("invalid-crane-duration"));
                    return true;
                }
                manager.setCraneDuration(args[1]);
                break;

            case "random":
                if (args.length < 3) {
                    sender.sendMessage(messages.get("usage-random"));
                    return true;
                }
                String type = args[1].toLowerCase(Locale.ROOT);
                if (!type.equals("player") && !type.equals("mob") && !type.equals("scenic")) {
                    sender.sendMessage(messages.get("invalid-random-type"));
                    return true;
                }

                String[] modes = args[2].split(",", -1);
                List<String> validModes = new ArrayList<>();
                for (String mode : modes) {
                    try {
                        SpectatePerspective perspective = SpectatePerspective.valueOf(mode.trim().toUpperCase(Locale.ROOT));
                        if (perspective == SpectatePerspective.RANDOM) {
                            sender.sendMessage(messages.get("random-value-not-allowed"));
                            return true;
                        }
                        validModes.add(perspective.name());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(messages.get("random-perspective-invalid", mode));
                        return true;
                    }
                }
                manager.setRandomPerspectives(type, validModes);
                break;

            case "scenic":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-scenic"));
                    return true;
                }
                Boolean scenicEnableArg = parseBooleanArgument(sender, args[1]);
                if (scenicEnableArg == null) return true;
                boolean scenicEnable = scenicEnableArg;
                if (scenicEnable) {
                    SpectatePerspective perspective = SpectatePerspective.POV;
                    if (args.length >= 3) {
                        perspective = parsePerspectiveArgument(sender, args[2]);
                        if (perspective == null) return true;
                    }
                    if (sender instanceof Player) {
                        Player p = (Player) sender;
                        manager.startScenicTask(p.getLocation(), perspective);
                        sender.sendMessage(messages.get("scenic-enabled-player", perspective));
                    } else {
                        // If console, use the cameraman's current location
                        Player cameraman = manager.getCameraman();
                        if (cameraman != null) {
                            manager.startScenicTask(cameraman.getLocation(), perspective);
                            sender.sendMessage(messages.get("scenic-enabled-cameraman", perspective));
                        } else {
                            sender.sendMessage(
                                    messages.get("scenic-console-no-cameraman"));
                        }
                    }
                } else {
                    manager.stopFollowing();
                    sender.sendMessage(messages.get("scenic-disabled"));
                }
                break;

            case "autoscenic":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-autoscenic"));
                    return true;
                }
                Boolean autoScenicArg = parseBooleanArgument(sender, args[1]);
                if (autoScenicArg == null) return true;
                boolean autoScenic = autoScenicArg;
                SpectatePerspective autoScenicPerspective = null;
                if (args.length >= 3) {
                    autoScenicPerspective = parsePerspectiveArgument(sender, args[2]);
                    if (autoScenicPerspective == null) return true;
                }
                manager.setAutoScenic(autoScenic, autoScenicPerspective);
                break;

            case "movespeed":
                if (args.length < 2) {
                    sender.sendMessage(messages.get("usage-move-speed"));
                    return true;
                }
                if (!isFiniteNumberOrRange(args[1])) {
                    sender.sendMessage(messages.get("invalid-move-speed"));
                    return true;
                }
                manager.setMoveSpeed(args[1]);
                break;

            case "movedirection":
                if (args.length < 4) {
                    sender.sendMessage(messages.get("usage-move-direction"));
                    return true;
                }
                if (!isFiniteNumberOrRange(args[1]) || !isFiniteNumberOrRange(args[2])
                        || !isFiniteNumberOrRange(args[3])) {
                    sender.sendMessage(messages.get("invalid-move-vector"));
                    return true;
                }
                manager.setMoveDirection(args[1], args[2], args[3]);
                break;

            default:
                sender.sendMessage(messages.get("unknown-subcommand"));
                break;
        }

        return true;
    }
}
