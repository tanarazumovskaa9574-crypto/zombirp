package com.reputationmod.stamina;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.List;

public class StaminaData implements INBTSerializable<CompoundTag> {
    private int stamina;
    private int maxStamina;
    private long lastExertionTime;
    private boolean isDirty;

    // Прокачка
    private int level = 0;
    private int experience = 0;
    private long lastExperienceTime = 0;
    private boolean levelIncreased = false;

    public StaminaData() {
        this.maxStamina = LevelingConfig.COMMON.BASE_MAX_STAMINA.get();
        this.stamina = maxStamina;
        this.lastExertionTime = 0;
        this.isDirty = false;
        System.out.println("[StaminaDebug] Constructor: maxStamina=" + maxStamina + ", stamina=" + stamina);
    }

    public int getStamina() { return stamina; }
    public int getMaxStamina() { return maxStamina; }
    public int getLevel() { return level; }
    public int getExperience() { return experience; }

    public int getRequiredExpForNextLevel() {
        return (level + 1) * LevelingConfig.COMMON.EXP_PER_LEVEL.get();
    }

    public float getProgress() {
        int maxLevel = LevelingConfig.COMMON.MAX_LEVEL.get();
        if (level >= maxLevel) return 1.0f;
        int required = getRequiredExpForNextLevel();
        int prevRequired = level * LevelingConfig.COMMON.EXP_PER_LEVEL.get();
        return (float)(experience - prevRequired) / (required - prevRequired);
    }

    private float getCurrentExpMultiplier() {
        List<? extends Double> multipliers = LevelingConfig.COMMON.EXP_MULTIPLIERS.get();
        float mult;
        if (level < multipliers.size()) {
            mult = multipliers.get(level).floatValue();
        } else {
            mult = multipliers.get(multipliers.size() - 1).floatValue();
        }
        System.out.println("[StaminaDebug] getCurrentExpMultiplier: level=" + level + ", multiplier=" + mult);
        return mult;
    }

    private boolean canGainExperience() {
        long now = System.currentTimeMillis();
        boolean can = now - lastExperienceTime >= LevelingConfig.COMMON.EXPERIENCE_COOLDOWN_MS.get();
        System.out.println("[StaminaDebug] canGainExperience: lastExpTime=" + lastExperienceTime + ", now=" + now + ", diff=" + (now - lastExperienceTime) + ", cooldown=" + LevelingConfig.COMMON.EXPERIENCE_COOLDOWN_MS.get() + ", result=" + can);
        return can;
    }

    public void setStamina(int stamina) {
        this.stamina = Math.min(stamina, maxStamina);
        this.isDirty = true;
        System.out.println("[StaminaDebug] setStamina: new stamina=" + this.stamina + ", max=" + maxStamina);
    }

    public void setMaxStamina(int maxStamina) {
        this.maxStamina = maxStamina;
        if (this.stamina > this.maxStamina) {
            this.stamina = this.maxStamina;
        }
        this.isDirty = true;
        System.out.println("[StaminaDebug] setMaxStamina: new max=" + maxStamina + ", stamina now=" + this.stamina);
    }

    public void setLevel(int level) {
        this.level = level;
        this.isDirty = true;
        System.out.println("[StaminaDebug] setLevel: level=" + level);
    }

    public void setExperience(int experience) {
        this.experience = experience;
        this.isDirty = true;
        System.out.println("[StaminaDebug] setExperience: exp=" + experience);
    }

    private void checkLevelUp() {
        int maxLevel = LevelingConfig.COMMON.MAX_LEVEL.get();
        int expPerLevel = LevelingConfig.COMMON.EXP_PER_LEVEL.get();
        System.out.println("[StaminaDebug] checkLevelUp: entering, level=" + level + ", exp=" + experience + ", maxLevel=" + maxLevel + ", expPerLevel=" + expPerLevel);
        while (level < maxLevel && experience >= (level + 1) * expPerLevel) {
            level++;
            maxStamina += LevelingConfig.COMMON.STAMINA_PER_LEVEL.get();
            stamina += LevelingConfig.COMMON.STAMINA_PER_LEVEL.get();
            levelIncreased = true;
            System.out.println("[StaminaDebug] checkLevelUp: level increased to " + level + ", new maxStamina=" + maxStamina + ", stamina=" + stamina);
        }
        System.out.println("[StaminaDebug] checkLevelUp: finished, level=" + level + ", maxStamina=" + maxStamina);
    }

