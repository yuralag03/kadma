package com.example.kadma.hud;

import com.example.kadma.KadmaClientState;
import com.example.kadma.KadmaMod;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Client HUD renderer.
 *
 * <p>Reads the latest server payload from {@link KadmaClientState} and paints a small
 * corner block. Nothing here influences gameplay and nothing is ever sent back.</p>
 */
@Mod.EventBusSubscriber(modid = KadmaMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class HudClientRenderer {

    private HudClientRenderer() {
    }

    private static int corner() {
        String raw = System.getProperty("kadma.hud.corner", "top-left");
        if ("top-right".equalsIgnoreCase(raw)) {
            return 1;
        }
        if ("bottom-left".equalsIgnoreCase(raw)) {
            return 2;
        }
        if ("bottom-right".equalsIgnoreCase(raw)) {
            return 3;
        }
        return 0;
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }
        HudPayload payload = KadmaClientState.payload();
        if (payload == null || !payload.visible) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        Font font = minecraft.font;
        int lineHeight = font.lineHeight + 2;

        java.util.List<Component> lines = buildLines(payload);
        int widest = 0;
        for (Component line : lines) {
            widest = Math.max(widest, font.width(line));
        }
        Window window = minecraft.getWindow();
        int x;
        int y;
        switch (corner()) {
            case 1 -> {
                x = window.getGuiScaledWidth() - widest - 4;
                y = 4;
            }
            case 2 -> {
                x = 4;
                y = window.getGuiScaledHeight() - lines.size() * lineHeight - 4;
            }
            case 3 -> {
                x = window.getGuiScaledWidth() - widest - 4;
                y = window.getGuiScaledHeight() - lines.size() * lineHeight - 4;
            }
            default -> {
                x = 4;
                y = 4;
            }
        }
        int background = 0x66000000;
        graphics.fill(x - 2, y - 2, x + widest + 2, y - 2 + lines.size() * lineHeight + 2, background);
        int current = y;
        for (Component line : lines) {
            graphics.drawString(font, line, x, current, 0xFFFFFF, false);
            current += lineHeight;
        }
    }

    private static java.util.List<Component> buildLines(HudPayload payload) {
        java.util.List<Component> lines = new java.util.ArrayList<>();
        lines.add(Component.literal("\u041a\u0430\u0434\u043c\u0430").withStyle(net.minecraft.ChatFormatting.AQUA));
        lines.add(Component.literal("\u0421\u043b\u043e\u0439: ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                .append(Component.literal(emptyTo(payload.layer, "\u2014")).withStyle(net.minecraft.ChatFormatting.GRAY)));
        lines.add(Component.literal("\u0413\u043b\u0430\u0432\u0430: ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                .append(Component.literal(emptyTo(payload.chapter, "\u2014")).withStyle(net.minecraft.ChatFormatting.GRAY)));
        if (payload.stage > 0) {
            lines.add(Component.literal("\u0421\u0442\u0430\u0434\u0438\u044f: ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                    .append(Component.literal(Integer.toString(payload.stage)).withStyle(net.minecraft.ChatFormatting.GRAY)));
        }
        if (payload.quest != null && !payload.quest.isEmpty()) {
            lines.add(Component.literal("\u0417\u0430\u043f\u0438\u0441\u044c: ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                    .append(Component.literal(payload.quest).withStyle(net.minecraft.ChatFormatting.YELLOW)));
            if (!payload.objective.isEmpty()) {
                lines.add(Component.literal("  " + payload.objective).withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            if (!payload.progress.isEmpty()) {
                lines.add(Component.literal("  " + payload.progress).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
        }
        lines.add(Component.literal("\u041f\u0440\u043e\u0437\u0440\u0435\u043d\u0438\u0435: ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                .append(Component.literal(payload.prozrenie + "/" + payload.maxProzrenie)
                        .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE)));
        lines.add(Component.literal("\u0412\u0435\u0440\u0430 " + payload.faith)
                .withStyle(net.minecraft.ChatFormatting.WHITE));
        lines.add(Component.literal("\u0422\u043b\u0435\u043d\u0438\u0435 " + payload.corruption)
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        lines.add(Component.literal("\u0420\u0430\u0441\u043a\u043e\u043b " + payload.schism)
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        lines.add(Component.literal("\u0418\u0441\u043a\u0440\u044b " + payload.spark)
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        if (payload.event != null && !payload.event.isEmpty()) {
            lines.add(Component.literal("\u0421\u043e\u0431\u044b\u0442\u0438\u0435: ").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)
                    .append(Component.literal(payload.event).withStyle(net.minecraft.ChatFormatting.GOLD)));
        }
        if (payload.rumors > 0) {
            lines.add(Component.literal("\u0421\u043b\u0443\u0445\u0438: " + payload.rumors + " \u043d\u043e\u0432\u044b\u0445")
                    .withStyle(net.minecraft.ChatFormatting.BLUE));
        }
        return lines;
    }

    private static String emptyTo(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    /** Kept for parity with the client mod lifecycle. */
    public static void reset() {
        KadmaClientState.clear();
    }
}
