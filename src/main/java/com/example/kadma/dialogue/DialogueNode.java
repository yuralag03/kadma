package com.example.kadma.dialogue;

import java.util.ArrayList;
import java.util.List;

/** One acceptance node of a conversation tree. */
public class DialogueNode {

    public String id = "";
    public String npc = "";
    public String dialogueId = "";
    public List<DialogueCondition> conditions = new ArrayList<>();
    public List<String> text = new ArrayList<>();
    public List<DialogueChoice> choices = new ArrayList<>();
    public List<DialogueEffect> effects = new ArrayList<>();
    public String next = "";
    public String journal = "";
    public String rumor = "";
    public String startQuest = "";
    public String completeQuest = "";
    public boolean root = false;
    public boolean terminal = true;

    public boolean hasVisibleChoice() {
        return choices != null && !choices.isEmpty();
    }

    public DialogueChoice choice(int index) {
        for (DialogueChoice candidate : choices) {
            if (candidate.index == index) {
                return candidate;
            }
        }
        return null;
    }

    public boolean hasConditions() {
        return conditions != null && !conditions.isEmpty();
    }
}
