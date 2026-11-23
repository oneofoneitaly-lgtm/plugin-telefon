package dev.adryh01.aphone.util;

import dev.adryh01.aphone.APhone;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hides the 36 storage slots of the player's inventory while the phone is open,
 * so the client only sees the phone. Items are kept in memory AND on disk
 * (plugins/APhone/backups/uuid.yml) so nothing is lost on crash.
 *
 * APhone by @Adryh01
 */
public final class InventoryHider {

    private static final int STORAGE = 36;

    private final APhone plugin;
    private final File folder;
    private final Map<UUID, ItemStack[]> hidden = new ConcurrentHashMap<>();

    public InventoryHider(APhone plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "backups");
        if (!folder.exists() && !folder.mkdirs()) plugin.getLogger().warning("Could not create backups folder");
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("gui.custom-background", true)
                && plugin.getConfig().getBoolean("gui.hide-player-inventory", true);
    }

    public boolean isHidden(Player p) {
        return hidden.containsKey(p.getUniqueId());
    }

    public boolean isHidden(UUID id) {
        return hidden.containsKey(id);
    }

    /** Saves and clears the storage slots (hotbar + main inventory). */
    public void hide(Player p) {
        if (!enabled() || isHidden(p)) return;
        PlayerInventory inv = p.getInventory();
        ItemStack[] saved = new ItemStack[STORAGE];
        YamlConfiguration y = new YamlConfiguration();
        boolean any = false;
        for (int i = 0; i < STORAGE; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType().isAir() || plugin.phoneItem().isUi(it)) continue;
            saved[i] = it.clone();
            y.set("slots." + i, Base64.getEncoder().encodeToString(it.serializeAsBytes()));
            any = true;
        }
        hidden.put(p.getUniqueId(), saved);
        if (!any) return;
        try {
            y.save(file(p.getUniqueId()));
        } catch (IOException e) {
            // never clear items we could not back up
            hidden.remove(p.getUniqueId());
            plugin.getLogger().warning("Could not back up inventory of " + p.getName() + ": " + e.getMessage());
            return;
        }
        for (int i = 0; i < STORAGE; i++) inv.setItem(i, null);
    }

    /** Gives back the hidden items (anything that does not fit is dropped at the player's feet). */
    public void restore(Player p) {
        ItemStack[] saved = hidden.remove(p.getUniqueId());
        plugin.phoneItem().stripUi(p);
        if (saved == null) return;
        give(p, saved);
        deleteFile(p.getUniqueId());
    }

    /** Called on death without keepInventory: hidden items go to the drops. */
    public void restoreToDrops(Player p, List<ItemStack> drops) {
        ItemStack[] saved = hidden.remove(p.getUniqueId());
        drops.removeIf(plugin.phoneItem()::isUi);
        plugin.phoneItem().stripUi(p);
        if (saved == null) return;
        for (ItemStack it : saved) if (it != null) drops.add(it);
        deleteFile(p.getUniqueId());
    }

    /** Crash recovery: a backup file still exists on join. */
    public void recover(Player p) {
        if (hidden.containsKey(p.getUniqueId())) return;
        plugin.phoneItem().stripUi(p);
        File f = file(p.getUniqueId());
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ItemStack[] saved = new ItemStack[STORAGE];
        var sec = y.getConfigurationSection("slots");
        if (sec != null) {
            for (String k : sec.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(k);
                    saved[slot] = ItemStack.deserializeBytes(Base64.getDecoder().decode(sec.getString(k, "")));
                } catch (Exception e) {
                    plugin.getLogger().warning("Bad backup slot " + k + " for " + p.getName() + ": " + e.getMessage());
                }
            }
        }
        give(p, saved);
        deleteFile(p.getUniqueId());
        plugin.getLogger().info("Recovered hidden inventory of " + p.getName());
    }

    public void restoreAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isHidden(p)) {
                p.closeInventory();
                restore(p);
            }
        }
    }

    private void give(Player p, ItemStack[] saved) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < saved.length; i++) {
            ItemStack it = saved[i];
            if (it == null) continue;
            ItemStack cur = inv.getItem(i);
            if (cur == null || cur.getType().isAir()) {
                inv.setItem(i, it);
            } else {
                inv.addItem(it).values().forEach(left -> p.getWorld().dropItemNaturally(p.getLocation(), left));
            }
        }
    }

    private File file(UUID id) {
        return new File(folder, id + ".yml");
    }

    private void deleteFile(UUID id) {
        File f = file(id);
        if (f.exists() && !f.delete()) plugin.getLogger().warning("Could not delete backup " + f.getName());
    }
}
