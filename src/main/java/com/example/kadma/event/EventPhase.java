package com.example.kadma.event;

/** One phase of a running weekly rite. */
public class EventPhase {

    public enum State {
        PENDING, RUNNING, DONE, FAILED
    }

    public final com.example.kadma.config.EventPhase definition;
    public State state = State.PENDING;
    public int elapsedTicks = 0;
    public int score = 0;

    public EventPhase(com.example.kadma.config.EventPhase definition) {
        this.definition = definition;
    }

    public String id() {
        return definition == null ? "" : definition.id;
    }

    public String title() {
        return definition == null ? "" : definition.title;
    }

    public float progress() {
        if (definition == null || definition.seconds <= 0) {
            return 0.0F;
        }
        int total = definition.seconds * 20;
        return Math.min(1.0F, (float) elapsedTicks / (float) Math.max(1, total));
    }

    public boolean running() {
        return state == State.RUNNING;
    }

    public boolean done() {
        return state == State.DONE;
    }

    @Override
    public String toString() {
        return id() + ":" + state;
    }
}
