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

    public CameramanCommand(CameramanManager manager) {
        this.manager = manager;
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
        sender.sendMessage("Неверное значение: укажите true или false.");
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
            sender.sendMessage("Неверный ракурс. Доступные ракурсы: " +
                    Arrays.stream(SpectatePerspective.values()).map(Enum::name).collect(java.util.stream.Collectors.joining(", ")));
            return null;
        }
    }

    private Long parsePositiveLongArgument(CommandSender sender, String value, String label) {
        try {
            long parsed = Long.parseLong(value);
            if (parsed > 0 && parsed <= Long.MAX_VALUE / 20L) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Report a single user-friendly validation message below.
        }
        sender.sendMessage("Неверное значение параметра «" + label + "»: требуется целое положительное число в допустимом диапазоне.");
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
                    sender.sendMessage("Использование: /cameraman set <player>");
                    return true;
                }
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    sender.sendMessage("Игрок не найден.");
                    return true;
                }
                manager.setCameraman(player);
                sender.sendMessage("Оператор камеры назначен: " + player.getName());
                break;

            case "unset":
                manager.removeCameraman();
                sender.sendMessage("Оператор камеры снят.");
                break;

            case "target":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman target <player>");
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("Игрок не найден.");
                    return true;
                }
                manager.setTarget(target);
                sender.sendMessage("Цель выбрана: " + target.getName());
                break;

            case "clear":
                manager.clearTarget();
                break;

            case "newcomer":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman newcomer <true/false>");
                    return true;
                }
                Boolean newcomerModeArg = parseBooleanArgument(sender, args[1]);
                if (newcomerModeArg == null) return true;
                boolean newcomerMode = newcomerModeArg;
                manager.setNewcomerMode(newcomerMode);
                sender.sendMessage("Режим новичков: " + newcomerMode);
                break;

            case "rotation":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman rotation <true|false> [interval]");
                    return true;
                }
                Boolean rotEnabledArg = parseBooleanArgument(sender, args[1]);
                if (rotEnabledArg == null) return true;
                boolean rotEnabled = rotEnabledArg;
                long interval = 60L;
                if (args.length >= 3) {
                    Long parsedInterval = parsePositiveLongArgument(sender, args[2], "интервал");
                    if (parsedInterval == null) return true;
                    interval = parsedInterval;
                }
                manager.setRotationMode(rotEnabled, interval);
                sender.sendMessage("Режим ротации: " + rotEnabled); // Changed to rotEnabled for consistency
                break;

            case "minrotationdistance":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman minrotationdistance <distance>");
                    return true;
                }
                try {
                    double dist = Double.parseDouble(args[1]);
                    if (!Double.isFinite(dist) || dist < 0) {
                        sender.sendMessage("Расстояние должно быть конечным числом не меньше нуля.");
                        return true;
                    }
                    manager.setMinRotationDistance(dist);
                    sender.sendMessage("Минимальное расстояние ротации: " + dist);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Неверное расстояние. Укажите число не меньше нуля.");
                }
                break;

            case "mobtarget":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman mobtarget <true/false>");
                    return true;
                }
                Boolean mobTargetModeArg = parseBooleanArgument(sender, args[1]);
                if (mobTargetModeArg == null) return true;
                boolean mobTargetMode = mobTargetModeArg;
                manager.setMobTargetMode(mobTargetMode);
                sender.sendMessage("Режим выбора мобов: " + mobTargetMode);
                break;

            case "automob":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman automob <true/false> [delay]");
                    return true;
                }
                Boolean autoMobTargetArg = parseBooleanArgument(sender, args[1]);
                if (autoMobTargetArg == null) return true;
                boolean autoMobTarget = autoMobTargetArg;
                long delay = 60L;
                if (args.length >= 3) {
                    Long parsedDelay = parsePositiveLongArgument(sender, args[2], "задержка");
                    if (parsedDelay == null) return true;
                    delay = parsedDelay;
                }
                manager.setAutoMobTarget(autoMobTarget, delay);
                sender.sendMessage("Автовыбор мобов: " + autoMobTarget);
                break;

            case "teleportsmooth":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman teleportsmooth <true/false> [duration]");
                    return true;
                }
                Boolean teleportSmoothArg = parseBooleanArgument(sender, args[1]);
                if (teleportSmoothArg == null) return true;
                boolean teleportSmooth = teleportSmoothArg;
                long duration = 3L;
                if (args.length >= 3) {
                    Long parsedDuration = parsePositiveLongArgument(sender, args[2], "длительность");
                    if (parsedDuration == null) return true;
                    duration = parsedDuration;
                }
                manager.setTeleportSmooth(teleportSmooth, duration);
                sender.sendMessage("Плавная телепортация: " + teleportSmooth);
                break;

            case "spectatemode":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman spectatemode <true|false|RANDOM> [perspective]");
                    return true;
                }
                String spectateArg = args[1];
                if (!isValidSpectateMode(spectateArg)) {
                    sender.sendMessage("Режим должен быть true, false или RANDOM.");
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
                sender.sendMessage("Режим наблюдения (игроки): " + spectateArg
                        + (playerPerspective == null ? "" : " (ракурс: " + playerPerspective + ")"));
                break;

            case "mobspectatemode":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman mobspectatemode <true|false|RANDOM> [perspective]");
                    return true;
                }
                String mobSpectateArg = args[1];
                if (!isValidSpectateMode(mobSpectateArg)) {
                    sender.sendMessage("Режим должен быть true, false или RANDOM.");
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
                sender.sendMessage("Режим наблюдения (мобы): " + mobSpectateArg
                        + (mobPerspective == null ? "" : " (ракурс: " + mobPerspective + ")"));
                break;

            case "mobnightvision":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman mobnightvision <true/false>");
                    return true;
                }
                Boolean mobNightVisionArg = parseBooleanArgument(sender, args[1]);
                if (mobNightVisionArg == null) return true;
                boolean mobNightVision = mobNightVisionArg;
                manager.setMobNightVision(mobNightVision);
                sender.sendMessage("Ночное зрение для мобов: " + mobNightVision);
                break;
            case "showmessage":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman showmessage <true/false>");
                    return true;
                }
                Boolean showMessageArg = parseBooleanArgument(sender, args[1]);
                if (showMessageArg == null) return true;
                boolean showMessage = showMessageArg;
                manager.setShowMessage(showMessage);
                sender.sendMessage("Показывать сообщения: " + showMessage);
                break;
            case "nightvisionthreshold":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman nightvisionthreshold <0-15>");
                    return true;
                }
                try {
                    int threshold = Integer.parseInt(args[1]);
                    if (threshold < 0 || threshold > 15) {
                        sender.sendMessage("Порог должен быть от 0 до 15.");
                        return true;
                    }
                    manager.setNightVisionThreshold(threshold);
                    sender.sendMessage("Порог ночного зрения: " + threshold);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Неверный формат числа.");
                }
                break;

            case "distance":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman distance <value|min-max>");
                    return true;
                }
                String distance = args[1];
                // Optional: Validate format here, but manager handles parsing safely
                if (!isFiniteNumberOrRange(distance)) {
                    sender.sendMessage("Неверный формат. Укажите конечное число или диапазон чисел (например, 3.0-6.0).");
                    return true;
                }
                manager.setSpectateDistance(distance);
                sender.sendMessage("Расстояние камеры: " + distance);
                break;

            case "height":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman height <value|min-max>");
                    return true;
                }
                String height = args[1];
                if (!isFiniteNumberOrRange(height)) {
                    sender.sendMessage("Неверный формат. Укажите конечное число или диапазон чисел (например, 1.0-3.0).");
                    return true;
                }
                manager.setSpectateHeight(height);
                sender.sendMessage("Высота камеры: " + height);
                break;

            case "orbitspeed":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman orbitspeed <value|min-max>");
                    return true;
                }
                String speed = args[1];
                if (!isFiniteNumberOrRange(speed)) {
                    sender.sendMessage("Неверный формат. Укажите конечное число или диапазон чисел (например, 0.5-2.0).");
                    return true;
                }
                manager.setOrbitSpeed(speed);
                sender.sendMessage("Скорость вращения установлена: " + speed);
                break;

            case "orbitdirection":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman orbitdirection <LEFT|RIGHT|RANDOM>");
                    return true;
                }
                String direction = args[1].toUpperCase(Locale.ROOT);
                if (direction.equals("LEFT") || direction.equals("RIGHT") || direction.equals("RANDOM")) {
                    manager.setOrbitDirection(direction);
                    sender.sendMessage("Направление вращения установлено: " + direction);
                } else {
                    sender.sendMessage("Неверное направление. Используйте LEFT, RIGHT или RANDOM.");
                }
                break;

            case "smoothness":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman smoothness <value>");
                    return true;
                }
                try {
                    double smoothness = Double.parseDouble(args[1]);
                    if (!Double.isFinite(smoothness) || smoothness < 0.01 || smoothness > 1.0) {
                        sender.sendMessage("Плавность должна быть числом от 0.01 до 1.0.");
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage("Неверный формат плавности. Укажите число от 0.01 до 1.0.");
                    return true;
                }
                manager.setDynamicSmoothness(args[1]);
                break;

            case "flybyduration":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman flybyduration <value> (секунды)");
                    return true;
                }
                if (!isPositiveFiniteNumber(args[1])) {
                    sender.sendMessage("Параметр «длительность пролёта» должен быть конечным числом больше нуля.");
                    return true;
                }
                manager.setFlybyDuration(args[1]);
                break;

            case "craneduration":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman craneduration <value> (секунды)");
                    return true;
                }
                if (!isPositiveFiniteNumber(args[1])) {
                    sender.sendMessage("Параметр «длительность крана» должен быть конечным числом больше нуля.");
                    return true;
                }
                manager.setCraneDuration(args[1]);
                break;

            case "random":
                if (args.length < 3) {
                    sender.sendMessage("Использование: /cameraman random <player|mob> <mode1,mode2...>");
                    return true;
                }
                String type = args[1].toLowerCase(Locale.ROOT);
                if (!type.equals("player") && !type.equals("mob") && !type.equals("scenic")) {
                    sender.sendMessage("Неверный тип. Использование: /cameraman random <player|mob|scenic> ...");
                    return true;
                }

                String[] modes = args[2].split(",", -1);
                List<String> validModes = new ArrayList<>();
                for (String mode : modes) {
                    try {
                        SpectatePerspective perspective = SpectatePerspective.valueOf(mode.trim().toUpperCase(Locale.ROOT));
                        if (perspective == SpectatePerspective.RANDOM) {
                            sender.sendMessage("Режим RANDOM нельзя включать в список случайных ракурсов.");
                            return true;
                        }
                        validModes.add(perspective.name());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("Неизвестный или пустой ракурс: '" + mode + "'. Список не изменён.");
                        return true;
                    }
                }
                manager.setRandomPerspectives(type, validModes);
                break;

            case "scenic":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman scenic <true|false> [perspective]");
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
                        sender.sendMessage("Пейзажный режим включён: " + perspective + " на вашей позиции.");
                    } else {
                        // If console, use the cameraman's current location
                        Player cameraman = manager.getCameraman();
                        if (cameraman != null) {
                            manager.startScenicTask(cameraman.getLocation(), perspective);
                            sender.sendMessage("Пейзажный режим включён: " + perspective + " на позиции оператора.");
                        } else {
                            sender.sendMessage(
                                    "No cameraman set. Cannot start Scenic Mode from console without a cameraman.");
                        }
                    }
                } else {
                    manager.stopFollowing();
                    sender.sendMessage("Пейзажный режим отключён.");
                }
                break;

            case "autoscenic":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman autoscenic <true|false> [perspective]");
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
                    sender.sendMessage("Использование: /cameraman movespeed <value|min-max>");
                    return true;
                }
                if (!isFiniteNumberOrRange(args[1])) {
                    sender.sendMessage("Неверный формат скорости. Укажите конечное число или диапазон чисел.");
                    return true;
                }
                manager.setMoveSpeed(args[1]);
                break;

            case "movedirection":
                if (args.length < 4) {
                    sender.sendMessage("Использование: /cameraman movedirection <x> <y> <z> (поддерживаются диапазоны, например -1-1)");
                    return true;
                }
                if (!isFiniteNumberOrRange(args[1]) || !isFiniteNumberOrRange(args[2])
                        || !isFiniteNumberOrRange(args[3])) {
                    sender.sendMessage("Координаты направления должны быть конечными числами или диапазонами чисел.");
                    return true;
                }
                manager.setMoveDirection(args[1], args[2], args[3]);
                break;

            default:
                sender.sendMessage("Неизвестная подкоманда.");
                break;
        }

        return true;
    }
}
