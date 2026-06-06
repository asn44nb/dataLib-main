package runtoolkit.datalib.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import runtoolkit.datalib.config.DataLibConfig;
import runtoolkit.datalib.core.DataLibCore;

public class DataLibCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("datalib")
                    .requires(source -> source.hasPermissionLevel(2))
                    // /datalib — reload only dataLib
                    .executes(DataLibCommand::executeReload)
                    // /datalib reload — reload only dataLib
                    .then(CommandManager.literal("reload")
                        .executes(DataLibCommand::executeReload)
                    )
                    // /datalib config <key> <value>
                    .then(CommandManager.literal("config")
                        .then(CommandManager.argument("key", StringArgumentType.word())
                            .suggests(CONFIG_KEY_SUGGESTIONS)
                            .then(CommandManager.argument("value", StringArgumentType.greedyString())
                                .executes(DataLibCommand::executeConfigSet)
                            )
                            .executes(DataLibCommand::executeConfigGet)
                        )
                        .executes(DataLibCommand::executeConfigList)
                    )
                    // /datalib status
                    .then(CommandManager.literal("status")
                        .executes(DataLibCommand::executeStatus)
                    )
            );
        });

        DataLibCore.LOGGER.info("[DataLib] Commands registered.");
    }

    private static final SuggestionProvider<ServerCommandSource> CONFIG_KEY_SUGGESTIONS = (context, builder) -> {
        DataLibCore.getInstance().getConfig().getAll().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };

    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        // Reload only dataLib config + engine — do NOT reload other datapacks
        DataLibCore.getInstance().getConfig().load();
        DataLibCore.getInstance().getEngine().reload(source.getServer());

        source.sendFeedback(() -> Text.literal("§a[DataLib] §fDataLib reloaded successfully. (Only dataLib was reloaded)"), true);
        return 1;
    }

    private static int executeConfigSet(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key");
        String value = StringArgumentType.getString(context, "value");
        ServerCommandSource source = context.getSource();

        DataLibConfig config = DataLibCore.getInstance().getConfig();
        if (!config.hasKey(key)) {
            source.sendError(Text.literal("§c[DataLib] §fUnknown config key: §e" + key));
            return 0;
        }

        config.set(key, value);
        source.sendFeedback(() -> Text.literal("§a[DataLib] §fConfig §e" + key + " §fset to §b" + value), true);
        return 1;
    }

    private static int executeConfigGet(CommandContext<ServerCommandSource> context) {
        String key = StringArgumentType.getString(context, "key");
        ServerCommandSource source = context.getSource();

        DataLibConfig config = DataLibCore.getInstance().getConfig();
        String value = config.get(key);
        if (value == null) {
            source.sendError(Text.literal("§c[DataLib] §fUnknown config key: §e" + key));
            return 0;
        }

        source.sendFeedback(() -> Text.literal("§a[DataLib] §e" + key + " §f= §b" + value), false);
        return 1;
    }

    private static int executeConfigList(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        DataLibConfig config = DataLibCore.getInstance().getConfig();

        source.sendFeedback(() -> Text.literal("§a[DataLib] §fConfiguration:"), false);
        config.getAll().forEach((key, value) ->
            source.sendFeedback(() -> Text.literal("  §e" + key + " §f= §b" + value), false)
        );
        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        boolean engineLoaded = DataLibCore.getInstance().getEngine().isLoaded();
        int packCount = DataLibCore.getInstance().getEngine().getManagedPackCount();

        source.sendFeedback(() -> Text.literal("§a[DataLib] §fStatus:"), false);
        source.sendFeedback(() -> Text.literal("  §fEngine: " + (engineLoaded ? "§aLoaded" : "§cNot loaded")), false);
        source.sendFeedback(() -> Text.literal("  §fManaged packs: §b" + packCount), false);
        source.sendFeedback(() -> Text.literal("  §fVersion: §b6.0.0"), false);
        return 1;
    }
}
