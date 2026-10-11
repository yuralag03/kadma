package com.example.kadma.visual;

import com.example.kadma.KadmaServices;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * TextDisplay factory and text helpers.
 *
 * <p>Vanilla {@code TextDisplay} entities are used as floating labels and quest
 * markers. Text is applied through {@link Entity#load(net.minecraft.nbt.CompoundTag)}
 * rather than private entity data so no reflection and no mixins are required.</p>
 */
public class TextDisplayService {

    private final KadmaServices services;

    public TextDisplayService(KadmaServices services) {
        this.services = services;
    }

    public Display.TextDisplay spawn(Level level, double x, double y, double z, String text) {
        return spawn(level, x, y, z, Component.literal(text == null ? "" : text));
    }

    public Display.TextDisplay spawn(Level level, double x, double y, double z, Component text) {
        if (level == null) {
            return null;
        }
        Display.TextDisplay display = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        display.moveTo(x, y, z, 0.0F, 0.0F);
        setText(display, text);
        if (level.addFreshEntity(display)) {
            return display;
        }
        display.discard();
        return null;
    }

    /** Replace the text of an existing display using the public save/load cycle. */
    public static boolean setText(Display.TextDisplay display, Component text) {
        if (display == null) {
            return false;
        }
        try {
            net.minecraft.nbt.CompoundTag tag = display.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            tag.putString(Display.TextDisplay.TAG_TEXT,
                    Component.Serializer.toJson(text == null ? Component.empty() : text));
            display.load(tag);
            return true;
        } catch (Exception ex) {
            com.example.kadma.KadmaMod.LOG.warn("[kadma] cannot set display text", ex);
            return false;
        }
    }

    public static String textOf(Display.TextDisplay display) {
        if (display == null) {
            return "";
        }
        try {
            net.minecraft.nbt.CompoundTag tag = display.saveWithoutId(new net.minecraft.nbt.CompoundTag());
            return tag.getString(Display.TextDisplay.TAG_TEXT);
        } catch (Exception ex) {
            return "";
        }
    }

    /** Entity ids inside a box, used by cleanup passes. */
    public static List<UUID> inside(Level level, net.minecraft.world.phys.AABB box) {
        List<UUID> out = new ArrayList<>();
        if (level == null || box == null) {
            return out;
        }
        for (Entity entity : level.getEntities((Entity) null, box, entity -> entity instanceof Display)) {
            out.add(entity.getUUID());
        }
        return out;
    }

    public static boolean isDisplay(Entity entity) {
        return entity instanceof Display.TextDisplay;
    }
}
