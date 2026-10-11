package com.example.kadma.command;

import com.example.kadma.KadmaServices;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Player safe subcommands.
 *
 * <p>Nothing here can change flags, quests or rewards: the server owns all of that.
 * These nodes only read server state and request safe navigation. The same leaf nodes
 * are exposed both under {@code /kadma player ...} and at the top level, so
 * {@code /kadma tp lower} and {@code /kadma journal} both work.</p>
 */
final class KadmaPlayerCommands {

    /** Long-form marker used by the admin branch to delegate navigation. */


    private KadmaPlayerCommands() {
    }

    /** The {@code player} grouping literal. */
    static LiteralArgumentBuilder<CommandSourceStack> build(KadmaServices services) {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("player");
        for (LiteralArgumentBuilder<CommandSourceStack> child : buildAll(services)) {
            node = node.then(child);
        }
        return node;
    }

    /** Top level aliases: /kadma status, /kadma journal, /kadma hud, /kadma tp, ... */
    static List<LiteralArgumentBuilder<CommandSourceStack>> buildAll(KadmaServices services) {
        return Arrays.asList(
                statusNode(services),
                journalNode(services),
                hudNode(services),
                navigationNode(services),
                loreNode(services));
    }

    // -------------------------------------------------------------- node builders

    private static LiteralArgumentBuilder<CommandSourceStack> statusNode(KadmaServices services) {
        return Commands.literal("status")
                .executes(context -> status(services, context))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> statusOther(services, context)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> journalNode(KadmaServices services) {
        return Commands.literal("journal")
                .executes(context -> journal(services, context, 0))
                .then(Commands.argument("page", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
                        .executes(context -> journal(services, context,
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "page"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> hudNode(KadmaServices services) {
        return Commands.literal("hud")
                .then(Commands.literal("on").executes(context -> hud(services, context, true)))
                .then(Commands.literal("off").executes(context -> hud(services, context, false)))
                .then(Commands.literal("toggle").executes(context -> hudToggle(services, context)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> navigationNode(KadmaServices services) {
        return Commands.literal("nav")
                .then(Commands.literal("lobby")
                        .executes(context -> lobby(services, context)))
                .then(Commands.literal("city")
                        .executes(context -> enterCity(services, context)))
                .then(Commands.literal("threshold")
                        .executes(context -> lobby(services, context)))
                .then(Commands.literal("tp")
                        .then(Commands.argument("layer", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        services.layers().ids(), builder))
                                .executes(context -> teleportLayer(services, context))))
                .then(Commands.literal("zone")
                        .then(Commands.literal("goto")
                                .then(Commands.argument("zone", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                services.zones().all().keySet(), builder))
                                        .executes(context -> teleportZone(services, context)))))
                .then(Commands.literal("cycle")
                        .then(Commands.literal("complete")
                                .executes(context -> completeCycle(services, context))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> loreNode(KadmaServices services) {
        return Commands.literal("world")
                .executes(context -> worldJournal(services, context, 0));
    }


    /** Exposes the admin navigation aliases without duplicating the implementation. */
    static int teleportLayer(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String layer = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "layer");
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        if (!services.layers().teleportTo(player, layer)) {
            context.getSource().sendFailure(TextUtil.error("\u0421\u043b\u043e\u0439 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + layer));
            return 0;
        }
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0422\u044b \u043d\u0430 \u0441\u043b\u043e\u0435: " + services.layers().byId(layer).config.displayName), false);
        return 1;
    }

    /** Exposes the admin zone alias without duplicating the implementation. */
    static int teleportZone(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String zoneId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "zone");
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        com.example.kadma.zone.ZoneRegion zone = services.zones().byId(zoneId);
        if (zone == null) {
            context.getSource().sendFailure(TextUtil.error("\u0417\u043e\u043d\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430: " + zoneId));
            return 0;
        }
        net.minecraft.world.phys.Vec3 center = zone.center();
        player.teleportTo(player.serverLevel(), center.x, zone.box.minY + 1.0D, center.z,
                player.getYRot(), player.getXRot());
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0417\u043e\u043d\u0430: " + zone.displayName()), false);
        return 1;
    }

    // ------------------------------------------------------------------ leaves

    private static int status(KadmaServices services, CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        context.getSource().sendSuccess(() -> report(services, player), false);
        return 1;
    }

    private static int statusOther(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        context.getSource().sendSuccess(() -> report(services, player), false);
        return 1;
    }

    /** Full status line for the HUD and for admins. */
    static net.minecraft.network.chat.Component report(KadmaServices services, ServerPlayer player) {
        PlayerData data = services.data().player(player.getUUID());
        com.example.kadma.config.ChapterConfig chapter = services.config().chapter(data.chapter);
        String layer = services.layers().currentLayerDisplay(player);
        String zone = services.zones().currentZoneId(player);

        StringBuilder out = new StringBuilder();
        out.append("\u0421\u043b\u043e\u0439: ").append(layer).append('\n');
        out.append("\u0417\u043e\u043d\u0430: ").append(zone == null ? "\u043d\u0435\u0442" : zone).append('\n');
        out.append("\u0413\u043b\u0430\u0432\u0430: ").append(chapter == null ? data.chapter : chapter.title).append('\n');
        out.append("\u0421\u0442\u0430\u0434\u0438\u044f: ").append(data.stage).append('\n');
        out.append("\u041f\u0440\u043e\u0437\u0440\u0435\u043d\u0438\u0435: ").append(services.story().prozrenieOf(data))
                .append('/').append(services.story().prozrenieMax(data)).append('\n');
        out.append("\u0410\u043a\u0442\u0438\u0432\u043d\u044b\u0435 \u0437\u0430\u043f\u0438\u0441\u0438: ").append(data.activeQuests.size()).append('\n');
        out.append("\u0417\u0430\u043a\u0440\u044b\u0442\u044b\u0435 \u0437\u0430\u043f\u0438\u0441\u0438: ").append(data.completedQuests.size()).append('\n');
        out.append("\u0417\u0430\u043f\u0438\u0441\u0435\u0439 \u0432 \u0434\u043d\u0435\u0432\u043d\u0438\u043a\u0435: ").append(data.journal.size()).append('\n');
        out.append("\u041d\u043e\u0432\u044b\u0435 \u0441\u043b\u0443\u0445\u0438: ").append(data.unreadRumorCount()).append('\n');
        out.append("\u0424\u043b\u0430\u0433\u0438: ").append(data.flags.size()).append('\n');
        out.append("\u0426\u0438\u043a\u043b\u044b: ").append(data.completedCycles);
        return TextUtil.info(out.toString());
    }

    private static int journal(KadmaServices services, CommandContext<CommandSourceStack> context, int page) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        services.journal().show(player, services.data().player(player.getUUID()), Math.max(0, page));
        return 1;
    }

    private static int worldJournal(KadmaServices services, CommandContext<CommandSourceStack> context, int page) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        services.journal().showWorld(player, Math.max(0, page));
        return 1;
    }

    private static int hud(KadmaServices services, CommandContext<CommandSourceStack> context, boolean value) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        java.util.UUID id = player.getUUID();
        services.hud().setEnabled(id, value);
        services.hud().send(player, services.data().player(id));
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0418\u043d\u0442\u0435\u0440\u0444\u0435\u0439\u0441 \u0432\u0435\u0440\u0445\u043d\u0435\u0433\u043e \u0443\u0440\u043e\u0432\u043d\u044f "
                + (value ? "\u0432\u043a\u043b\u044e\u0447\u0451\u043d." : "\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d.")), false);
        return 1;
    }

    private static int hudToggle(KadmaServices services, CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        PlayerData data = services.data().player(player.getUUID());
        data.hudEnabled = !data.hudEnabled;
        return hud(services, context, data.hudEnabled);
    }

    private static int lobby(KadmaServices services, CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        if (!services.lobby().toLobby(player)) {
            context.getSource().sendFailure(TextUtil.error("\u041f\u043e\u0440\u043e\u0433 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d."));
            return 0;
        }
        return 1;
    }

    private static int enterCity(KadmaServices services, CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        if (!services.lobby().enterCity(player)) {
            context.getSource().sendFailure(TextUtil.error("\u0413\u043e\u0440\u043e\u0434 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d."));
            return 0;
        }
        return 1;
    }

    private static int completeCycle(KadmaServices services, CommandContext<CommandSourceStack> context) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            context.getSource().sendFailure(TextUtil.error("\u041a\u043e\u043c\u0430\u043d\u0434\u0430 \u0442\u0440\u0435\u0431\u0443\u0435\u0442 \u0438\u0433\u0440\u043e\u043a\u0430."));
            return 0;
        }
        return services.lobby().completeCycle(player) ? 1 : 0;
    }
}
