package com.example.kadma.quest;

import com.example.kadma.config.QuestConfig;
import com.example.kadma.data.ObjectiveProgress;
import com.example.kadma.data.QuestProgress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Live quest: definition plus persisted progress. */
public class QuestInstance {

    public final QuestConfig definition;
    public final QuestProgress progress;

    private final Map<String, Objective> objectives = new LinkedHashMap<>();

    public QuestInstance(QuestConfig definition, QuestProgress progress) {
        this.definition = definition;
        this.progress = progress;
        rebuild();
    }

    private void rebuild() {
        objectives.clear();
        if (definition == null || definition.objectives == null) {
            return;
        }
        for (com.example.kadma.config.ObjectiveDefinition objective : definition.objectives) {
            ObjectiveProgress stored = progress.objectives.get(objective.id);
            if (stored == null) {
                stored = new ObjectiveProgress(objective.id);
                progress.put(stored);
            }
            objectives.put(objective.id, new Objective(objective, stored));
        }
    }

    public String id() {
        return definition == null ? "" : definition.id;
    }

    public String title() {
        return definition == null ? "" : definition.title;
    }

    public String type() {
        return definition == null ? "side" : definition.type;
    }

    public Objective objective(String id) {
        return objectives.get(id);
    }

    public List<Objective> objectives() {
        return new ArrayList<>(objectives.values());
    }

    public int completed() {
        int total = 0;
        for (Objective objective : objectives.values()) {
            if (objective.complete()) {
                total++;
            }
        }
        return total;
    }

    public int total() {
        return objectives.size();
    }

    public Objective firstIncomplete() {
        for (Objective objective : objectives.values()) {
            if (!objective.complete() && !objective.definition.optional) {
                return objective;
            }
        }
        for (Objective objective : objectives.values()) {
            if (!objective.complete()) {
                return objective;
            }
        }
        return null;
    }

    public boolean allComplete() {
        for (Objective objective : objectives.values()) {
            if (!objective.complete() && !objective.definition.optional) {
                return false;
            }
        }
        return total() > 0;
    }

    public void markCurrent(String objectiveId) {
        if (objectiveId != null && !objectiveId.isEmpty()) {
            progress.currentObjective = objectiveId;
        }
    }

    public String currentObjectiveId() {
        Objective objective = firstIncomplete();
        return objective == null ? "" : objective.id();
    }

    public String hudObjective() {
        Objective objective = firstIncomplete();
        if (objective == null) {
            return definition != null && !definition.objectives.isEmpty()
                    ? definition.objectives.get(definition.objectives.size() - 1).description
                    : "";
        }
        return objective.label();
    }

    public String hudProgress() {
        return completed() + "/" + total();
    }
}
