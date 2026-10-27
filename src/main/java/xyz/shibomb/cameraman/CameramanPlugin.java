package xyz.shibomb.cameraman;

import org.bukkit.plugin.java.JavaPlugin;

public final class CameramanPlugin extends JavaPlugin {

    private OperatorRegistry operators;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getLogger().info("Cameraman plugin has been enabled!");

        this.operators = new OperatorRegistry(this);
        CameramanMenu menu = new CameramanMenu(this);
        CameramanCommand cameramanCommand = new CameramanCommand(this.operators, menu);
        getCommand("cameraman").setExecutor(cameramanCommand);
        getCommand("cameraman").setTabCompleter(cameramanCommand);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this.operators), this);
        getServer().getPluginManager().registerEvents(menu, this);
    }

    @Override
    public void onDisable() {
        if (operators != null) operators.stopAllTasks();
        getLogger().info("Cameraman plugin has been disabled!");
    }

    public OperatorRegistry getOperatorRegistry() {
        return operators;
    }

    public CameramanManager getCameramanManager() {
        return operators.getPrimaryManager();
    }
}
