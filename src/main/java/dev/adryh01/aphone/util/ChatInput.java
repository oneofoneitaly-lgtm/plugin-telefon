package dev.adryh01.aphone.util;

import dev.adryh01.aphone.APhone;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Captures the next chat line of a player (used for SMS text, numbers, notes...). */
public final class ChatInput {

    private final APhone plugin;
    private final Map<UUID, Consumer<String>> waiting = new ConcurrentHashMap<>();

    public ChatInput(APhone plugin) {
        this.plugin = plugin;
    }

    public void ask(Player p, String messageKey, Consumer<String> callback, String... ph) {
        p.closeInventory();
        plugin.msg().send(p, messageKey, ph);
        waiting.put(p.getUniqueId(), callback);
    }

    public boolean isWaiting(UUID id) {
        return waiting.containsKey(id);
    }

    /** Must be called on the main thread. */
    public void handle(Player p, String text) {
        Consumer<String> cb = waiting.remove(p.getUniqueId());
        if (cb == null) return;
        if (text.trim().equalsIgnoreCase("cancel")) {
            plugin.msg().send(p, "sms-cancelled");
            return;
        }
        cb.accept(text.trim());
    }

    public void clear(UUID id) {
        waiting.remove(id);
    }
}
