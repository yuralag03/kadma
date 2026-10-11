package com.example.kadma.command;

import com.example.kadma.KadmaServices;
import com.example.kadma.KadmaMod;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Admin subcommands.
 *
 * <p>Everything here changes authoritative state, so the whole {@code admin} branch
 * sits behind the configured permission level (2 by default) and refuses to run from
 * a console source where the target is missing.</p>
 */
final class KadmaAdminCommands {

    private KadmaAdminCommands() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> build(KadmaServices services) {
        // The whole admin branch is gated here (level 2 by default, configurable),
        // so no player-facing subcommand can inherit the requirement.
        return Commands.literal("admin")
                .requires(source -> KadmaCommandContext.admin(services, source))
                .then(Commands.literal("reload").executes(context -> reload(services, context)))
                .then(flagNodes(services))
                .then(questNodes(services))
                .then(chapterNodes(services))
                .then(prozrenieNodes(services))
                .then(npcNodes(services))
                .then(markerNodes(services))
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
                .then(eventNodes(services))
                .then(rumorNodes(services))
                .then(Commands.literal("debug")
                        .then(Commands.literal("on").executes(context -> debug(services, context, true)))
                        .then(Commands.literal("off").executes(context -> debug(services, context, false)))
                        .then(Commands.literal("selftest").executes(context -> KadmaSelfTest.run(services, context))))
                .then(Commands.literal("reset")
                        .then(Commands.literal("daily")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> resetDaily(services, context)))))
                .then(Commands.literal("cycle")
                        .then(Commands.literal("complete")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> cycleComplete(services, context))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(context -> cycleReset(services, context)))))
                .then(Commands.literal("save").executes(context -> save(services, context)));
    }

    // ------------------------------------------------------------------ leaves

    private static int reload(KadmaServices services, CommandContext<CommandSourceStack> context) {
        int reloads = services.config().reload();
        services.layers().rebuild();
        services.zones().rebuild();
        services.npcs().rebuild();
        services.quests().rebuild();
        services.visual().rebuild();
        List<String> problems = services.config().problems();
        CommandSourceStack source = context.getSource();
        source.sendSuccess(() -> TextUtil.ok("\u041a\u043e\u043d\u0444\u0438\u0433\u0438 \u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0436\u0435\u043d\u044b. "
                + "Контент: " + services.config().countContent() + " (\u043f\u0435\u0440\u0435\u0437\u0430\u0433\u0440\u0443\u0437\u043a\u0430 "
                + reloads + ")"), false);
        if (!problems.isEmpty()) {
            source.sendSuccess(() -> TextUtil.warning("\u041f\u0440\u043e\u0431\u043b\u0435\u043c\u044b: " + problems.size()), false);
            for (String problem : problems.subList(0, Math.min(10, problems.size()))) {
                source.sendSuccess(() -> TextUtil.warning(" - " + problem), false);
            }
        }
        if (source.getPlayer() != null) {
            services.hud().send(source.getPlayer(), services.data().player(source.getPlayer().getUUID()));
        }
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> flagNodes(KadmaServices services) {
        return Commands.literal("flag")
                .then(Commands.literal("player")
                        .then(Commands.literal("set")
                                .then(Commands.argument("key", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .then(Commands.argument("value", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                .executes(context -> flagPlayerSet(services, context)))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("key", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .executes(context -> flagPlayerRemove(services, context)))))
                .then(Commands.literal("global")
                        .then(Commands.literal("set")
                                .then(Commands.argument("key", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .then(Commands.argument("value", com.mojang.brigadier.arguments.StringArgumentType.greedyString())
                                                .executes(context -> flagGlobalSet(services, context)))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("key", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .executes(context -> flagGlobalRemove(services, context)))));
    }

    private static int flagPlayerSet(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "key");
        String value = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "value");
        services.data().player(player.getUUID()).flags.put(key, value);
        services.data().markPlayer(player.getUUID());
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0424\u043b\u0430\u0433 \u0438\u0433\u0440\u043e\u043a\u0430 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d: " + key + "=" + value), false);
        return 1;
    }

    private static int flagPlayerRemove(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String key = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "key");
        services.data().player(player.getUUID()).flags.remove(key);
        services.data().markPlayer(player.getUUID());
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0424\u043b\u0430\u0433 \u0438\u0433\u0440\u043e\u043a\u0430 \u0443\u0434\u0430\u043b\u0451\u043d: " + key), false);
        return 1;
    }

    private static int flagGlobalSet(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String key = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "key");
        String value = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "value");
        GlobalData global = services.data().global();
        global.flags.put(key, value);
        services.data().touch();
        services.story().chapters().recomputePoints(global);
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0413\u043b\u043e\u0431\u0430\u043b\u044c\u043d\u044b\u0439 \u0444\u043b\u0430\u0433 \u0443\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d: " + key + "=" + value), false);
        return 1;
    }

    private static int flagGlobalRemove(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String key = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "key");
        services.data().global().flags.remove(key);
        services.data().touch();
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0413\u043b\u043e\u0431\u0430\u043b\u044c\u043d\u044b\u0439 \u0444\u043b\u0430\u0433 \u0443\u0434\u0430\u043b\u0451\u043d: " + key), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> questNodes(KadmaServices services) {
        return Commands.literal("quest")
                .then(Commands.literal("start")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("questId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                services.config().quests().keySet(), builder))
                                        .executes(context -> questStart(services, context)))))
                .then(Commands.literal("complete")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("questId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                services.config().quests().keySet(), builder))
                                        .executes(context -> questComplete(services, context)))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("questId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                services.config().quests().keySet(), builder))
                                        .executes(context -> questReset(services, context)))));
    }

    private static int questStart(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String questId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "questId");
        boolean started = services.quests().startQuest(player, services.data().player(player.getUUID()), questId, false);
        return started ? 1 : 0;
    }

    private static int questComplete(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String questId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "questId");
        boolean done = services.quests().completeQuest(player, services.data().player(player.getUUID()), questId, false);
        if (!done) {
            context.getSource().sendFailure(TextUtil.error(
                    "\u0417\u0430\u043f\u0438\u0441\u044c \u043d\u0435 \u043c\u043e\u0436\u0435\u0442 \u0431\u044b\u0442\u044c \u0437\u0430\u043a\u0440\u044b\u0442\u0430."));
        }
        return done ? 1 : 0;
    }

    private static int questReset(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String questId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "questId");
        services.quests().resetProgress(player, services.data().player(player.getUUID()), questId);
        context.getSource().sendSuccess(() -> TextUtil.ok("\u041f\u0440\u043e\u0433\u0440\u0435\u0441\u0441 \u0441\u0431\u0440\u043e\u0448\u0435\u043d: " + questId), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> chapterNodes(KadmaServices services) {
        return Commands.literal("chapter")
                .then(Commands.literal("set")
                        .then(Commands.literal("player")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("chapterId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                        services.config().chapters().keySet(), builder))
                                                .executes(context -> chapterSetPlayer(services, context)))))
                        .then(Commands.literal("global")
                                .then(Commands.argument("chapterId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                                services.config().chapters().keySet(), builder))
                                        .executes(context -> chapterSetGlobal(services, context)))));
    }

    private static int chapterSetPlayer(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        String chapterId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "chapterId");
        boolean moved = services.story().setChapter(player, services.data().player(player.getUUID()), chapterId);
        if (!moved) {
            context.getSource().sendFailure(TextUtil.error("\u0413\u043b\u0430\u0432\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430: " + chapterId));
            return 0;
        }
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0413\u043b\u0430\u0432\u0430 \u0438\u0433\u0440\u043e\u043a\u0430: " + chapterId), false);
        return 1;
    }

    private static int chapterSetGlobal(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String chapterId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "chapterId");
        com.example.kadma.config.ChapterConfig chapter = services.config().chapter(chapterId);
        if (chapter == null) {
            context.getSource().sendFailure(TextUtil.error("\u0413\u043b\u0430\u0432\u0430 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u0430: " + chapterId));
            return 0;
        }
        GlobalData global = services.data().global();
        services.story().chapters().unlock(global, chapter, true);
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0413\u043b\u043e\u0431\u0430\u043b\u044c\u043d\u0430\u044f \u0433\u043b\u0430\u0432\u0430: " + chapterId), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> prozrenieNodes(KadmaServices services) {
        return Commands.literal("prozrenie")
                .then(Commands.literal("add")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100))
                                        .executes(context -> prozrenie(services, context, 0)))))
                .then(Commands.literal("set")
                        .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 100))
                                        .executes(context -> prozrenie(services, context, 1)))));
    }

    private static int prozrenie(KadmaServices services, CommandContext<CommandSourceStack> context, int mode)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        int amount = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "amount");
        PlayerData data = services.data().player(player.getUUID());
        if (mode == 1) {
            services.story().prozrenie().set(player, data, amount);
        } else {
            int gained = services.story().prozrenie().grant(player, data, amount);
            amount = gained;
        }
        int value = services.story().prozrenieOf(data);
        services.hud().send(player, data);
        final int shown = amount;
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u041f\u0440\u043e\u0437\u0440\u0435\u043d\u0438\u0435: " + shown + " -> " + value + "/"
                        + services.story().prozrenieMax(data)), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> npcNodes(KadmaServices services) {
        return Commands.literal("npc")
                .then(Commands.literal("spawn")
                        .then(Commands.argument("npcId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        services.config().npcs().keySet(), builder))
                                .executes(context -> npcSpawn(services, context))))
                .then(Commands.literal("spawnall").executes(context -> npcSpawnAll(services, context)))
                .then(Commands.literal("clear").executes(context -> npcClear(services, context)));
    }

    private static int npcSpawn(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String npcId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "npcId");
        if (services.config().npc(npcId) == null) {
            context.getSource().sendFailure(TextUtil.error("NPC \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + npcId));
            return 0;
        }
        ServerLevel level = context.getSource().getLevel();
        if (!services.npcs().spawn(npcId, level, context.getSource().getPlayer())) {
            context.getSource().sendFailure(TextUtil.error(
                    "\u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u0441\u043e\u0437\u0434\u0430\u0442\u044c \u0442\u0435\u043b\u043e: " + npcId));
            return 0;
        }
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0422\u0435\u043b\u043e NPC \u0433\u043e\u0442\u043e\u0432\u043e: " + npcId), false);
        return 1;
    }

    private static int npcSpawnAll(KadmaServices services, CommandContext<CommandSourceStack> context) {
        int spawned = services.npcs().spawnAll(context.getSource().getLevel(), context.getSource().getPlayer());
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0422\u0435\u043b \u0441\u043e\u0437\u0434\u0430\u043d\u043e: " + spawned), false);
        return spawned > 0 ? 1 : 0;
    }

    private static int npcClear(KadmaServices services, CommandContext<CommandSourceStack> context) {
        int removed = services.npcs().clear(context.getSource().getLevel());
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0422\u0435\u043b \u0443\u0431\u0440\u0430\u043d\u043e: " + removed), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> markerNodes(KadmaServices services) {
        return Commands.literal("marker")
                .then(Commands.literal("clear").executes(context -> markerClear(services, context)));
    }

    private static int markerClear(KadmaServices services, CommandContext<CommandSourceStack> context) {
        int removed = services.visual().markers().clearWorld();
        context.getSource().sendSuccess(() -> TextUtil.ok("\u041c\u0430\u0440\u043a\u0435\u0440\u043e\u0432 \u0443\u0431\u0440\u0430\u043d\u043e: " + removed), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> eventNodes(KadmaServices services) {
        return Commands.literal("event")
                .then(Commands.literal("start")
                        .then(Commands.argument("eventId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        services.config().events().keySet(), builder))
                                .executes(context -> eventStart(services, context))))
                .then(Commands.literal("stop").executes(context -> eventStop(services, context)));
    }

    private static int eventStart(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String eventId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "eventId");
        if (services.config().event(eventId) == null) {
            context.getSource().sendFailure(TextUtil.error("\u0421\u043e\u0431\u044b\u0442\u0438\u0435 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d\u043e: " + eventId));
            return 0;
        }
        if (!services.events().start(eventId)) {
            context.getSource().sendFailure(TextUtil.error("\u0421\u043e\u0431\u044b\u0442\u0438\u0435 \u0443\u0436\u0435 \u0438\u0434\u0451\u0442: " + eventId));
            return 0;
        }
        context.getSource().sendSuccess(() -> TextUtil.ok("\u0421\u043e\u0431\u044b\u0442\u0438\u0435 \u0437\u0430\u043f\u0443\u0449\u0435\u043d\u043e: " + eventId), false);
        return 1;
    }

    private static int eventStop(KadmaServices services, CommandContext<CommandSourceStack> context) {
        boolean stopped = services.events().stop();
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0421\u043e\u0431\u044b\u0442\u0438\u044f " + (stopped ? "\u043e\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u044b." : "\u043d\u0435 \u0438\u0434\u0443\u0442.")), false);
        return 1;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> rumorNodes(KadmaServices services) {
        return Commands.literal("rumor")
                .then(Commands.literal("add")
                        .then(Commands.argument("rumorId", com.mojang.brigadier.arguments.StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(
                                        services.config().rumors().keySet(), builder))
                                .executes(context -> rumorAdd(services, context))));
    }

    private static int rumorAdd(KadmaServices services, CommandContext<CommandSourceStack> context) {
        String rumorId = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "rumorId");
        if (services.config().rumor(rumorId) == null) {
            context.getSource().sendFailure(TextUtil.error("\u0421\u043b\u0443\u0445 \u043d\u0435 \u043d\u0430\u0439\u0434\u0435\u043d: " + rumorId));
            return 0;
        }
        ServerPlayer player = context.getSource().getPlayer();
        int sent;
        if (player != null) {
            sent = services.rumors().deliver(services.data().global(), player, rumorId) ? 1 : 0;
        } else {
            // no players online is not a failure: the rumor is registered as fired
            // and will reach everyone who is online at the next refresh
            sent = services.rumors().broadcast("", rumorId);
        }
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0421\u043b\u0443\u0445 \u0434\u043e\u0441\u0442\u0430\u0432\u043b\u0435\u043d: " + rumorId
                        + (sent > 0 ? "" : " (\u043e\u043d\u043b\u0430\u0439\u043d-\u043f\u043e\u043b\u0443\u0447\u0430\u0442\u0435\u043b\u0435\u0439 \u043d\u0435\u0442)")), false);
        return 1;
    }

    private static int debug(KadmaServices services, CommandContext<CommandSourceStack> context, boolean value) {
        services.config().settings().verboseLogging = value;
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u041e\u0442\u043b\u0430\u0434\u043a\u0430 " + (value ? "\u0432\u043a\u043b\u044e\u0447\u0435\u043d\u0430." : "\u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d\u0430.")), false);
        return 1;
    }

    private static int resetDaily(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        PlayerData data = services.data().player(player.getUUID());
        services.daily().resetPlayer(player, data);
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0414\u043d\u0435\u0432\u043d\u044b\u0435 \u0441\u0447\u0451\u0442\u0447\u0438\u043a\u0438 \u0441\u0431\u0440\u043e\u0448\u0435\u043d\u044b."), false);
        return 1;
    }

    private static int cycleComplete(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        boolean done = services.lobby().completeCycle(player);
        return done ? 1 : 0;
    }

    private static int cycleReset(KadmaServices services, CommandContext<CommandSourceStack> context)
            throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = EntityArgument.getPlayer(context, "player");
        boolean done = services.lobby().resetCycle(player);
        return done ? 1 : 0;
    }

    private static int save(KadmaServices services, CommandContext<CommandSourceStack> context) {
        int saved = services.data().saveAll();
        context.getSource().sendSuccess(() -> TextUtil.ok(
                "\u0421\u043e\u0445\u0440\u0430\u043d\u0435\u043d\u043e \u0437\u0430\u043f\u0438\u0441\u0435\u0439: " + saved), false);
        return 1;
    }

    private static int teleportLayer(KadmaServices services, CommandContext<CommandSourceStack> context) {
        return KadmaPlayerCommands.teleportLayer(services, context);
    }

    private static int teleportZone(KadmaServices services, CommandContext<CommandSourceStack> context) {
        return KadmaPlayerCommands.teleportZone(services, context);
    }
}
