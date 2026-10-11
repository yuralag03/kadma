package com.example.kadma.quest;

import com.example.kadma.config.ObjectiveDefinition;
import com.example.kadma.data.ObjectiveProgress;

/** Live objective: definition plus authoritative progress. */
public class Objective {

    public final ObjectiveDefinition definition;
    public final ObjectiveProgress progress;

    public Objective(ObjectiveDefinition definition, ObjectiveProgress progress) {
        this.definition = definition;
        this.progress = progress;
    }

    public String id() {
        return definition == null ? "" : definition.id;
    }

    public ObjectiveType type() {
        return definition == null ? ObjectiveType.GOTO : ObjectiveType.parse(definition.type, ObjectiveType.GOTO);
    }

    public int amount() {
        return definition == null ? 1 : Math.max(1, definition.amount);
    }

    public int current() {
        return progress == null ? 0 : progress.progress;
    }

    public boolean complete() {
        return progress != null && progress.complete;
    }

    public void complete(boolean value) {
        if (progress != null) {
            progress.complete = value;
        }
    }

    public void add(int delta) {
        if (progress == null) {
            return;
        }
        progress.setProgress(Math.max(0, current() + delta));
        if (current() >= amount()) {
            progress.complete = true;
        }
    }

    public void setProgress(int value) {
        if (progress == null) {
            return;
        }
        progress.setProgress(value);
        if (current() >= amount()) {
            progress.complete = true;
        }
    }

    /** Progress text shown on the HUD. Computed on the server. */
    public String label() {
        if (definition == null) {
            return "";
        }
        String base = definition.description;
        switch (type()) {
            case COLLECT:
            case KILL:
                int done = Math.min(current(), amount());
                return base + ": " + done + "/" + amount();
            case DELIVER:
                return base + ": доставлено " + Math.min(current(), amount()) + "/" + amount();
            case TIMER:
            case SURVIVE:
                long left = Math.max(0L, definition.ticks - current());
                return base + ": " + com.example.kadma.util.TextUtil.ticks(left);
            case ENTER_LAYER:
            case GOTO:
            case TALK:
            case INTERACT:
            case CHOOSE:
            case USE_ITEM:
            case REACH_FLAG:
            default:
                return base + (complete() ? " \u2014 \u0433\u043e\u0442\u043e\u0432\u043e" : "");
        }
    }

    public String shortLabel() {
        return label();
    }
}
