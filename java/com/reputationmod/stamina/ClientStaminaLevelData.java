package com.reputationmod.stamina;

public class ClientStaminaLevelData {
    private static int level = 0;
    private static int experience = 0;
    private static int maxStamina = 100;

    public static void set(int lvl, int exp, int max) {
        level = lvl;
        experience = exp;
        maxStamina = max;
    }

    public static int getLevel() { return level; }
    public static int getExperience() { return experience; }
    public static int getMaxStamina() { return maxStamina; }
}