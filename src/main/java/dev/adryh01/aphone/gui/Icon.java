package dev.adryh01.aphone.gui;

import org.bukkit.Material;

/**
 * App icons. With the resource pack they are PAPER + custom_model_data
 * (see resourcepack/assets/minecraft/models/item/paper.json).
 * Without it, the fallback vanilla item is used.
 */
public enum Icon {
    APP_PHONE(2001, Material.LIME_DYE),
    APP_MESSAGES(2002, Material.WRITABLE_BOOK),
    APP_CONTACTS(2003, Material.PLAYER_HEAD),
    APP_GPS(2004, Material.COMPASS),
    APP_FLASHLIGHT(2005, Material.TORCH),
    APP_WEATHER(2006, Material.CLOCK),
    APP_NOTES(2007, Material.PAPER),
    APP_CALCULATOR(2008, Material.REPEATER),
    APP_EMERGENCY(2009, Material.REDSTONE_BLOCK),
    APP_SETTINGS(2010, Material.COMPARATOR),
    HOME(2020, Material.NETHER_STAR),
    BACK(2021, Material.ARROW),
    ADD(2022, Material.LIME_STAINED_GLASS_PANE),
    CONTACT(2023, Material.PLAYER_HEAD),
    SMS(2024, Material.MAP),
    CALL_ACCEPT(2025, Material.LIME_CONCRETE),
    CALL_END(2026, Material.RED_CONCRETE),
    INFO(2027, Material.OAK_SIGN),
    PREV(2028, Material.SPECTRAL_ARROW),
    NEXT(2029, Material.SPECTRAL_ARROW),
    TOGGLE_ON(2030, Material.LIME_DYE),
    TOGGLE_OFF(2031, Material.GRAY_DYE),
    LOCATION(2032, Material.FILLED_MAP),
    NOTE(2033, Material.PAPER);

    private final int modelData;
    private final Material fallback;

    Icon(int modelData, Material fallback) {
        this.modelData = modelData;
        this.fallback = fallback;
    }

    public int modelData() { return modelData; }
    public Material fallback() { return fallback; }
}
