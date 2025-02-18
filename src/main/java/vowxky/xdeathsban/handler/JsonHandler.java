package vowxky.xdeathsban.handler;

import com.google.gson.*;
import vowxky.xdeathsban.Constant;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;

public class JsonHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FOLDER = Constant.pathConfig();
    private static final Path BANS_FOLDER = CONFIG_FOLDER.resolve("bans");
    private static final Path CONFIG_FILE = CONFIG_FOLDER.resolve("config.json");

    private static JsonObject configCache = new JsonObject();

    public static void initialize() {
        try {
            Files.createDirectories(CONFIG_FOLDER);
            Files.createDirectories(BANS_FOLDER);
            configCache = Files.exists(CONFIG_FILE) ? loadJson(CONFIG_FILE) : createDefaultConfig();
        } catch (IOException e) {
            Constant.LOGGER.error("Error initializing the ban system", e);
        }
    }

    private static JsonObject createDefaultConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("maxDeaths", 3);
        config.addProperty("banTemporary", false);
        config.addProperty("banTimeTicks", 6000);
        config.addProperty("onlyPvPDeaths", false);
        saveJson(CONFIG_FILE, config);
        return config;
    }

    private static JsonObject loadJson(Path filePath) {
        if (!Files.exists(filePath)) return new JsonObject();
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Exception e) {
            Constant.LOGGER.error("Error reading JSON from {}", filePath, e);
            return new JsonObject();
        }
    }

    private static void saveJson(Path filePath, JsonObject jsonObject) {
        try (Writer writer = new FileWriter(filePath.toFile())) {
            GSON.toJson(jsonObject, writer);
        } catch (IOException e) {
            Constant.LOGGER.error("Error saving JSON to {}", filePath, e);
        }
    }

    public static void reloadConfig() {
        try {
            configCache = Files.exists(CONFIG_FILE) ? loadJson(CONFIG_FILE) : createDefaultConfig();
            Constant.LOGGER.info("Config reloaded successfully.");
        } catch (Exception e) {
            Constant.LOGGER.error("Error reloading config", e);
        }
    }

    public static int getMaxDeaths() {
        return configCache.get("maxDeaths").getAsInt();
    }

    public static boolean isBanTemporary() {
        return configCache.get("banTemporary").getAsBoolean();
    }

    public static int getBanTimeTicks() {
        return configCache.get("banTimeTicks").getAsInt();
    }

    public static boolean getOnlyPvPDeaths() {
        return configCache.has("onlyPvPDeaths") && configCache.get("onlyPvPDeaths").getAsBoolean();
    }

    private static Path getPlayerFile(String playerUUID) {
        return BANS_FOLDER.resolve(playerUUID + ".json");
    }

    public static void setMaxDeaths(int value) {
        configCache.addProperty("maxDeaths", value);
        saveJson(CONFIG_FILE, configCache);
    }

    public static void setBanTemporary(boolean enabled) {
        configCache.addProperty("banTemporary", enabled);
        saveJson(CONFIG_FILE, configCache);
    }

    public static void setBanTimeTicks(int ticks) {
        configCache.addProperty("banTimeTicks", ticks);
        saveJson(CONFIG_FILE, configCache);
    }

    public static void setOnlyPvPDeaths(boolean enabled) {
        configCache.addProperty("onlyPvPDeaths", enabled);
        saveJson(CONFIG_FILE, configCache);
    }

    public static boolean registerDeath(String playerUUID, long currentServerTick, String playerName) {
        Path playerFile = getPlayerFile(playerUUID);
        JsonObject playerData = loadJson(playerFile);

        int deaths = Optional.ofNullable(playerData.get("deaths")).map(JsonElement::getAsInt).orElse(0) + 1;
        playerData.addProperty("deaths", deaths);
        playerData.addProperty("playerName", playerName);

        if (deaths >= getMaxDeaths()) {
            playerData.addProperty("banned", true);

            if (isBanTemporary()) {
                playerData.addProperty("banTicks", getBanTimeTicks());
            }

            saveJson(playerFile, playerData);
            return true;
        }

        saveJson(playerFile, playerData);
        return false;
    }

    public static boolean isBanned(String playerUUID) {
        Path playerFile = getPlayerFile(playerUUID);
        JsonObject playerData = loadJson(playerFile);

        return playerData.has("banned") && playerData.get("banned").getAsBoolean();
    }

    public static long getRemainingBanTicks(String playerUUID) {
        Path playerFile = getPlayerFile(playerUUID);
        JsonObject playerData = loadJson(playerFile);

        if (!playerData.has("banTicks") || !playerData.get("banned").getAsBoolean()) {
            return 0L;
        }

        return playerData.get("banTicks").getAsLong();
    }

    public static void unbanPlayer(String playerUUID) {
        Path playerFile = getPlayerFile(playerUUID);
        try {
            if (Files.exists(playerFile)) {
                Files.delete(playerFile);
                Constant.LOGGER.info("Player {} has been unbanned and their file deleted.", playerUUID);
            }
        } catch (IOException e) {
            Constant.LOGGER.error("Error deleting ban file for player: {}", playerUUID, e);
        }
    }

    public static void ticksPlayersBan() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BANS_FOLDER)) {
            for (Path file : stream) {
                JsonObject playerData = loadJson(file);
                if (!playerData.has("banTicks") || !isBanTemporary()) {
                    continue;
                }

                int banTicks = playerData.get("banTicks").getAsInt();

                if (banTicks > 0) {
                    playerData.addProperty("banTicks", banTicks - 1);
                    saveJson(file, playerData);
                }

                if (banTicks <= 1) {
                    String playerUUID = file.getFileName().toString().replace(".json", "");
                    unbanPlayer(playerUUID);
                }
            }
        } catch (IOException e) {
            Constant.LOGGER.error("Error checking expired bans.", e);
        }
    }

    public static List<String> getAllBannedPlayerNames() {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BANS_FOLDER)) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .map(JsonHandler::loadJson)
                    .filter(json -> json.has("banned") && json.get("banned").getAsBoolean()) 
                    .map(json -> json.has("playerName") ? json.get("playerName").getAsString() : null)
                    .filter(Objects::nonNull)
                    .toList();
        } catch (IOException e) {
            Constant.LOGGER.error("Error retrieving banned player names.", e);
            return List.of();
        }
    }

    public static String getBannedUUIDByName(String playerName) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BANS_FOLDER)) {
            return StreamSupport.stream(stream.spliterator(), false)
                    .filter(file -> {
                        JsonObject playerData = loadJson(file);
                        return playerData.has("playerName") && playerData.get("playerName").getAsString().equalsIgnoreCase(playerName);
                    })
                    .map(file -> file.getFileName().toString().replace(".json", "")) 
                    .findFirst()
                    .orElse(null);
        } catch (IOException e) {
            Constant.LOGGER.error("Error retrieving banned player UUID by name: {}", playerName, e);
            return null;
        }
    }

    public static int unbanAllPlayers() {
        int unbannedCount = 0;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BANS_FOLDER)) {
            for (Path file : stream) {
                Files.delete(file);
                unbannedCount++;
            }
        } catch (IOException e) {
            Constant.LOGGER.error("Error unbanning all players.", e);
        }
        return unbannedCount;
    }
}
