package com.example.kadma.data;

/** Single objective progress inside an active quest. */
public class ObjectiveProgress {

    public String id = "";
    public int progress = 0;
    public boolean complete = false;
    public int tickBudget = 0;
    public String note = "";

    public ObjectiveProgress() {
    }

    public ObjectiveProgress(String id) {
        this.id = id == null ? "" : id;
    }

    public void setProgress(int value) {
        this.progress = Math.max(0, value);
    }

    public boolean isComplete() {
        return complete;
    }
}
