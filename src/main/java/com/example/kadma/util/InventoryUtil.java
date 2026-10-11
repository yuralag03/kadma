package com.example.kadma.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;

/** Server side inventory counting and removal. All mutations happen on the server thread. */
public final class InventoryUtil {

    private InventoryUtil() {
    }

    /** True when the stack carries {@code kadma.<logicalItemId>} = 1. */
    public static boolean isStoryItem(ItemStack stack, String logicalItemId) {
        if (stack == null || stack.isEmpty() || logicalItemId == null) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains("kadma")) {
            return false;
        }
        CompoundTag kadma = tag.getCompound("kadma");
        if (kadma == null || kadma.isEmpty()) {
            return false;
        }
        return kadma.contains(logicalItemId);
    }

    public static int countStoryItem(Player player, String logicalItemId) {
        if (player == null) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int total = countIn(inventory.items, logicalItemId)
                + countIn(inventory.offhand, logicalItemId)
                + countIn(inventory.armor, logicalItemId);
        return total;
    }

    private static int countIn(Iterable<ItemStack> stacks, String logicalItemId) {
        int total = 0;
        for (ItemStack stack : stacks) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (isStoryItem(stack, logicalItemId)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    public static int countItem(Player player, Item item) {
        if (player == null || item == null) {
            return 0;
        }
        return player.getInventory().countItem(item);
    }

    /**
     * Remove up to {@code amount} matching story items. Returns the amount actually
     * removed so rewards stay transactional.
     */
    public static int removeStoryItem(Player player, String logicalItemId, int amount) {
        if (player == null || amount <= 0) {
            return 0;
        }
        Inventory inventory = player.getInventory();
        int remaining = amount;
        remaining = removeFrom(inventory.items, logicalItemId, remaining);
        remaining = removeFrom(inventory.offhand, logicalItemId, remaining);
        remaining = removeFrom(inventory.armor, logicalItemId, remaining);
        return amount - remaining;
    }

    private static int removeFrom(net.minecraft.core.NonNullList<ItemStack> stacks, String logicalItemId, int remaining) {
        for (int slot = 0; slot < stacks.size() && remaining > 0; slot++) {
            ItemStack stack = stacks.get(slot);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            if (!isStoryItem(stack, logicalItemId)) {
                continue;
            }
            int take = Math.min(remaining, stack.getCount());
            stack.shrink(take);
            remaining -= take;
        }
        return remaining;
    }

    /** Greedy stack destruction across the whole inventory. */
    public static void clearStoryItem(Player player, String logicalItemId) {
        removeStoryItem(player, logicalItemId, Integer.MAX_VALUE);
    }

    public static boolean hasItem(Player player, Item item, int amount) {
        return countItem(player, item) >= amount;
    }

    public static void give(Player player, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) {
            return;
        }
        boolean added = player.getInventory().add(stack);
        if (!added || !stack.isEmpty()) {
            player.drop(stack, false);
        }
    }

    @Nullable
    public static Item resolveItem(@Nullable String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String id = raw.contains(":") ? raw : "minecraft:" + raw;
        try {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse(id));
            return item == Items.AIR ? null : item;
        } catch (Exception ex) {
            return null;
        }
    }
}
