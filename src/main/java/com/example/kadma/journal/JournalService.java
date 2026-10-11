package com.example.kadma.journal;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.QuestConfig;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Player journal.
 *
 * <p>Stores what the player themselves has seen and heard: the chapter they are in,
 * the flags they hold, rumors they collected and the decisions they took. Printed as
 * paginated chat with clickable next/prev.</p>
 */
public class JournalService {

    private static final int PAGE_SIZE = 9;

    private final KadmaServices services;

    public JournalService(KadmaServices services) {
        this.services = services;
    }

    /** Write an entry into the player's own journal. */
    public void note(PlayerData data, QuestConfig quest) {
        if (data == null || quest == null) {
            return;
        }
        data.addJournal("\u0417\u0430\u043f\u0438\u0441\u044c: " + quest.title);
    }

    public void noteCompletion(PlayerData data, QuestConfig quest) {
        if (data == null || quest == null) {
            return;
        }
        data.addJournal("\u0417\u0430\u043a\u0440\u044b\u0442\u0430 \u0437\u0430\u043f\u0438\u0441\u044c: " + quest.title);
    }

    /** Write an entry into the world journal. */
    public void world(List<String> entries, String chapterTitle) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        com.example.kadma.data.GlobalData global = services.data().global();
        String prefix = chapterTitle == null || chapterTitle.isEmpty()
                ? "" : "\u0413\u043b\u0430\u0432\u0430 \u0441\u043c\u0435\u043d\u0438\u043b\u0430\u0441\u044c: " + chapterTitle + ". ";
        boolean changed = false;
        for (String entry : entries) {
            if (entry == null || entry.isEmpty()) {
                continue;
            }
            global.rememberWorld(prefix + entry);
            changed = true;
        }
        if (changed) {
            services.data().touch();
        }
    }

    public void noteReset(long day) {
        services.data().global().rememberWorld("\u0414\u0435\u043d\u044c " + day + " \u0437\u0430\u0432\u0435\u0440\u0448\u0438\u043b\u0441\u044f.");
        services.data().touch();
    }

    public void noteCompletedCycle(PlayerData data, int cycle) {
        data.rememberHistory("\u0426\u0438\u043a\u043b " + cycle + " \u0437\u0430\u043a\u0440\u044b\u0442.");
    }

    // ------------------------------------------------------------------- output

    /** Print page {@code page} (0 based) of the player's journal. */
    public void show(ServerPlayer player, PlayerData data, int page) {
        List<String> entries = data.journal;
        if (entries.isEmpty()) {
            player.sendSystemMessage(TextUtil.info(
                    "\u0414\u043d\u0435\u0432\u043d\u0438\u043a \u043f\u0443\u0441\u0442. \u041e\u043d \u0437\u0430\u043f\u043e\u043b\u043d\u0438\u0442\u0441\u044f, \u043a\u043e\u0433\u0434\u0430 \u0447\u0442\u043e-\u0442\u043e \u043f\u0440\u043e\u0438\u0437\u043e\u0439\u0434\u0451\u0442."));
            return;
        }
        int pages = (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = Math.max(0, Math.min(pages - 1, page));
        int from = current * PAGE_SIZE;
        int to = Math.min(entries.size(), from + PAGE_SIZE);

        com.example.kadma.config.ChapterConfig chapter = services.config().chapter(data.chapter);
        player.sendSystemMessage(TextUtil.info("\u0414\u043d\u0435\u0432\u043d\u0438\u043a \u2014 "
                + (chapter == null ? data.chapter : chapter.title)
                + " \u00b7 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0430 " + (current + 1) + "/" + pages));
        for (int index = from; index < to; index++) {
            player.sendSystemMessage(Component.literal("  \u25aa ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                    .append(Component.literal(entries.get(index)).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
        java.util.List<Component> buttons = new java.util.ArrayList<>();
        if (current > 0) {
            buttons.add(TextUtil.choice("\u041d\u0430\u0437\u0430\u0434", "/kadma journal " + (current - 1)));
        }
        if (current < pages - 1) {
            buttons.add(TextUtil.choice("\u0414\u0430\u043b\u044c\u0448\u0435", "/kadma journal " + (current + 1)));
        }
        if (!buttons.isEmpty()) {
            net.minecraft.network.chat.MutableComponent row = net.minecraft.network.chat.Component.empty();
            for (Component button : buttons) {
                row.append(button).append(net.minecraft.network.chat.Component.literal(" "));
            }
            player.sendSystemMessage(row);
        }
        data.readRumors();
        services.data().markPlayer(player.getUUID());
    }

    /** Journal of the world, readable by anyone. */
    public void showWorld(ServerPlayer player, int page) {
        List<String> entries = services.data().global().worldJournal;
        if (entries.isEmpty()) {
            player.sendSystemMessage(TextUtil.info("\u0417\u0430\u043f\u0438\u0441\u0435\u0439 \u0433\u043e\u0440\u043e\u0434\u0430 \u043f\u043e\u043a\u0430 \u043d\u0435\u0442."));
            return;
        }
        int pages = (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int current = Math.max(0, Math.min(pages - 1, page));
        int from = current * PAGE_SIZE;
        int to = Math.min(entries.size(), from + PAGE_SIZE);
        player.sendSystemMessage(TextUtil.info("\u0417\u0430\u043f\u0438\u0441\u0438 \u0433\u043e\u0440\u043e\u0434\u0430 \u00b7 \u0441\u0442\u0440\u0430\u043d\u0438\u0446\u0430 " + (current + 1) + "/" + pages));
        for (int index = from; index < to; index++) {
            player.sendSystemMessage(Component.literal("  \u25aa ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                    .append(Component.literal(entries.get(index)).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
    }

    public int entries(PlayerData data) {
        return data.journal.size();
    }
}
