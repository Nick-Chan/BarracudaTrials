package com.BarracudaTrials.model;

public enum Difficulty {
    SWORDFISH("swordfish"),
    SHARK("shark"),
    MARLIN("marlin");

    private final String key;

    Difficulty(String key) { this.key = key; }

    public String getKey() { return key; }
}
