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

/**
 * /datalib command — mod-side only, does NOT interfere with
 * the datapack-side commands (datalib_menu trigger, dl_run trigger, etc.).
 *
 * /datalib           — reload only dataLib mod config (not /reload)
 * /datalib reload    — same as above
 * /datalib config    — list all config keys
 * /datalib config k  — get value of key k
 * /datalib config k v — set key k to value v
 * /datalib status    — engine + managed-pack info
 * /datalib check     — run datalib:debug/tools/utils/check_all
 */
public class DataLibCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(
                CommandManager.literal("datalib")
                    .requires(source -> source.hasPermissionLevel(2))
                    // /datalib — reload only dataLib mod config
                    .executes(DataLibCommand::executeReload)
                    // /datalib reload
                    .then(CommandManager.literal("reload")
                        .executes(DataLibCommand::executeReload)
                    )
                    // /datalib config ...
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
                    // /datalib check — run check_all
                    .then(CommandManager.literal("check")
                        .executes(DataLibCommand::executeCheck)
                    )
            );
        });

        DataLibCore.LOGGER.info("[DataLib] Commands registered.");
    }

    private static final SuggestionProvider<ServerCommandSource> CONFIG_KEY_SUGGESTIONS = (context, builder) -> {
        DataLibCore.getInstance().getConfig().getAll().keySet().forEach(builder::suggest);
        return builder.buildFuture();
    };

    /**
     * Reloads only the dataLib mod config file.
     * Does NOT call /reload, does NOT re-trigger the dl_load gate,
     * does NOT affect other datapacks at all.
     */
    private static int executeReload(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        DataLibCore.getInstance().getEngine().reload(source.getServer());

        source.sendFeedback(() -> Text.literal(
                "§a[DataLib] §fMod config reloaded. " +
                "§7(Only dataLib — no /reload triggered)"
        ), true);
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
        source.sendFeedback(() -> Text.literal(
                "§a[DataLib] §fConfig §e" + key + " §fset to §b" + value
        ), true);
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

        source.sendFeedback(() -> Text.literal(
                "§a[DataLib] §e" + key + " §f= §b" + value
        ), false);
        return 1;
    }

    private static int executeConfigList(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        DataLibConfig config = DataLibCore.getInstance().getConfig();

        source.sendFeedback(() -> Text.literal("§a[DataLib] §fConfiguration:"), false);
        config.getAll().forEach((key, value) ->
            source.sendFeedback(() -> Text.literal(
                    "  §e" + key + " §f= §b" + value
            ), false)
        );
        return 1;
    }

    private static int executeStatus(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();
        boolean engineLoaded = DataLibCore.getInstance().getEngine().isLoaded();
        int packCount = DataLibCore.getInstance().getEngine().getManagedPackCount();

        source.sendFeedback(() -> Text.literal("§a[DataLib] §fStatus:"), false);
        source.sendFeedback(() -> Text.literal(
                "  §fEngine: " + (engineLoaded ? "§aLoaded" : "§cNot loaded")
        ), false);
        source.sendFeedback(() -> Text.literal(
                "  §fManaged packs: §b" + packCount
        ), false);
        source.sendFeedback(() -> Text.literal(
                "  §fMod version: §b6.0.0"
        ), false);
        source.sendFeedback(() -> Text.literal(
                "  §fDatapack version: §b5.1.2 §7(via build.sh)"
        ), false);
        source.sendFeedback(() -> Text.literal(
                "  §7Tip: Run §f/function datalib:version §7for full datapack info"
        ), false);
        return 1;
    }

    /**
     * Runs datalib:debug/tools/utils/check_all via the server command dispatcher.
     * Validates load, perm, and input states.
     */
    private static int executeCheck(CommandContext<ServerCommandSource> context) {
        ServerCommandSource source = context.getSource();

        DataLibCore.getInstance().getEngine().runCheckAll();

        source.sendFeedback(() -> Text.literal(
                "§a[DataLib] §fcheck_all executed. §7See chat for results."
        ), true);
        return 1;
    }
}
