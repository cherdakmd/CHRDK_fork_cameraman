package xyz.shibomb.cameraman;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
                boolean newcomerMode = Boolean.parseBoolean(args[1]);
                manager.setNewcomerMode(newcomerMode);
                sender.sendMessage("Режим новичков: " + newcomerMode);
                break;

            case "rotation":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman rotation <true|false> [interval]");
                    return true;
                }
                boolean rotEnabled = Boolean.parseBoolean(args[1]);
                long interval = 60L;
                if (args.length >= 3) {
                    try {
                        interval = Long.parseLong(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Неверный интервал (секунды). Используется значение по умолчанию: 60 с.");
                    }
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
                    manager.setMinRotationDistance(Math.max(0.0, dist));
                    sender.sendMessage("Минимальное расстояние ротации: " + Math.max(0.0, dist));
                } catch (NumberFormatException e) {
                    sender.sendMessage("Неверное расстояние.");
                }
                break;

            case "mobtarget":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman mobtarget <true/false>");
                    return true;
                }
                boolean mobTargetMode = Boolean.parseBoolean(args[1]);
                manager.setMobTargetMode(mobTargetMode);
                sender.sendMessage("Режим выбора мобов: " + mobTargetMode);
                break;

            case "automob":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman automob <true/false> [delay]");
                    return true;
                }
                boolean autoMobTarget = Boolean.parseBoolean(args[1]);
                long delay = 5; // Default
                if (args.length >= 3) {
                    try {
                        delay = Long.parseLong(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Неверная задержка. Используется значение по умолчанию: 5 с.");
                    }
                }
                manager.setAutoMobTarget(autoMobTarget, delay);
                sender.sendMessage("Автовыбор мобов: " + autoMobTarget);
                break;

            case "teleportsmooth":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman teleportsmooth <true/false> [duration]");
                    return true;
                }
                boolean teleportSmooth = Boolean.parseBoolean(args[1]);
                long duration = 3; // Default
                if (args.length >= 3) {
                    try {
                        duration = Long.parseLong(args[2]);
                    } catch (NumberFormatException e) {
                        sender.sendMessage("Неверная длительность. Используется значение по умолчанию: 3 с.");
                    }
                }
                manager.setTeleportSmooth(teleportSmooth, duration);
                sender.sendMessage("Плавная телепортация: " + teleportSmooth);
                break;

            case "spectatemode":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman spectatemode <true/false/RANDOM> [perspective]");
                    return true;
                }
                String spectateArg = args[1];
                manager.setSpectateMode(spectateArg);

                if (args.length >= 3) {
                    try {
                        SpectatePerspective perspective = SpectatePerspective.valueOf(args[2].toUpperCase());
                        manager.setSpectatePerspective(perspective);
                        sender.sendMessage(
                                "Режим наблюдения (игроки): " + spectateArg + " (Perspective: " + perspective + ")");
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(
                                "Неверный ракурс. Доступные ракурсы: POV, BEHIND, FRONT, RANDOM, ORBIT, DYNAMIC, DYNAMIC_BEHIND, DYNAMIC_FRONT, DYNAMIC_POV, FLYBY, CRANE, FIX");
                        sender.sendMessage("Режим наблюдения (игроки): " + spectateArg);
                    }
                } else {
                    sender.sendMessage("Режим наблюдения (игроки): " + spectateArg);
                }
                break;

            case "mobspectatemode":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman mobspectatemode <true/false/RANDOM> [perspective]");
                    return true;
                }
                String mobSpectateArg = args[1];
                manager.setMobSpectateMode(mobSpectateArg);

                if (args.length >= 3) {
                    try {
                        SpectatePerspective perspective = SpectatePerspective.valueOf(args[2].toUpperCase());
                        manager.setMobSpectatePerspective(perspective);
                        sender.sendMessage(
                                "Режим наблюдения (мобы): " + mobSpectateArg + " (Perspective: " + perspective + ")");
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage(
                                "Неверный ракурс. Доступные ракурсы: POV, BEHIND, FRONT, RANDOM, ORBIT, DYNAMIC, DYNAMIC_BEHIND, DYNAMIC_FRONT, DYNAMIC_POV, FLYBY, CRANE, FIX");
                        sender.sendMessage("Режим наблюдения (мобы): " + mobSpectateArg);
                    }
                } else {
                    sender.sendMessage("Режим наблюдения (мобы): " + mobSpectateArg);
                }
                break;

            case "mobnightvision":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman mobnightvision <true/false>");
                    return true;
                }
                boolean mobNightVision = Boolean.parseBoolean(args[1]);
                manager.setMobNightVision(mobNightVision);
                sender.sendMessage("Ночное зрение для мобов: " + mobNightVision);
                break;
            case "showmessage":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman showmessage <true/false>");
                    return true;
                }
                boolean showMessage = Boolean.parseBoolean(args[1]);
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
                try {
                    // Check if it's a number or a range
                    if (!distance.contains("-")) {
                        Double.parseDouble(distance);
                    } else {
                        String[] parts = distance.split("-");
                        if (parts.length != 2)
                            throw new NumberFormatException();
                        Double.parseDouble(parts[0].trim());
                        Double.parseDouble(parts[1].trim());
                    }
                    manager.setSpectateDistance(distance);
                    sender.sendMessage("Расстояние камеры: " + distance);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Неверный формат. Укажите число (например, 3.0) или диапазон (например, 3.0-6.0).");
                }
                break;

            case "height":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman height <value|min-max>");
                    return true;
                }
                String height = args[1];
                try {
                    if (!height.contains("-")) {
                        Double.parseDouble(height);
                    } else {
                        String[] parts = height.split("-");
                        if (parts.length != 2)
                            throw new NumberFormatException();
                        Double.parseDouble(parts[0].trim());
                        Double.parseDouble(parts[1].trim());
                    }
                    manager.setSpectateHeight(height);
                    sender.sendMessage("Высота камеры: " + height);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Неверный формат. Укажите число (например, 1.0) или диапазон (например, 1.0-3.0).");
                }
                break;

            case "orbitspeed":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman orbitspeed <value|min-max>");
                    return true;
                }
                String speed = args[1];
                try {
                    if (!speed.contains("-")) {
                        Double.parseDouble(speed);
                    } else {
                        String[] parts = speed.split("-");
                        if (parts.length != 2)
                            throw new NumberFormatException();
                        Double.parseDouble(parts[0].trim());
                        Double.parseDouble(parts[1].trim());
                    }
                    manager.setOrbitSpeed(speed);
                    sender.sendMessage("Скорость вращения установлена: " + speed);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid format. Use a number (e.g. 1.0) or range (e.g. 0.5-2.0).");
                }
                break;

            case "orbitdirection":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman orbitdirection <LEFT|RIGHT|RANDOM>");
                    return true;
                }
                String direction = args[1].toUpperCase();
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
                manager.setDynamicSmoothness(args[1]);
                break;

            case "flybyduration":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman flybyduration <value> (секунды)");
                    return true;
                }
                manager.setFlybyDuration(args[1]);
                break;

            case "craneduration":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman craneduration <value> (секунды)");
                    return true;
                }
                manager.setCraneDuration(args[1]);
                break;

            case "random":
                if (args.length < 3) {
                    sender.sendMessage("Использование: /cameraman random <player|mob> <mode1,mode2...>");
                    return true;
                }
                String type = args[1].toLowerCase();
                if (!type.equals("player") && !type.equals("mob") && !type.equals("scenic")) {
                    sender.sendMessage("Неверный тип. Использование: /cameraman random <player|mob|scenic> ...");
                    return true;
                }

                String[] modes = args[2].toUpperCase().split(",");
                List<String> validModes = new ArrayList<>();
                for (String mode : modes) {
                    try {
                        SpectatePerspective p = SpectatePerspective.valueOf(mode);
                        if (p != SpectatePerspective.RANDOM) {
                            validModes.add(p.name());
                        }
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("Предупреждение: неизвестный режим пропущен: '" + mode + "'");
                    }
                }

                if (validModes.isEmpty()) {
                    sender.sendMessage("Не указано ни одного допустимого режима. Список не изменён.");
                    return true;
                }
                manager.setRandomPerspectives(type, validModes);
                break;

            case "scenic":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman scenic <true|false> [perspective]");
                    return true;
                }
                boolean scenicEnable = Boolean.parseBoolean(args[1]);
                if (scenicEnable) {
                    SpectatePerspective perspective = SpectatePerspective.POV;
                    if (args.length >= 3) {
                        try {
                            perspective = SpectatePerspective.valueOf(args[2].toUpperCase());
                        } catch (IllegalArgumentException e) {
                            sender.sendMessage("Неверный ракурс.");
                            return true;
                        }
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
                boolean autoScenic = Boolean.parseBoolean(args[1]);
                SpectatePerspective autoScenicPerspective = null;
                if (args.length >= 3) {
                    try {
                        autoScenicPerspective = SpectatePerspective.valueOf(args[2].toUpperCase());
                    } catch (IllegalArgumentException e) {
                        sender.sendMessage("Неверный ракурс.");
                        return true;
                    }
                }
                manager.setAutoScenic(autoScenic, autoScenicPerspective);
                break;

            case "movespeed":
                if (args.length < 2) {
                    sender.sendMessage("Использование: /cameraman movespeed <value|min-max>");
                    return true;
                }
                manager.setMoveSpeed(args[1]);
                break;

            case "movedirection":
                if (args.length < 4) {
                    sender.sendMessage("Использование: /cameraman movedirection <x> <y> <z> (поддерживаются диапазоны, например -1-1)");
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
