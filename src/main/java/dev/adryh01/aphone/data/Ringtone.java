package dev.adryh01.aphone.data;

import org.bukkit.Sound;

public enum Ringtone {
    CLASSIC(Sound.BLOCK_NOTE_BLOCK_BELL, new float[]{1.0f, 1.26f, 1.0f, 1.26f}),
    CHIME(Sound.BLOCK_NOTE_BLOCK_CHIME, new float[]{0.8f, 1.0f, 1.2f, 1.5f}),
    PLING(Sound.BLOCK_NOTE_BLOCK_PLING, new float[]{1.5f, 1.0f, 1.5f, 1.0f}),
    RETRO(Sound.BLOCK_NOTE_BLOCK_BIT, new float[]{1.0f, 1.0f, 1.5f, 2.0f}),
    MARIMBA(Sound.BLOCK_NOTE_BLOCK_XYLOPHONE, new float[]{1.2f, 1.6f, 1.2f, 0.9f});

    private final Sound sound;
    private final float[] pitches;

    Ringtone(Sound sound, float[] pitches) {
        this.sound = sound;
        this.pitches = pitches;
    }

    public Sound sound() { return sound; }
    public float pitch(int step) { return pitches[Math.floorMod(step, pitches.length)]; }

    public Ringtone next() {
        Ringtone[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public String pretty() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    public static Ringtone parse(String s) {
        try {
            return valueOf(s.toUpperCase());
        } catch (Exception e) {
            return CLASSIC;
        }
    }
}
