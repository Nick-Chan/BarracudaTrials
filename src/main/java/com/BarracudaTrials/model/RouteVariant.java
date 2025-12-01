package com.BarracudaTrials.model;

public enum RouteVariant {
    WIKI("wiki");

    private final String key;

    RouteVariant(String key) { this.key = key; }

    public String getKey() { return key; }
}
