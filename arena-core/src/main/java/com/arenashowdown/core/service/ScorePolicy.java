package com.arenashowdown.core.service;

@FunctionalInterface
public interface ScorePolicy { int points(String minigame,int position,int submittedPoints); }
