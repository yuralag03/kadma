package com.example.kadma.command;

import com.example.kadma.KadmaServices;
import net.minecraft.commands.CommandSourceStack;

/** Permission gate and services lookup for the command tree. */
public final class KadmaCommandContext {

    private static volatile KadmaServices active;

    private KadmaCommandContext() {
    }

    public static void bind(KadmaServices services) {
        active = services;
    }

    public static void clear() {
        active = null;
    }

    public static KadmaServices services(CommandSourceStack source) {
        return active;
    }

    static boolean admin(KadmaServices services, CommandSourceStack source) {
        int level = services == null ? 2 : services.config().settings().adminPermissionLevel;
        return source.hasPermission(level);
    }

    static boolean bound() {
        return active != null;
    }
}
