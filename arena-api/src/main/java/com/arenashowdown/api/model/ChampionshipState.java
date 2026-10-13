package com.arenashowdown.api.model;

public enum ChampionshipState {
    WAITING, PREPARING, RUNNING, PAUSED, FINISHED, CANCELLED;

    public boolean canTransitionTo(ChampionshipState next) {
        return switch (this) {
            case WAITING -> next == PREPARING || next == CANCELLED;
            case PREPARING -> next == RUNNING || next == CANCELLED;
            case RUNNING -> next == PAUSED || next == FINISHED || next == CANCELLED;
            case PAUSED -> next == RUNNING || next == FINISHED || next == CANCELLED;
            case FINISHED, CANCELLED -> false;
        };
    }
}
