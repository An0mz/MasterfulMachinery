package io.ticticboom.mods.mm.controller.machine.register;

import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public enum ModelDisplay {
    ON,
    OFF,
    HIDDEN;

    public ModelDisplay next(boolean hasAnimations) {
        return switch (this) {
            case ON -> hasAnimations ? OFF : HIDDEN;
            case OFF -> HIDDEN;
            case HIDDEN -> ON;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static ModelDisplay parse(String name) {
        for (ModelDisplay display : values()) {
            if (display.serializedName().equals(name)) {
                return display;
            }
        }
        return null;
    }
}
