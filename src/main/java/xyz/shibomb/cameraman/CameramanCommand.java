package xyz.shibomb.cameraman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CameramanCommand implements CommandExecutor, TabCompleter {

    private final CameramanManager manager;
    private final Messages messages;

    public CameramanCommand(CameramanManager manager) {
        this.manager = manager;
        this.messages = manager.getMessages();
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
            Map.entry("направлениедвижения", "movedirection"));

    private static String normalizeSubCommand(String input) {
        String normalized = input.toLowerCase(Locale.ROOT);
        return RUSSIAN_SUBCOMMANDS.getOrDefault(normalized, normalized);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
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
                    "movedirection"));
            subcommands.addAll(RUSSIAN_SUBCOMMANDS.keySet());
            return matching(subcommands, args[0]);
        }

        if (args.length < 2) {
            return List.of();
        }

        String subcommand = normalizeSubCommand(args[0]);
        int argumentIndex = args.length - 1;
        String current = args[args.length - 1];

        if ((subcommand.equals("set") || subcommand.equals("target")) && argumentIndex == 1) {
            return matching(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), current);
        }

        if (argumentIndex == 1) {
            if (List.of("newcomer", "mobtarget", "automob", "teleportsmooth", "mobnightvision", "showmessage")
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

    private static final Pattern NUMBER_PATTERN = Pattern.compile("-?\\d+(?:\\.\\d+)?");
    private static final Pattern RANGE_PATTERN = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)-(-?\\d+(?:\\.\\d+)?)$");

    private Boolean parseBooleanArgument(CommandSender sender, String value) {
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        sender.sendMessage(messages.get("invalid-boolean"));
        return null;
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
        try {
            long parsed = Long.parseLong(value);
            if (parsed > 0 && parsed <= Long.MAX_VALUE / 20L) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Report a single user-friendly validation message below.
        }
        sender.sendMessage(messages.get("invalid-integer"));
        return null;
    }

    private boolean isPositiveFiniteNumber(String value) {
        try {
            double parsed = Double.parseDouble(value);
            return Double.isFinite(parsed) && parsed > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isFiniteNumberOrRange(String value) {
        String trimmed = value.trim();
        try {
            if (NUMBER_PATTERN.matcher(trimmed).matches()) {
                return Double.isFinite(Double.parseDouble(trimmed));
            }
            Matcher matcher = RANGE_PATTERN.matcher(trimmed);
            return matcher.matches()
                    && Double.isFinite(Double.parseDouble(matcher.group(1)))
                    && Double.isFinite(Double.parseDouble(matcher.group(2)));
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
            @NotNull String[] args) {
        if (args.length == 0) {
            return false;
        }

        String subCommand = normalizeSubCommand(args[0]);

        switch (subCommand) {
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
                manager.setCameraman(player);
                sender.sendMessage(messages.get("cameraman-set", player.getName()));
                break;

            case "unset":
                manager.removeCameraman();
                sender.sendMessage(messages.get("cameraman-unset"));
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
