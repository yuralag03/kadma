package com.example.kadma.dialogue;

import com.example.kadma.quest.QuestService;
import com.example.kadma.story.StoryService;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative outcome applied by the server after a dialogue choice is validated.
 *
 * <p>Effects are the only way conversations can change state. They run on the
 * server thread, in order, and every reward is generated here.</p>
 */
public class DialogueEffect {

    public String type = "";
    public String key = "";
    public String value = "";
    public String questId = "";
    public String itemId = "";
    public String rumorId = "";
    public String chapterId = "";
    public String dialogueId = "";
    public String nodeId = "";
    public String journal = "";
    public String title = "";
    public String stat = "";
    public String text = "";
    public String scope = "player";
    public int amount = 1;
    public int stage = 0;

    public static List<DialogueEffect> readList(JsonObject raw, String member) {
        List<DialogueEffect> out = new ArrayList<>();
        if (raw == null) {
            return out;
        }
        com.google.gson.JsonArray array = com.example.kadma.data.JsonUtils.array(raw, member);
        if (array == null) {
            return out;
        }
        for (com.google.gson.JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            DialogueEffect effect = from(element.getAsJsonObject());
            if (!effect.type.isEmpty()) {
                out.add(effect);
            }
        }
        return out;
    }

    public static DialogueEffect from(JsonObject raw) {
        DialogueEffect effect = new DialogueEffect();
        if (raw == null) {
            return effect;
        }
        effect.type = com.example.kadma.data.JsonUtils.str(raw, "type", "").toLowerCase(java.util.Locale.ROOT);
        effect.key = com.example.kadma.data.JsonUtils.str(raw, "key", "");
        effect.value = com.example.kadma.data.JsonUtils.str(raw, "value", "");
        effect.questId = com.example.kadma.data.JsonUtils.str(raw, "questId", "");
        effect.itemId = com.example.kadma.data.JsonUtils.str(raw, "itemId", "");
        effect.rumorId = com.example.kadma.data.JsonUtils.str(raw, "rumorId", "");
        effect.chapterId = com.example.kadma.data.JsonUtils.str(raw, "chapterId", "");
        effect.dialogueId = com.example.kadma.data.JsonUtils.str(raw, "dialogueId", "");
        effect.nodeId = com.example.kadma.data.JsonUtils.str(raw, "nodeId", "");
        effect.journal = com.example.kadma.data.JsonUtils.str(raw, "journal", "");
        effect.title = com.example.kadma.data.JsonUtils.str(raw, "title", "");
        effect.stat = com.example.kadma.data.JsonUtils.str(raw, "stat", "");
        effect.text = com.example.kadma.data.JsonUtils.str(raw, "text", "");
        effect.scope = com.example.kadma.data.JsonUtils.str(raw, "scope", "player").toLowerCase(java.util.Locale.ROOT);
        effect.amount = com.example.kadma.data.JsonUtils.integer(raw, "amount", 1);
        effect.stage = com.example.kadma.data.JsonUtils.integer(raw, "stage", 0);
        return effect;
    }

    public String describe() {
        return type + (key.isEmpty() ? "" : " " + key) + (questId.isEmpty() ? "" : " " + questId);
    }

    public int apply(ServerPlayer player, com.example.kadma.KadmaServices services, com.example.kadma.data.PlayerData data,
                     com.example.kadma.data.GlobalData global) {
        int changed = 0;
        switch (type) {
            case "set_player_flag": {
                String target = key.isEmpty() ? itemId : key;
                String payload = value.isEmpty() ? "true" : value;
                if (data.flags.put(target, payload) == null || !payload.equals(data.flags.get(target))) {
                    changed++;
                }
                data.flags.put(target, payload);
                break;
            }
            case "set_global_flag": {
                String target = key.isEmpty() ? itemId : key;
                String payload = value.isEmpty() ? "true" : value;
                if (!payload.equals(global.flags.put(target, payload))) {
                    changed++;
                }
                break;
            }
            case "remove_player_flag": {
                if (data.flags.remove(key.isEmpty() ? itemId : key) != null) {
                    changed++;
                }
                break;
            }
            case "add_journal": {
                String entry = journal.isEmpty() ? text : journal;
                if (!data.hasJournal(entry)) {
                    data.addJournal(entry);
                    changed++;
                }
                break;
            }
            case "add_rumor": {
                String rumor = rumorId.isEmpty() ? key : rumorId;
                if (!data.knownRumor(rumor)) {
                    data.rememberRumor(rumor);
                    services.rumors().deliver(global, player, rumor);
                    changed++;
                }
                break;
            }
            case "start_quest": {
                QuestService quests = services.quests();
                boolean started = quests.startQuest(player, data, questId, true);
                if (started) {
                    changed++;
                }
                break;
            }
            case "complete_quest": {
                QuestService quests = services.quests();
                if (quests.completeQuest(player, data, questId, true)) {
                    changed++;
                }
                break;
            }
            case "give_item": {
                String target = itemId.isEmpty() ? key : itemId;
                if (target != null && services.config().items().containsKey(target)) {
                    net.minecraft.world.item.ItemStack created =
                            com.example.kadma.quest.RewardService.createStoryItem(services, target, Math.max(1, amount));
                    if (created != null) {
                        com.example.kadma.util.InventoryUtil.give(player, created);
                        changed++;
                    }
                }
                break;
            }
            case "enter_city": {
                if (!player.level().isClientSide()) {
                    services.lobby().enterCity(player);
                }
                changed++;
                break;
            }
            case "to_lobby": {
                services.lobby().toLobby(player);
                changed++;
                break;
            }
            case "change_stat": {
                if (!stat.isEmpty()) {
                    global.bumpStat(stat, amount);
                    changed++;
                }
                break;
            }
            case "set_stage": {
                String chapter = chapterId.isEmpty() ? data.chapter : chapterId;
                if (data.stage != stage) {
                    data.stage = stage;
                    data.chapter = chapter;
                    changed++;
                }
                break;
            }
            case "unlock_dialogue": {
                String id = dialogueId.isEmpty() ? key : dialogueId;
                if (id != null && data.unlockedDialogues.add(id)) {
                    changed++;
                }
                break;
            }
            case "prozrenie": {
                StoryService story = services.story();
                story.grantProzrenie(player, data, amount);
                changed++;
                break;
            }
            case "met_npc": {
                String npc = key.isEmpty() ? npcOf(itemId) : key;
                if (data.flags.put("npc.met." + npc, "true") == null) {
                    changed++;
                }
                data.flags.put("npc.met." + npc, "true");
                break;
            }
            case "title": {
                String heading = title.isEmpty() ? text : title;
                if (!heading.isEmpty()) {
                    services.visual().title(player, heading, "", 20, 60, 20);
                    changed++;
                }
                break;
            }
            default:
                break;
        }
        return changed;
    }

    private static String npcOf(String raw) {
        return raw == null ? "" : raw;
    }
}
