package com.example.kadma.dialogue;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** A single dialogue acceptance node. */
public class DialogueChoice {

    public int index = 0;
    public String text = "";
    public String next = "";
    public List<DialogueCondition> conditions = new ArrayList<>();
    public List<DialogueEffect> effects = new ArrayList<>();
    public boolean terminal = true;

    public static DialogueChoice from(JsonObject raw, int fallbackIndex) {
        DialogueChoice choice = new DialogueChoice();
        choice.index = com.example.kadma.data.JsonUtils.integer(raw, "index", fallbackIndex);
        choice.text = com.example.kadma.data.JsonUtils.str(raw, "text", "...");
        choice.next = com.example.kadma.data.JsonUtils.str(raw, "next", "");
        choice.terminal = com.example.kadma.data.JsonUtils.bool(raw, "terminal", choice.next.isEmpty());
        choice.conditions = DialogueCondition.readList(raw, "conditions");
        choice.effects = DialogueEffect.readList(raw, "effects");
        return choice;
    }
}
