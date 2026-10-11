package com.example.kadma.config;

import com.example.kadma.dialogue.DialogueChoice;
import com.example.kadma.dialogue.DialogueCondition;
import com.example.kadma.dialogue.DialogueEffect;
import com.example.kadma.dialogue.DialogueNode;
import com.example.kadma.dialogue.DialogueChoice;
import com.example.kadma.dialogue.DialogueCondition;
import com.example.kadma.dialogue.DialogueEffect;
import com.example.kadma.dialogue.DialogueNode;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A full dialogue tree for one NPC. */
public class DialogueConfig {

    public String id = "";
    public String npc = "";
    public String title = "";
    public Map<String, DialogueNode> nodes = new LinkedHashMap<>();
    public String rootNode = "root";

    public static DialogueConfig from(JsonObject raw) {
        DialogueConfig dialogue = new DialogueConfig();
        if (raw == null) {
            return dialogue;
        }
        dialogue.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        dialogue.npc = com.example.kadma.data.JsonUtils.str(raw, "npc", "");
        dialogue.title = com.example.kadma.data.JsonUtils.str(raw, "title", dialogue.id);
        dialogue.rootNode = com.example.kadma.data.JsonUtils.str(raw, "root", "root");
        com.google.gson.JsonArray rawNodes = com.example.kadma.data.JsonUtils.array(raw, "nodes");
        if (rawNodes == null) {
            return dialogue;
        }
        if (!rawNodes.isEmpty() && rawNodes.get(0).isJsonObject()
                && rawNodes.get(0).getAsJsonObject().has("id")) {
            for (com.google.gson.JsonElement element : rawNodes) {
                if (!element.isJsonObject()) {
                    continue;
                }
                DialogueNode node = nodeFrom(element.getAsJsonObject(), dialogue.id, dialogue.npc);
                dialogue.nodes.put(node.id, node);
            }
            return dialogue;
        }
        // {"nodes": {"root": {...}}}
        JsonObject map = com.example.kadma.data.JsonUtils.object(raw, "nodes");
        if (map != null) {
            for (Map.Entry<String, com.google.gson.JsonElement> entry : map.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                DialogueNode node = nodeFrom(entry.getValue().getAsJsonObject(), dialogue.id, dialogue.npc);
                if (node.id.isEmpty() || "root".equals(node.id)) {
                    node.id = entry.getKey();
                }
                dialogue.nodes.put(node.id, node);
            }
        }
        return dialogue;
    }

    private static DialogueNode nodeFrom(JsonObject raw, String dialogueId, String ownerNpc) {
        DialogueNode node = new DialogueNode();
        node.dialogueId = dialogueId;
        node.id = com.example.kadma.data.JsonUtils.str(raw, "id", "");
        node.npc = com.example.kadma.data.JsonUtils.str(raw, "npc", ownerNpc);
        node.root = com.example.kadma.data.JsonUtils.bool(raw, "root", "root".equals(node.id));
        node.conditions = DialogueCondition.readList(raw, "conditions");
        node.text = com.example.kadma.data.JsonUtils.stringList(raw, "text");
        node.effects = DialogueEffect.readList(raw, "effects");
        node.next = com.example.kadma.data.JsonUtils.str(raw, "next", "");
        node.journal = com.example.kadma.data.JsonUtils.str(raw, "journal", "");
        node.rumor = com.example.kadma.data.JsonUtils.str(raw, "rumor", "");
        node.startQuest = com.example.kadma.data.JsonUtils.str(raw, "startQuest", "");
        node.completeQuest = com.example.kadma.data.JsonUtils.str(raw, "completeQuest", "");
        node.terminal = com.example.kadma.data.JsonUtils.bool(raw, "terminal", node.next.isEmpty() && !node.hasVisibleChoice());

        com.google.gson.JsonArray rawChoices = com.example.kadma.data.JsonUtils.array(raw, "choices");
        if (rawChoices != null) {
            int index = 0;
            for (com.google.gson.JsonElement element : rawChoices) {
                if (!element.isJsonObject()) {
                    continue;
                }
                node.choices.add(DialogueChoice.from(element.getAsJsonObject(), index++));
            }
        }
        if (node.text.isEmpty()) {
            List<String> fallback = new ArrayList<>();
            fallback.add("...");
            node.text = fallback;
        }
        return node;
    }

    public DialogueNode root() {
        DialogueNode node = nodes.get(rootNode);
        if (node != null) {
            return node;
        }
        return nodes.values().isEmpty() ? null : nodes.values().iterator().next();
    }

    public DialogueNode node(String id) {
        return nodes.get(id);
    }
}
