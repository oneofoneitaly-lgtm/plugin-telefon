package dev.adryh01.aphone.listener;

import dev.adryh01.aphone.APhone;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.UUID;

/** Chat capture (SMS text, numbers...) and private call chat. */
public final class ChatListener implements Listener {

    private final APhone plugin;

    public ChatListener(APhone plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        Player p = e.getPlayer();
        UUID id = p.getUniqueId();
        String text = PlainTextComponentSerializer.plainText().serialize(e.message());

        if (plugin.chatInput().isWaiting(id)) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> plugin.chatInput().handle(p, text));
            return;
        }

        UUID partner = plugin.calls().partner(id);
        if (partner != null) {
            e.setCancelled(true);
            Player other = Bukkit.getPlayer(partner);
            var line = plugin.msg().get("call-format", "name", p.getName(), "message", text);
            p.sendMessage(line);
            if (other != null) other.sendMessage(line);
        }
    }
}
