package com.cookmate.main.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

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

    @JsonCreator
    public static ActionType fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        for (ActionType type : ActionType.values()) {
            if (type.name().equals(normalized)) {
                return type;
            }
        }
        return switch (normalized) {
            case "FRY", "PAN", "SAUTE", "SAUTÉ" -> FRYING_PAN;
            case "BOIL", "COOK", "HEAT", "SIMMER" -> POT;
            case "POURING", "ADD" -> POUR;
            case "WEIGHING" -> WEIGH;
            case "MIXING", "COMBINE" -> MIX;
            case "CHOPPING" -> CHOP;
            case "CUTTING", "SLICE", "SLICING" -> CUT;
            case "STIRRING" -> STIR;
            case "WAITING", "SERVE", "SERVING", "GARNISH" -> WAIT;
            case "BAKING" -> BAKE;
            case "GRILLING" -> GRILL;
            case "BLENDING" -> BLEND;
            case "MARINATING" -> MARINATE;
            default -> null;
        };
    }
}
