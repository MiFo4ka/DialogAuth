package fun.vespera.dialogAuth.api;

import com.sun.net.httpserver.HttpServer;
import fun.vespera.dialogAuth.DatabaseManager;
import fun.vespera.dialogAuth.DialogAuth;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class PremiumAuth {
    private final DialogAuth plugin;
    private HttpServer server;

    public PremiumAuth(DialogAuth plugin) {
        this.plugin = plugin;
    }

    public void start(String hostname,int port) {
        try {
            server = HttpServer.create(new InetSocketAddress(hostname, port), 0);

            // 1-st endpoint, getting premium status
            server.createContext("/api/isPremium", exchange -> {
                // false response by default(for security)
                String query = exchange.getRequestURI().getQuery();
                String response = "false";

                if (query != null && query.startsWith("username=")) {
                    // get players nickname
                    String username = query.substring(9);
                    // checkin isPremium in db
                    DatabaseManager.PlayerData data = plugin.getDb().getPlayerData(username).join();
                    if (data != null && data.isPremium()) {
                        // changing response to true
                        response = "true";
                    }
                }

                // setting http code: 200 (success)
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            // 2-nd endpoint, setting premium status
            server.createContext("/api/setPremium", exchange -> {
                // error response by default(for security)
                String query = exchange.getRequestURI().getQuery();
                String response = "error";
                int statusCode = 400;

                if (query != null) {
                    String[] params = query.split("&");
                    String username = null;
                    boolean status = false;

                    // parsing query parameters
                    for (String param : params) {
                        if (param.startsWith("username=")) username = param.substring(9);
                        if (param.startsWith("status=")) status = Boolean.parseBoolean(param.substring(7));
                    }

                    if (username != null) {
                        DatabaseManager.PlayerData data = plugin.getDb().getPlayerData(username).join();
                        if (data != null) {
                            // updating table with isPremium=true status
                            DatabaseManager.PlayerData newData = new DatabaseManager.PlayerData(
                                    data.uuid(), data.username(), data.passwordHash(),
                                    data.lastIp(), data.lastLogin(), status
                            );
                            plugin.getDb().savePlayer(newData).join();
                            // changing response to success
                            response = "success";
                            statusCode = 200;
                        } else {
                            // if player not found, changing to player not found (too ez)
                            response = "player_not_found";
                            statusCode = 404;
                        }
                    }
                }

                exchange.sendResponseHeaders(statusCode, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            });

            server.setExecutor(plugin.getAsyncExecutor());
            server.start();
            plugin.getLogger().info("API enabled on port: " + port);

        } catch (IOException e) {
            plugin.getLogger().severe("An error occurred while attempting to enable API on port: " + port);
            e.printStackTrace();
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}