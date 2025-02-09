package vowxky.xdeathsban.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import vowxky.xdeathsban.Constant;
import vowxky.xdeathsban.command.provider.BansSuggestion;
import vowxky.xdeathsban.handler.JsonHandler;

import java.util.List;

/**
 * This class was created by Vowxky.
 * All rights reserved to the developer.
 */

public class XDeathsBanCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("xdeathsban")
                .requires(source -> source.hasPermissionLevel(3))

                .then(CommandManager.literal("settings")
                        .then(CommandManager.literal("status")
                                .executes(context -> displayStatus(context.getSource()))
                        )
                        .then(CommandManager.literal("maxdeaths")
                                .then(CommandManager.argument("value", IntegerArgumentType.integer(1))
                                        .executes(XDeathsBanCommand::setMaxDeaths)
                                )
                        )
                        .then(CommandManager.literal("onlypvp")
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(XDeathsBanCommand::setOnlyPvPDeaths)
                                )
                        )
                )


                .then(CommandManager.literal("ban")
                        .then(CommandManager.literal("bantemp")
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(XDeathsBanCommand::setBanTemporary)
                                )
                        )
                        .then(CommandManager.literal("bantime")
                                .then(CommandManager.argument("ticks", IntegerArgumentType.integer(20))
                                        .executes(XDeathsBanCommand::setBanTimeTicks)
                                )
                        )
                )

                .then(CommandManager.literal("unban")
                        .then(CommandManager.argument("playerName", StringArgumentType.string())
                                .suggests(new BansSuggestion())
                                .executes(XDeathsBanCommand::unbanPlayer)
                        )
                        .then(CommandManager.literal("all")
                                .executes(context -> unbanAllPlayers(context.getSource()))
                        )
                )

                .then(CommandManager.literal("util")
                        .then(CommandManager.literal("listbans")
                                .executes(context -> listBans(context.getSource()))
                        )

                        .then(CommandManager.literal("reload")
                                .executes(context -> reloadConfig(context.getSource()))
                        )
                )
        );
    }

    private static int displayStatus(ServerCommandSource source) {
        source.sendFeedback(() -> Text.literal("§6=============================="), false);
        source.sendFeedback(() -> Text.literal("§e      ✦ XDeathsBan Settings ✦"), false);
        source.sendFeedback(() -> Text.literal("§6=============================="), false);
        source.sendFeedback(() -> Text.literal("§a✔ Max Deaths: §f" + JsonHandler.getMaxDeaths()), false);
        source.sendFeedback(() -> Text.literal("§a✔ Temporary Ban: " + (JsonHandler.isBanTemporary() ? "§2Enabled" : "§4Disabled")), false);
        source.sendFeedback(() -> Text.literal("§a✔ Ban Duration: §f" + JsonHandler.getBanTimeTicks() + " ticks"), false);
        source.sendFeedback(() -> Text.literal("§a✔ Only PvP Deaths: " + (JsonHandler.getOnlyPvPDeaths() ? "§2Enabled" : "§4Disabled")), false);
        source.sendFeedback(() -> Text.literal("§6=============================="), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int setMaxDeaths(CommandContext<ServerCommandSource> context) {
        int value = IntegerArgumentType.getInteger(context, "value");
        JsonHandler.setMaxDeaths(value);
        context.getSource().sendFeedback(() -> Text.literal("§a✔ Max deaths set to: §f" + value), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setBanTemporary(CommandContext<ServerCommandSource> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        JsonHandler.setBanTemporary(enabled);
        context.getSource().sendFeedback(() -> Text.literal("§a✔ Temporary bans " + (enabled ? "§2enabled" : "§4disabled")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setBanTimeTicks(CommandContext<ServerCommandSource> context) {
        int ticks = IntegerArgumentType.getInteger(context, "ticks");
        JsonHandler.setBanTimeTicks(ticks);
        context.getSource().sendFeedback(() -> Text.literal("§a✔ Ban duration set to: §f" + ticks + " ticks"), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int setOnlyPvPDeaths(CommandContext<ServerCommandSource> context) {
        boolean enabled = BoolArgumentType.getBool(context, "enabled");
        JsonHandler.setOnlyPvPDeaths(enabled);
        context.getSource().sendFeedback(() -> Text.literal("§a✔ Only PvP Deaths " + (enabled ? "§2enabled" : "§4disabled")), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int unbanPlayer(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        String playerName = StringArgumentType.getString(context, "playerName");

        try {
            String playerUUID = JsonHandler.getBannedUUIDByName(playerName);
            if (playerUUID == null || !JsonHandler.isBanned(playerUUID)) {
                source.sendError(Text.literal("§c✖ That player is not banned or was not found in the ban list."));
                return 0;
            }

            JsonHandler.unbanPlayer(playerUUID);
            source.sendFeedback(() -> Text.literal("§a✔ Player §f" + playerName + " §ahas been unbanned."), true);
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendError(Text.literal("§c✖ An error occurred while unbanning the player. Check logs for details."));
            Constant.LOGGER.error("Error unbanning player: {}", playerName, e);
            return 0;
        }
    }

    private static int unbanAllPlayers(ServerCommandSource source) {
        int unbannedCount = JsonHandler.unbanAllPlayers();
        source.sendFeedback(() -> Text.literal("§a✔ Successfully unbanned §f" + unbannedCount + " §aplayers."), true);
        return Command.SINGLE_SUCCESS;
    }

    private static int listBans(ServerCommandSource source) {
        List<String> bannedPlayers = JsonHandler.getAllBannedPlayerNames();

        if (bannedPlayers.isEmpty()) {
            source.sendFeedback(() -> Text.literal("§a✔ No players are currently banned."), false);
        } else {
            source.sendFeedback(() -> Text.literal("§c✖ Currently Banned Players: §f" + String.join(", ", bannedPlayers)), false);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int reloadConfig(ServerCommandSource source) {
        JsonHandler.reloadConfig();
        source.sendFeedback(() -> Text.literal("§a✔ Successfully reload config"), true);
        return Command.SINGLE_SUCCESS;
    }
}
