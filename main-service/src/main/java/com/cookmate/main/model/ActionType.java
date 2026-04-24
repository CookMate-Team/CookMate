package com.cookmate.main.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum ActionType {
    WEIGH("Ważenie"),
    POUR("Nalewanie"),
    MIX("Mieszanie ogólne"),
    CUT("Krojenie"),
    CHOP("Szatkowanie"),
    STIR("Mieszanie łyżką"),
    FRYING_PAN("Smażenie na patelni"),
    POT("Gotowanie w garnku"),
    WAIT("Oczekiwanie"),
    BAKE("Pieczenie"),
    GRILL("Grillowanie"),
    BLEND("Blendowanie"),
    MARINATE("Marynowanie");

    private final String displayName;

    ActionType(String displayName) {
        this.displayName = displayName;
    }
}
