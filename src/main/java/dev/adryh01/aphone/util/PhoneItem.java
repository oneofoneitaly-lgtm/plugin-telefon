package dev.adryh01.aphone.util;

import dev.adryh01.aphone.APhone;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/** Creates and recognises the phone item (tagged with a PersistentDataContainer key). */
public final class PhoneItem {

    private final APhone plugin;
    private final NamespacedKey key;
    /** marks phone UI icons placed in the (hidden) player inventory, so they can never leak */
    private final NamespacedKey uiKey;

    public PhoneItem(APhone plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "phone");
        this.uiKey = new NamespacedKey(plugin, "ui");
    }

    /** @param number owner's number, or null for a blank (crafted) phone */
    public ItemStack create(String number) {
        Material mat = Material.matchMaterial(plugin.getConfig().getString("phone.material", "PAPER"));
        if (mat == null) mat = Material.PAPER;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        String shown = number == null ? "not activated" : number;
        meta.displayName(Msg.item(plugin.getConfig().getString("phone.name", "<white>Smartphone")));
        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("phone.lore")) {
            lore.add(Msg.item(line.replace("%number%", shown)));
        }
        meta.lore(lore);
        meta.setCustomModelData(plugin.getConfig().getInt("phone.custom-model-data", 1001));
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, number == null ? "" : number);
        meta.setMaxStackSize(1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isPhone(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public String numberOf(ItemStack item) {
        if (!isPhone(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public boolean hasPhone(Player p) {
        // Phone screen open = inventory temporarily hidden, but they are holding the phone
        if (plugin.hider() != null && plugin.hider().isHidden(p.getUniqueId())) return true;
        for (ItemStack it : p.getInventory().getContents()) {
            if (isPhone(it)) return true;
        }
        return false;
    }

    public void markUi(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(uiKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
    }

    public boolean isUi(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(uiKey, PersistentDataType.BYTE);
    }

    /** Removes every phone UI icon from the player's inventory. */
    public void stripUi(Player p) {
        ItemStack[] c = p.getInventory().getContents();
        for (int i = 0; i < c.length; i++) {
            if (isUi(c[i])) p.getInventory().setItem(i, null);
        }
        if (isUi(p.getItemOnCursor())) p.setItemOnCursor(null);
    }
}
