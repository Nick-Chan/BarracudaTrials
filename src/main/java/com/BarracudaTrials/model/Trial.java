package com.BarracudaTrials.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public enum Trial {
    GWENITH_GLIDE("gwenith_glide"),
    TEMPOR_TANTRUM("tempor_tantrum"),
    JUBBLY_JIVE("jubbly_jive");

    private final String key;

    Trial(String key) { this.key = key; }

    public String getKey() { return key; }
}
