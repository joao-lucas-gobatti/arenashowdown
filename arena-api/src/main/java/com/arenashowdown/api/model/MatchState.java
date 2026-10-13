package com.arenashowdown.api.model;

public enum MatchState {
    WAITING, PREPARING, COUNTDOWN, RUNNING, FINISHED, CANCELLED;

    public boolean canTransitionTo(MatchState next) {
        return switch (this) {
            case WAITING -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == COUNTDOWN || next == CANCELLED;
            case COUNTDOWN -> next == RUNNING || next == CANCELLED;
            case RUNNING -> next == FINISHED || next == CANCELLED;
            case FINISHED, CANCELLED -> false;
        };
    }
}
