package com.example.kadma.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The {@code /kadma} command tree.
 *
 * <p>Every state changing subcommand requires the configured admin level (2 by
 * default). Player safe subcommands are gated on being an actual player only.</p>
 */
public final class KadmaCommand {

    private KadmaCommand() {
    }

    /** @return the root node, to be registered by the caller. */
    public static LiteralArgumentBuilder<CommandSourceStack> build(com.example.kadma.KadmaServices services) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("kadma")
                // The admin branch carries its own permission requirement (level 2 by
                // default), so no player-facing subcommand can inherit it.
                .then(KadmaAdminCommands.build(services))
                .then(KadmaDialogueCommands.build(services));

        // Player safe branches, exposed both grouped (/kadma player ...) and flat.
        root.then(KadmaPlayerCommands.build(services));
        for (LiteralArgumentBuilder<CommandSourceStack> node : KadmaPlayerCommands.buildAll(services)) {
            root.then(node);
        }
        root.then(Commands.literal("lobby"));
        root.then(Commands.literal("city"));
        root.then(Commands.literal("threshold"));
        root.then(Commands.literal("cycle"));

        return root.executes(KadmaCommand::usage);
    }

    static int usage(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        boolean admin = KadmaCommandContext.admin(KadmaCommandContext.services(source), source);
        source.sendSuccess(() -> com.example.kadma.util.TextUtil.info(
                "\u041a\u0430\u0434\u043c\u0430 \u2014 \u0441\u0435\u0440\u0432\u0435\u0440\u043d\u044b\u0439 \u0434\u0432\u0438\u0436\u043e\u043a \u0438\u0441\u0442\u043e\u0440\u0438\u0438."), false);
        source.sendSuccess(() -> com.example.kadma.util.TextUtil.info(
                "/kadma status \u00b7 /kadma journal [\u0441\u0442\u0440] \u00b7 /kadma hud on|off"), false);
        source.sendSuccess(() -> com.example.kadma.util.TextUtil.info(
                "/kadma lobby \u00b7 /kadma enter city \u00b7 /kadma tp <\u0441\u043b\u043e\u0439> \u00b7 /kadma zone goto <\u0437\u043e\u043d\u0430>"), false);
        if (admin) {
            source.sendSuccess(() -> com.example.kadma.util.TextUtil.info(
                    "\u0410\u0434\u043c\u0438\u043d: reload \u00b7 flag \u00b7 quest \u00b7 chapter \u00b7 prozrenie \u00b7 npc \u00b7 marker \u00b7 event \u00b7 rumor \u00b7 debug \u00b7 reset \u00b7 cycle"), false);
            source.sendSuccess(() -> com.example.kadma.util.TextUtil.info(
                    "\u0414\u0438\u0430\u043b\u043e\u0433: /kadma dialogue choose <id> <\u0443\u0437\u0435\u043b> <\u0432\u044b\u0431\u043e\u0440>"), false);
        }
        return 1;
    }
}
