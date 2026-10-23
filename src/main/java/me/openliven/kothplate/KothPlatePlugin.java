package me.openliven.kothplate;

import me.openliven.kothplate.capture.CaptureService;
import me.openliven.kothplate.capture.CaptureListener;
import me.openliven.kothplate.command.KothPlateCommand;
import me.openliven.kothplate.config.PluginSettings;
import me.openliven.kothplate.config.SettingsLoader;
import me.openliven.kothplate.plate.PlateService;
import me.openliven.kothplate.service.MessageService;
import me.openliven.kothplate.service.VisualEffectService;
import me.openliven.kothplate.service.VaultEconomyService;
import me.openliven.kothplate.service.AfterRewardActionService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Clock;

public final class KothPlatePlugin extends JavaPlugin {
    private SettingsLoader settingsLoader;
    private PluginSettings settings;
    private MessageService messages;
    private PlateService plates;
    private CaptureService captures;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        settingsLoader = new SettingsLoader(this);
        messages = new MessageService(this);
        settings = settingsLoader.load();
        messages.load(settings.language());
        plates = new PlateService();
        captures = new CaptureService(
                this,
                new VaultEconomyService(this::resolveEconomy),
                messages,
                new VisualEffectService(getLogger()),
                new AfterRewardActionService(),
                plates,
                Clock.systemUTC()
        );
        captures.updateSettings(settings);

        KothPlateCommand commandHandler = new KothPlateCommand(this);
        PluginCommand command = getCommand("koth");
        if (command == null) {
            getLogger().severe("Command koth is missing from plugin.yml.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        Bukkit.getPluginManager().registerEvents(new CaptureListener(this, captures), this);

        // Economy plugins (EssentialsX, CMI, ...) may register their Vault provider after we enable.
        // Delayed tasks run once every plugin has enabled, so check the provider then instead of now.
        Bukkit.getScheduler().runTask(this, this::verifyEconomyAvailable);
    }

    @Override
    public void onDisable() {
        if (captures != null) {
            captures.cancelActiveCapture(false);
        }
    }

    public PluginSettings settings() {
        return settings;
    }

    public MessageService messages() {
        return messages;
    }

    public PlateService plates() {
        return plates;
    }

    public void reloadPluginSettings() {
        settings = settingsLoader.load();
        messages.load(settings.language());
        captures.updateSettings(settings);
    }

    private void verifyEconomyAvailable() {
        if (!isEnabled()) {
            return;
        }
        Economy economy = resolveEconomy();
        if (economy == null) {
            getLogger().severe("[KingOfTheHill] Vault или плагин экономики не найдены! Выключение...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getLogger().info("Using economy provider: " + economy.getName());
    }

    private Economy resolveEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }

        RegisteredServiceProvider<Economy> registration = getServer()
                .getServicesManager()
                .getRegistration(Economy.class);
        return registration == null ? null : registration.getProvider();
    }
}
