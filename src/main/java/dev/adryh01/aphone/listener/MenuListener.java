package dev.adryh01.aphone.listener;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/** Makes phone screens read-only, routes clicks and gives the hidden inventory back on close. */
public final class MenuListener implements Listener {

    private final APhone plugin;

    public MenuListener(APhone plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder(false) instanceof Menu menu)) {
            // safety: a phone icon should never exist outside the phone
            if (plugin.phoneItem().isUi(e.getCurrentItem())) {
                e.setCancelled(true);
                e.setCurrentItem(null);
            }
            return;
        }
        e.setCancelled(true);
        if (e.getClickedInventory() == null) return;
        // top = chest part, bottom = hidden player inventory used as phone screen/dock
        if (e.getClickedInventory() == e.getView().getTopInventory() || plugin.hider().isHidden((Player) e.getWhoClicked())) {
            menu.handleClick(e.getRawSlot(), e.getClick());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder(false) instanceof Menu) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder(false) instanceof Menu)) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        // wait 1 tick: if another phone screen was opened, keep the inventory hidden
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (p.getOpenInventory().getTopInventory().getHolder(false) instanceof Menu) return;
            plugin.hider().restore(p);
        });
    }

    @EventHandler
    public void onDropUi(org.bukkit.event.player.PlayerDropItemEvent e) {
        if (plugin.phoneItem().isUi(e.getItemDrop().getItemStack())) e.getItemDrop().remove();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && plugin.hider().isHidden(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!plugin.hider().isHidden(p)) return;
        if (e.getKeepInventory()) plugin.hider().restore(p);
        else plugin.hider().restoreToDrops(p, e.getDrops());
    }
}
