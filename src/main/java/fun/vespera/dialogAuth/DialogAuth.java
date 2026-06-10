package fun.vespera.dialogAuth;

import fun.vespera.dialogAuth.command.AuthCommand;
import fun.vespera.dialogAuth.api.PremiumAuth;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public final class DialogAuth extends JavaPlugin {

    private DatabaseManager db;
    private ExecutorService asyncExecutor;
    private PremiumAuth api;

    @Override
    public void onEnable() {

        // if folder doesnt exist - creting it
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        // dont remove this sign please :)
        var mm = MiniMessage.miniMessage();
        String version = getDescription().getVersion();

        Bukkit.getConsoleSender().sendMessage(mm.deserialize(""));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7>  _____  _       _                           _   _     "));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7> |  __ \\(_)     | |               /\\        | | | |    "));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7> | |  | |_  __ _| | ___   __ _   /  \\  _   _| |_| |__  "));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7> | |  | | |/ _` | |/ _ \\ / _` | / /\\ \\| | | | __| '_ \\ "));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7> | |__| | | (_| | | (_) | (_| |/ ____ \\ |_| | |_| | | |"));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7> |_____/|_|\\__,_|_|\\___/ \\__, /_/    \\_\\__,_|\\__|_| |_|"));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7>                          __/ |                        "));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize("<#BDB6E7>                         |___/                         <#64F8A7>v" + version));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(""));

        saveDefaultConfig();

        asyncExecutor = Executors.newFixedThreadPool(4);

        // enabling database
        db = new DatabaseManager(
                getConfig().getString("database.type", "h2"),
                getConfig().getString("database.host", "127.0.0.1"),
                getConfig().getInt("database.port", 3306),
                getConfig().getString("database.database", "DialogAuth"),
                getConfig().getString("database.username", "root"),
                getConfig().getString("database.password", ""),
                getDataFolder().getAbsolutePath(),
                asyncExecutor
        );

        // is API enabled?
        boolean apiEnabled = getConfig().getBoolean("api.enabled", true);

        // enabling API if enabled
        if (apiEnabled) {
            int apiPort = getConfig().getInt("api.port", 8080);
            String apiHost = getConfig().getString("api.host", "0.0.0.0");
            api = new PremiumAuth(this);
            api.start(apiHost, apiPort);
        } else {
            getLogger().info("API disabled in config.yml");
        }


        AuthCommand authCommand = new AuthCommand(this);

        // !event registration!
        getServer().getPluginManager().registerEvents(new AuthManager(this), this);
        getServer().getPluginManager().registerEvents(authCommand, this);

        // !command registration! (they are all in single "AuthCommand" class)
        getCommand("forceunreg").setExecutor(authCommand);
        getCommand("forcechangepass").setExecutor(authCommand);
        getCommand("dauth").setExecutor(authCommand);


    }

    @Override
    public void onDisable() {

        // disabling API if it was enabled
        if (api != null) {
            api.stop();
        }

        // disabling DataBase if it was enabled
        if (db != null) {
            db.close();
        }

        // disabling asyncExecutor
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(3, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
            }
        }
    }

    public DatabaseManager getDb() {
        return db;
    }

    public ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }
}