    public boolean consumeLevelIncreased() {
        if (levelIncreased) {
            levelIncreased = false;
            System.out.println("[StaminaDebug] consumeLevelIncreased: true (level up consumed)");
            return true;
        }
        return false;
    }

    public boolean hasStamina(int cost) {
        boolean has = stamina >= cost;
        System.out.println("[StaminaDebug] hasStamina: cost=" + cost + ", stamina=" + stamina + ", result=" + has);
        return has;
    }

    public boolean trySpend(int cost) {
        System.out.println("[StaminaDebug] trySpent: cost=" + cost + ", current stamina=" + stamina + ", max=" + maxStamina + ", level=" + level + ", exp=" + experience);
        if (hasStamina(cost)) {
            stamina -= cost;
            System.out.println("[StaminaDebug] after spend: stamina=" + stamina);
            if (canGainExperience()) {
                float multiplier = getCurrentExpMultiplier();
                int expGain = (int)(cost * multiplier);
                System.out.println("[StaminaDebug] gaining exp: multiplier=" + multiplier + ", expGain=" + expGain);
                if (expGain > 0) {
                    experience += expGain;
                    lastExperienceTime = System.currentTimeMillis();
                    System.out.println("[StaminaDebug] exp now=" + experience);
                    checkLevelUp();
                } else {
                    System.out.println("[StaminaDebug] expGain <=0, no experience added");
                }
            } else {
                System.out.println("[StaminaDebug] cannot gain exp (cooldown)");
            }
            lastExertionTime = System.currentTimeMillis();
            isDirty = true;
            System.out.println("[StaminaDebug] trySpend returning true (success)");
            return true;
        } else {
            System.out.println("[StaminaDebug] trySpend returning false (insufficient stamina)");
            return false;
        }
    }

    public void tickRegen() {
        long now = System.currentTimeMillis();
        long regenDelayMs = StaminaConfig.COMMON.REGEN_DELAY.get() * 1000L;
        if (now - lastExertionTime > regenDelayMs) {
            int regenRate = StaminaConfig.COMMON.REGEN_RATE.get();
            if (stamina < maxStamina) {
                stamina = Math.min(stamina + regenRate, maxStamina);
                isDirty = true;
                System.out.println("[StaminaDebug] tickRegen: regen +" + regenRate + ", stamina now=" + stamina);
            }
        }
    }

    public void setLastExertionTime() {
        this.lastExertionTime = System.currentTimeMillis();
        isDirty = true;
        System.out.println("[StaminaDebug] setLastExertionTime: time=" + lastExertionTime);
    }

    public boolean isDirty() { return isDirty; }
    public void setDirty(boolean dirty) { isDirty = dirty; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("stamina", stamina);
        tag.putInt("maxStamina", maxStamina);
        tag.putLong("lastExertionTime", lastExertionTime);
        tag.putInt("level", level);
        tag.putInt("experience", experience);
        tag.putLong("lastExperienceTime", lastExperienceTime);
        System.out.println("[StaminaDebug] serializeNBT: saving stamina=" + stamina + ", max=" + maxStamina + ", level=" + level + ", exp=" + experience);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        stamina = nbt.getInt("stamina");
        maxStamina = nbt.getInt("maxStamina");
        lastExertionTime = nbt.getLong("lastExertionTime");
        level = nbt.getInt("level");
        experience = nbt.getInt("experience");
        lastExperienceTime = nbt.getLong("lastExperienceTime");
        System.out.println("[StaminaDebug] deserializeNBT: loaded stamina=" + stamina + ", max=" + maxStamina + ", level=" + level + ", exp=" + experience);
    }
}