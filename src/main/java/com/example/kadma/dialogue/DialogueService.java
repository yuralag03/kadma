package com.example.kadma.dialogue;

import com.example.kadma.KadmaServices;
import com.example.kadma.config.DialogueConfig;
import com.example.kadma.data.PlayerData;
import com.example.kadma.util.TextUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Map;

/**
 * Chat based dialogue with clickable choices.
 *
 * <p>Lines arrive as server chat. Choices are clickable chat components that run a
 * server command. The server validates the requested node, the request time and the
 * player's own state before applying anything, so a forged packet cannot unlock
 * content or take rewards.</p>
 */
public class DialogueService {

    /** Command prefix a dialogue choice executes. */
    public static final String COMMAND = "/kadma dialogue choose ";

    private final KadmaServices services;
    private final long cooldownMillis;

    public DialogueService(KadmaServices services) {
        this.services = services;
        this.cooldownMillis = Math.max(0L, services.config().settings().dialogueCooldownSeconds) * 1000L;
    }

    public DialogueConfig byId(String id) {
        return services.config().dialogue(id);
    }

    public boolean exists(String id) {
        return services.config().dialogues().containsKey(id);
    }

    /**
     * Open a dialogue at a node (or at the configured root when the node is unknown).
     *
     * @return true when something was shown.
     */
    public boolean open(ServerPlayer player, String dialogueId, String nodeId) {
        if (!services.config().settings().features.dialogues) {
            return false;
        }
        DialogueConfig dialogue = byId(dialogueId);
        if (dialogue == null) {
            player.sendSystemMessage(TextUtil.warning(
                    "\u0414\u0438\u0430\u043b\u043e\u0433 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d: " + dialogueId));
            return false;
        }
        PlayerData data = services.data().player(player.getUUID());
        markUnlocked(data, dialogueId);
        DialogueNode node = node(dialogue, nodeId);
        if (node == null) {
            player.sendSystemMessage(TextUtil.warning(
                    "\u042d\u0442\u0430 \u0440\u0435\u043f\u043b\u0438\u043a\u0430 \u0437\u0430\u043a\u0440\u044b\u0442\u0430."));
            return false;
        }
        if (node.hasConditions() && !conditionsMet(player, data, node)) {
            return false;
        }
        send(player, dialogue, node);
        applyNodeEffects(player, data, node);
        return true;
    }

