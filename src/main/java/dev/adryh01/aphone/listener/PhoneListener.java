package dev.adryh01.aphone.listener;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.menus.HomeMenu;
import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.net.URI;
import java.util.UUID;

public final class PhoneListener implements Listener {

    private final APhone plugin;

    public PhoneListener(APhone plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.hider().recover(p);
        PhoneData d = plugin.data().get(p);
        if (plugin.getConfig().getBoolean("phone.give-on-first-join", true) && !d.receivedPhone()) {
            d.receivedPhone(true);
            p.getInventory().addItem(plugin.phoneItem().create(d.number()));
            plugin.msg().send(p, "phone-received", "number", d.number());
        }
        plugin.data().save(d);
        Actions.applyFlashlight(plugin, p);
        if (d.unread() > 0 || d.missedCalls() > 0) {
            p.sendMessage(plugin.msg().prefixed(net.kyori.adventure.text.Component.text(
                    "You have " + d.unread() + " unread SMS and " + d.missedCalls() + " missed calls.")));
        }
        sendPack(p);
    }

    private void sendPack(Player p) {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", false)) return;
        String url = plugin.getConfig().getString("resource-pack.url", "");
        if (url.isBlank()) return;
        try {
            String sha1 = plugin.getConfig().getString("resource-pack.sha1", "");
            ResourcePackInfo info = ResourcePackInfo.resourcePackInfo(
                    UUID.nameUUIDFromBytes(url.getBytes()), URI.create(url), sha1 == null ? "" : sha1);
            ResourcePackRequest req = ResourcePackRequest.resourcePackRequest()
                    .packs(info)
                    .required(plugin.getConfig().getBoolean("resource-pack.required", false))
                    .prompt(dev.adryh01.aphone.util.Msg.MM.deserialize(plugin.getConfig().getString("resource-pack.prompt", "")))
                    .build();
            Bukkit.getScheduler().runTaskLater(plugin, () -> p.sendResourcePacks(req), 20L);
        } catch (Exception ex) {
            plugin.getLogger().warning("Invalid resource-pack url: " + ex.getMessage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (plugin.hider().isHidden(e.getPlayer().getUniqueId())) plugin.hider().restore(e.getPlayer());
        plugin.calls().handleQuit(e.getPlayer());
        PhoneData d = plugin.data().get(e.getPlayer());
        plugin.data().save(d);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> Actions.applyFlashlight(plugin, e.getPlayer()), 2L);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (!plugin.phoneItem().isPhone(item)) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (!p.hasPermission("aphone.use")) {
            plugin.msg().send(p, "no-permission");
            return;
        }
        PhoneData d = plugin.data().get(p);
        // Activate blank / someone else's phone with the holder's number
        if (!d.number().equals(plugin.phoneItem().numberOf(item))) {
            ItemStack fresh = plugin.phoneItem().create(d.number());
            fresh.setAmount(item.getAmount());
            if (e.getHand() != null) p.getInventory().setItem(e.getHand(), fresh);
        }
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.5f, 2f);
        new HomeMenu(plugin, p).open();
    }
}
