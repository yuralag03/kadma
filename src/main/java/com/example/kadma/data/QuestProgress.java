package com.example.kadma.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Runtime progress of one accepted quest. Fully serialised, so no state is lost on restart. */
public class QuestProgress {

    public String questId = "";
    public String questType = "main";
    public long startedDay = 0L;
    public long startedAt = 0L;
    public String currentObjective = "";
    public boolean complete = false;
    public boolean rewarded = false;
    public boolean abandoned = false;
    public Map<String, ObjectiveProgress> objectives = new LinkedHashMap<>();

    public QuestProgress() {
    }

    public QuestProgress(String questId, String questType, long day, long startedAt) {
        this.questId = questId == null ? "" : questId;
        this.questType = questType == null ? "main" : questType;
        this.startedDay = day;
        this.startedAt = startedAt;
    }

    public ObjectiveProgress objective(String id) {
        return objectives.get(id);
    }

    public void put(ObjectiveProgress progress) {
        if (progress != null && progress.id != null) {
            objectives.put(progress.id, progress);
        }
    }

    public void ensureObjective(String id) {
        if (!objectives.containsKey(id)) {
            objectives.put(id, new ObjectiveProgress(id));
        }
    }

    public List<ObjectiveProgress> ordered() {
        return new ArrayList<>(objectives.values());
    }

    public int completedCount() {
        int total = 0;
        for (ObjectiveProgress progress : objectives.values()) {
            if (progress.complete) {
                total++;
            }
        }
        return total;
    }

    public int totalCount() {
        return objectives.size();
    }

    /** Human readable progress such as "2/3" for the HUD. */
    public String shortProgress() {
        return completedCount() + "/" + totalCount();
    }
}