    private DialogueNode node(DialogueConfig dialogue, String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return dialogue.root();
        }
        return dialogue.node(nodeId);
    }

    private void markUnlocked(PlayerData data, String dialogueId) {
        if (data.unlockedDialogues.add(dialogueId)) {
            services.data().markPlayer(java.util.UUID.fromString(data.uuid));
        }
    }

    private boolean conditionsMet(ServerPlayer player, PlayerData data, DialogueNode node) {
        for (DialogueCondition condition : node.conditions) {
            if (!condition.matches(player, services)) {
                return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------- output

    public void send(ServerPlayer player, DialogueConfig dialogue, DialogueNode node) {
        String speaker = node.npc == null || node.npc.isEmpty() ? dialogue.title : node.npc;
        com.example.kadma.config.NpcConfig npc = services.config().npc(node.npc);
        if (npc != null) {
            speaker = npc.displayName;
        }
        player.sendSystemMessage(Component.empty());
        player.sendSystemMessage(Component.literal("\u2500\u2500\u2500").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        for (String line : node.text) {
            player.sendSystemMessage(TextUtil.speaker(speaker, line));
        }
        for (DialogueChoice choice : choices(player, dataOf(player), node)) {
            player.sendSystemMessage(TextUtil.choice(choice.text,
                    COMMAND + dialogue.id + " " + node.id + " " + choice.index));
        }
        player.sendSystemMessage(Component.literal("\u2500\u2500\u2500").withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    private PlayerData dataOf(ServerPlayer player) {
        return services.data().player(player.getUUID());
    }

    /** Only the choices whose conditions the server currently accepts. */
    public List<DialogueChoice> choices(ServerPlayer player, PlayerData data, DialogueNode node) {
        List<DialogueChoice> out = new java.util.ArrayList<>();
        for (DialogueChoice choice : node.choices) {
            boolean ok = true;
            for (DialogueCondition condition : choice.conditions) {
                if (!condition.matches(player, services)) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                out.add(choice);
            }
        }
        return out;
    }

    private void applyNodeEffects(ServerPlayer player, PlayerData data, DialogueNode node) {
        for (DialogueEffect effect : node.effects) {
            effect.apply(player, services, data, services.data().global());
        }
        if (!node.journal.isEmpty()) {
            data.addJournal(node.journal);
        }
        if (!node.rumor.isEmpty() && !data.knownRumor(node.rumor)) {
            data.rememberRumor(node.rumor);
            services.rumors().deliver(services.data().global(), player, node.rumor);
        }
        if (!node.startQuest.isEmpty()) {
            services.quests().startQuest(player, data, node.startQuest, false);
        }
        if (!node.completeQuest.isEmpty()) {
            services.quests().completeQuest(player, data, node.completeQuest, false);
        }
        services.data().markPlayer(player.getUUID());
    }

    // ------------------------------------------------------------------- choose

    /**
     * Apply a dialogue choice.
     *
     * @param choiceIndex the index the player clicked.
     */
    public boolean choose(ServerPlayer player, String dialogueId, String nodeId, int choiceIndex) {
        if (player == null || !services.config().settings().features.dialogues) {
            return false;
        }
        PlayerData data = services.data().player(player.getUUID());
        long now = System.currentTimeMillis();
        Long last = data.cooldowns.get("dialogue");
        if (last != null && now - last < cooldownMillis && cooldownMillis > 0) {
            player.sendSystemMessage(TextUtil.warning(
                    "\u0421\u043b\u0438\u0448\u043a\u043e\u043c \u0431\u044b\u0441\u0442\u0440\u043e. \u041e\u0434\u043d\u043e \u0441\u043b\u043e\u0432\u043e \u0437\u0430 \u0440\u0430\u0437."));
            return false;
        }
        data.cooldowns.put("dialogue", now);
        services.data().markPlayer(player.getUUID());

        DialogueConfig dialogue = byId(dialogueId);
        if (dialogue == null) {
            return false;
        }
        DialogueNode node = dialogue.node(nodeId);
        if (node == null) {
            player.sendSystemMessage(TextUtil.warning(
                    "\u042d\u0442\u0430 \u0440\u0435\u043f\u043b\u0438\u043a\u0430 \u0437\u0430\u043a\u0440\u044b\u0442\u0430."));
            return false;
        }
        if (!data.unlockedDialogues.contains(dialogueId)) {
            player.sendSystemMessage(TextUtil.warning(
                    "\u0420\u0430\u0437\u0433\u043e\u0432\u043e\u0440 \u043d\u0435 \u043d\u0430\u0447\u0430\u0442."));
            return false;
        }
        if (node.hasConditions() && !conditionsMet(player, data, node)) {
            services.visual().deny(player);
            return false;
        }
        DialogueChoice choice = null;
        for (DialogueChoice candidate : choices(player, data, node)) {
            if (candidate.index == choiceIndex) {
                choice = candidate;
                break;
            }
        }
        if (choice == null) {
            player.sendSystemMessage(TextUtil.warning("\u0422\u0430\u043a\u043e\u0433\u043e \u0432\u044b\u0431\u043e\u0440\u0430 \u043d\u0435\u0442."));
            services.visual().deny(player);
            return false;
        }

        for (DialogueEffect effect : choice.effects) {
            effect.apply(player, services, data, services.data().global());
        }
        services.data().markPlayer(player.getUUID());
        services.quests().validator().onChoose(player, data, dialogueId, nodeId, choiceIndex);

        String next = choice.next == null ? "" : choice.next;
        if (!next.isEmpty() && dialogue.node(next) != null) {
            open(player, dialogueId, next);
        } else if (choice.terminal) {
            player.sendSystemMessage(TextUtil.info("\u0420\u0430\u0437\u0433\u043e\u0432\u043e\u0440 \u0437\u0430\u043a\u043e\u043d\u0447\u0435\u043d."));
        } else if (!next.isEmpty()) {
            player.sendSystemMessage(TextUtil.warning("\u042d\u0442\u043e\u0439 \u0440\u0435\u043f\u043b\u0438\u043a\u0438 \u0431\u043e\u043b\u044c\u0448\u0435 \u043d\u0435\u0442."));
        }
        return true;
    }

    public Map<String, DialogueConfig> all() {
        return services.config().dialogues();
    }

    public int count() {
        return services.config().dialogues().size();
    }
}
