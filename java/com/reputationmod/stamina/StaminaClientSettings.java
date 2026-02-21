package com.reputationmod.stamina;

public class StaminaClientSettings {
    private static boolean hudEnabled = true;

    public static boolean isHudEnabled() {
        return hudEnabled;
    }

    public static void setHudEnabled(boolean enabled) {
        hudEnabled = enabled;
    }
}