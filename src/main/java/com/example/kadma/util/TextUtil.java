package com.example.kadma.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Tiny chat helper. Narrative text lives in JSON, this class only decides colour
 * and layout of the wrapper so the mod keeps a consistent voice in chat.
 */
public final class TextUtil {

    public static final String PREFIX_RAW = "\u00a77[\u00a7d\u041a\u0430\u0434\u043c\u0430\u00a77]\u00a7r ";

    private TextUtil() {
    }

    public static MutableComponent prefix() {
        return Component.literal(PREFIX_RAW);
    }

    /** Materialise a legacy-ampersand coloured string into a real component. */
    public static MutableComponent coloured(String raw) {
        if (raw == null || raw.isEmpty()) {
            return Component.empty();
        }
        StringBuilder plain = new StringBuilder();
        MutableComponent result = Component.empty();
        ChatFormatting current = ChatFormatting.GRAY;

        for (int index = 0; index < raw.length(); index++) {
            char character = raw.charAt(index);
            if ((character == '\u00a7' || character == '&') && index + 1 < raw.length()) {
                char code = Character.toLowerCase(raw.charAt(index + 1));
                ChatFormatting formatting = ChatFormatting.getByCode(code);
                if (formatting != null) {
                    if (plain.length() > 0) {
                        result.append(Component.literal(plain.toString()).withStyle(current));
                        plain.setLength(0);
                    }
                    current = formatting;
                    index++;
                    continue;
                }
            }
            plain.append(character);
        }
        if (plain.length() > 0) {
            result.append(Component.literal(plain.toString()).withStyle(current));
        }
        return result;
    }

    public static MutableComponent info(String text) {
        return prefix().append(Component.literal(text == null ? "" : text).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent warning(String text) {
        return prefix().append(Component.literal(text == null ? "" : text).withStyle(ChatFormatting.YELLOW));
    }

    public static MutableComponent error(String text) {
        return prefix().append(Component.literal(text == null ? "" : text).withStyle(ChatFormatting.RED));
    }

    public static MutableComponent ok(String text) {
        return prefix().append(Component.literal(text == null ? "" : text).withStyle(ChatFormatting.GREEN));
    }

    public static MutableComponent speaker(String speaker, String line) {
        return Component.empty()
                .append(Component.literal("[" + (speaker == null ? "?" : speaker) + "] ").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(line == null ? "" : line).withStyle(ChatFormatting.GRAY));
    }

    public static MutableComponent choice(String label, String command) {
        return Component.empty()
                .append(Component.literal("  \u25b8 ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("[" + label + "]").withStyle(ChatFormatting.YELLOW)
                        .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, command))
                                .withInsertion(command)));
    }

    public static MutableComponent hint(String command, String label) {
        return Component.empty()
                .append(Component.literal("  \u25b8 ").withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal("[" + (label == null ? command : label) + "]").withStyle(ChatFormatting.DARK_GRAY)
                        .withStyle(style -> style
                                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                                        net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, command))));
    }

    public static String ticks(long ticks) {
        long totalSeconds = Math.max(0L, ticks) / 20L;
        return humanise(totalSeconds);
    }

    public static String humanise(long seconds) {
        return TimeUtil.humaniseSeconds(seconds);
    }
}
