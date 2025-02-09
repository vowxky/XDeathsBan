package vowxky.xdeathsban.command.provider;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.server.command.ServerCommandSource;
import vowxky.xdeathsban.handler.JsonHandler;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * This class was created by Vowxky.
 * All rights reserved to the developer.
 */

public class BansSuggestion implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        List<String> bannedPlayerNames = JsonHandler.getAllBannedPlayerNames();

        return net.minecraft.command.CommandSource.suggestMatching(bannedPlayerNames, builder);
    }
}