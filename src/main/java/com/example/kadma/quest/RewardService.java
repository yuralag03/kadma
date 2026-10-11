package com.example.kadma.quest;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.ItemConfig;
import com.example.kadma.config.RewardDefinition;
import com.example.kadma.data.GlobalData;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.InventoryUtil;
import com.example.kadma.util.TextUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Transactional rewards. Items are built first and only inserted after every other
 * part of the bundle is known to be valid, so a partial reward is never applied.
 */
public final class RewardService {

    private RewardService() {
    }

    /** Build a configured story item with name, lore and marker NBT. */
    public static ItemStack createStoryItem(KadmaServices services, String itemId, int amount) {
        ItemConfig config = services.config().item(itemId);
        if (config == null) {
            return null;
        }
        Item base = InventoryUtil.resolveItem(config.baseItem);
        if (base == null) {
            return null;
        }
        ItemStack stack = new ItemStack(base, Math.max(1, amount));
        CompoundTag tag = stack.getOrCreateTag();

        CompoundTag kadma = new CompoundTag();
        kadma.putByte(config.tagKey(), (byte) 1);
        kadma.putString("story_item", itemId);
        tag.put("kadma", kadma);

        if (config.glow) {
            // vanilla-friendly glow without needing a model
            CompoundTag display = new CompoundTag();
            display.putInt("color", -1);
            tag.put("display", display);
        }
        String name = config.displayName == null || config.displayName.isEmpty() ? itemId : config.displayName;
        stack.setHoverName(net.minecraft.network.chat.Component.literal(name)
                .withStyle(net.minecraft.ChatFormatting.AQUA));
        if (config.lore != null && !config.lore.isEmpty()) {
            List<net.minecraft.network.chat.Component> lore = new ArrayList<>();
            for (String line : config.lore) {
                lore.add(net.minecraft.network.chat.Component.literal(line)
                        .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            }
            CompoundTag display = tag.getCompound("display");
            net.minecraft.nbt.ListTag loreTag = new net.minecraft.nbt.ListTag();
            for (net.minecraft.network.chat.Component line : lore) {
                loreTag.add(net.minecraft.nbt.StringTag.valueOf(
                        net.minecraft.network.chat.Component.Serializer.toJson(line)));
            }
            display.put("Lore", loreTag);
            tag.put("display", display);
        }
        if (config.unbreakable) {
            tag.putBoolean("Unbreakable", true);
        }
        if (config.customModelData > 0) {
            tag.putInt("CustomModelData", config.customModelData);
        }
        return stack;
    }

    /**
     * Apply a reward bundle. Returns {@code true} when at least one branch applied.
     */
    public static boolean apply(KadmaServices services, ServerPlayer player, PlayerData data, GlobalData global,
                                RewardDefinition rewards, boolean silent) {
        if (rewards == null || rewards.empty()) {
            return false;
        }
        List<ItemStack> pending = new ArrayList<>();
        boolean ok = true;

        for (RewardDefinition.ItemReward entry : rewards.items) {
            ItemStack stack = createStoryItem(services, entry.itemId, entry.amount);
            if (stack == null) {
                com.example.kadma.KadmaMod.LOG.warn("[kadma] reward item {} is unknown, skipped", entry.itemId);
                continue;
            }
            pending.add(stack);
        }

        if (ok) {
            for (ItemStack stack : pending) {
                InventoryUtil.give(player, stack);
            }
        }

        boolean changed = false;
        for (RewardDefinition.FlagReward flag : rewards.playerFlags) {
            if (flag.key.isEmpty()) {
                continue;
            }
            data.flags.put(flag.key, flag.value == null || flag.value.isEmpty() ? "true" : flag.value);
            changed = true;
        }
        for (RewardDefinition.FlagReward flag : rewards.globalFlags) {
            if (flag.key.isEmpty()) {
                continue;
            }
            global.flags.put(flag.key, flag.value == null || flag.value.isEmpty() ? "true" : flag.value);
            changed = true;
        }
        for (Map.Entry<String, Integer> entry : rewards.stats.entrySet()) {
            global.bumpStat(entry.getKey(), entry.getValue() == null ? 0 : entry.getValue());
            changed = true;
        }
        for (String entry : rewards.journal) {
            data.addJournal(entry);
            changed = true;
        }
        for (String rumor : rewards.rumors) {
            data.rememberRumor(rumor);
            services.rumors().deliver(global, player, rumor);
            changed = true;
        }
        if (rewards.prozrenie != 0) {
            if (rewards.prozrenie > 0) {
                services.story().grantProzrenie(player, data, rewards.prozrenie);
            } else {
                services.story().prozrenie().spend(player, data, -rewards.prozrenie);
            }
            changed = true;
        }
        if (rewards.experience > 0) {
            player.giveExperiencePoints(rewards.experience);
            changed = true;
        }
        for (String command : rewards.commands) {
            try {
                services.server().getCommands().performPrefixedCommand(
                        services.server().createCommandSourceStack()
                                .withSuppressedOutput()
                                .withPermission(4)
                                .withSource(player),
                        command);
            } catch (Exception ex) {
                com.example.kadma.KadmaMod.LOG.warn("[kadma] reward command failed: {}", command, ex);
            }
        }
        if (changed) {
            services.data().markPlayer(player.getUUID());
            services.data().touch();
            services.story().afterWorldChange(global);
        }
        if (!silent && !pending.isEmpty()) {
            player.sendSystemMessage(TextUtil.ok("\u0417\u0430\u043f\u0438\u0441\u044c \u043f\u043e\u043f\u043e\u043b\u043d\u0435\u043d\u0430: "
                    + describePending(services, pending)));
        }
        return changed;
    }

    private static String describePending(KadmaServices services, List<ItemStack> stacks) {
        StringBuilder builder = new StringBuilder();
        for (ItemStack stack : stacks) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(stack.getHoverName().getString()).append(" x").append(stack.getCount());
        }
        return builder.toString();
    }
}
