package com.example.kadma.command;

import com.example.kadma.KadmaServices;
import com.example.kadma.util.TextUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * Dialogue commands.
 *
 * <p>These are the only player reachable commands that can nudge the story. They are
 * permission-free but every handler re-validates ownership, cooldown, node existence
 * and effect legality on the server.</p>
 */
final class KadmaDialogueCommands {

    private KadmaDialogueCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build(KadmaServices services) {
        return Commands.literal("dialogue")
                .then(Commands.literal("choose")
                        .then(Commands.argument("dialogueId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        services.config().dialogues().keySet(), builder))
                                .then(Commands.argument("nodeId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String dialogueId = safeRead(context, "dialogueId");
                                            return SharedSuggestionProvider.suggest(services.dialogues()
                                                    .byId(dialogueId) == null
                                                    ? java.util.List.<String>of()
                                                    : services.dialogues().byId(dialogueId).nodes.keySet(), builder);
                                        })
                                        .then(Commands.argument("choiceIndex", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 32))
                                                .executes(context -> choose(services, context))))))
                .then(Commands.literal("open")
                        .then(Commands.argument("dialogueId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        services.config().dialogues().keySet(), builder))
                                .executes(context -> open(services, context))));
    }

    private static String safeRead(CommandContext<CommandSourceStack> context, String name) {
        try {
            return com.mojang.brigadier.arguments.StringArgumentType.getString(context, name);
        } catch (IllegalArgumentException ex) {
            return "";
        }
    }

    private static int choose(KadmaServices services, CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        String dialogueId = safeRead(context, "dialogueId");
        String nodeId = safeRead(context, "nodeId");
        int choiceIndex = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "choiceIndex");
        boolean applied = services.dialogues().choose(context.getSource().getPlayer(), dialogueId, nodeId, choiceIndex);
        return applied ? 1 : 0;
    }

    private static int open(KadmaServices services, CommandContext<CommandSourceStack> context) {
        if (context.getSource().getPlayer() == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        String dialogueId = safeRead(context, "dialogueId");
        boolean opened = services.dialogues().open(context.getSource().getPlayer(), dialogueId, null);
        return opened ? 1 : 0;
    }
}
