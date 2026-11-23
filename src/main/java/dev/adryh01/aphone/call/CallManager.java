package dev.adryh01.aphone.call;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.util.Msg;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Handles ringing, active calls and call chat routing. */
public final class CallManager {

    private final APhone plugin;
    /** callee -> caller */
    private final Map<UUID, UUID> ringing = new ConcurrentHashMap<>();
    /** caller -> callee */
    private final Map<UUID, UUID> outgoing = new ConcurrentHashMap<>();
    /** both directions */
    private final Map<UUID, UUID> active = new ConcurrentHashMap<>();
    private final Map<UUID, Long> startedAt = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> ringSeconds = new ConcurrentHashMap<>();
    private BukkitTask ticker;

    public CallManager(APhone plugin) {
        this.plugin = plugin;
    }

    public boolean isBusy(UUID id) {
        return ringing.containsKey(id) || outgoing.containsKey(id) || active.containsKey(id);
    }

    public UUID partner(UUID id) { return active.get(id); }
    public boolean isRinging(UUID callee) { return ringing.containsKey(callee); }
    public boolean isCalling(UUID caller) { return outgoing.containsKey(caller); }
    public boolean inCall(UUID id) { return active.containsKey(id); }

    public void call(Player caller, PhoneData target) {
        PhoneData me = plugin.data().get(caller);
        if (target.uuid().equals(caller.getUniqueId())) {
            plugin.msg().send(caller, "call-self");
            return;
        }
        Player t = Bukkit.getPlayer(target.uuid());
        if (t == null || !plugin.phoneItem().hasPhone(t)) {
            plugin.msg().send(caller, "call-offline", "number", target.number());
            return;
        }
        if (isBusy(caller.getUniqueId()) || isBusy(t.getUniqueId())) {
            plugin.msg().send(caller, "call-busy");
            return;
        }
        if (target.silent()) {
            target.missedCalls(target.missedCalls() + 1);
            plugin.msg().send(caller, "call-silent", "name", target.name());
            return;
        }
        ringing.put(t.getUniqueId(), caller.getUniqueId());
        outgoing.put(caller.getUniqueId(), t.getUniqueId());
        ringSeconds.put(t.getUniqueId(), 0);

        plugin.msg().send(caller, "call-calling", "name", target.name(), "number", target.number());
        plugin.msg().send(t, "call-incoming", "name", me.name(), "number", me.number());
        Component accept = plugin.msg().get("call-accept-button")
                .clickEvent(ClickEvent.runCommand("/aphone accept"))
                .hoverEvent(HoverEvent.showText(Msg.MM.deserialize("<green>Answer the call")));
        Component decline = plugin.msg().get("call-decline-button")
                .clickEvent(ClickEvent.runCommand("/aphone decline"))
                .hoverEvent(HoverEvent.showText(Msg.MM.deserialize("<red>Decline the call")));
        t.sendMessage(Component.text("   ").append(accept).append(Component.text("   ")).append(decline));
        t.showTitle(net.kyori.adventure.title.Title.title(
                Msg.MM.deserialize("<aqua>☎ " + Msg.esc(me.name())),
                Msg.MM.deserialize("<gray>is calling you...")));
    }

    public void accept(Player callee) {
        UUID callerId = ringing.remove(callee.getUniqueId());
        ringSeconds.remove(callee.getUniqueId());
        if (callerId == null) {
            plugin.msg().send(callee, "call-none");
            return;
        }
        outgoing.remove(callerId);
        Player caller = Bukkit.getPlayer(callerId);
        if (caller == null) {
            plugin.msg().send(callee, "call-no-answer");
            return;
        }
        active.put(callerId, callee.getUniqueId());
        active.put(callee.getUniqueId(), callerId);
        long now = System.currentTimeMillis();
        startedAt.put(callerId, now);
        startedAt.put(callee.getUniqueId(), now);
        callee.clearTitle();
        plugin.msg().send(caller, "call-started", "name", callee.getName());
        plugin.msg().send(callee, "call-started", "name", caller.getName());
        caller.playSound(caller.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
        callee.playSound(callee.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.5f);
    }

    public void decline(Player callee) {
        UUID callerId = ringing.remove(callee.getUniqueId());
        ringSeconds.remove(callee.getUniqueId());
        if (callerId == null) {
            plugin.msg().send(callee, "call-none");
            return;
        }
        outgoing.remove(callerId);
        callee.clearTitle();
        Player caller = Bukkit.getPlayer(callerId);
        if (caller != null) {
            plugin.msg().send(caller, "call-declined", "name", callee.getName());
            caller.playSound(caller.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
        }
        plugin.msg().send(callee, "call-ended", "duration", "0:00");
    }

    /** Hang up whatever is going on (active call, outgoing or incoming). */
    public void hangup(Player p) {
        UUID id = p.getUniqueId();
        if (active.containsKey(id)) {
            end(id, active.get(id));
        } else if (outgoing.containsKey(id)) {
            UUID callee = outgoing.remove(id);
            ringing.remove(callee);
            ringSeconds.remove(callee);
            missed(callee, id);
            plugin.msg().send(p, "call-ended", "duration", "0:00");
        } else if (ringing.containsKey(id)) {
            decline(p);
        } else {
            plugin.msg().send(p, "call-none");
        }
    }

    private void end(UUID a, UUID b) {
        long start = startedAt.getOrDefault(a, System.currentTimeMillis());
        String dur = duration((System.currentTimeMillis() - start) / 1000);
        active.remove(a);
        active.remove(b);
        startedAt.remove(a);
        startedAt.remove(b);
        for (UUID u : new UUID[]{a, b}) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) {
                plugin.msg().send(p, "call-ended", "duration", dur);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
                p.sendActionBar(Component.empty());
            }
        }
    }

    private void missed(UUID callee, UUID caller) {
        PhoneData target = plugin.data().get(callee);
        PhoneData from = plugin.data().get(caller);
        if (target == null || from == null) return;
        target.missedCalls(target.missedCalls() + 1);
        Player t = Bukkit.getPlayer(callee);
        if (t != null) {
            t.clearTitle();
            plugin.msg().send(t, "call-missed", "name", from.name(), "number", from.number());
        }
    }

    public void handleQuit(Player p) {
        UUID id = p.getUniqueId();
        plugin.chatInput().clear(id);
        if (active.containsKey(id)) end(id, active.get(id));
        if (outgoing.containsKey(id)) {
            UUID callee = outgoing.remove(id);
            ringing.remove(callee);
            ringSeconds.remove(callee);
            missed(callee, id);
        }
        if (ringing.containsKey(id)) {
            UUID caller = ringing.remove(id);
            ringSeconds.remove(id);
            outgoing.remove(caller);
            Player c = Bukkit.getPlayer(caller);
            if (c != null) plugin.msg().send(c, "call-no-answer");
        }
    }

    public void endAll() {
        Set<UUID> done = new HashSet<>();
        for (Map.Entry<UUID, UUID> e : active.entrySet()) {
            if (done.add(e.getKey()) && done.add(e.getValue())) end(e.getKey(), e.getValue());
        }
        ringing.clear();
        outgoing.clear();
        if (ticker != null) ticker.cancel();
    }

    /** Runs every second: ringtone, timeouts, call timer on action bar. */
    public void startTicker() {
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int timeout = plugin.getConfig().getInt("call.ring-timeout", 20);
            for (UUID callee : new HashSet<>(ringing.keySet())) {
                int sec = ringSeconds.merge(callee, 1, Integer::sum);
                UUID caller = ringing.get(callee);
                Player t = Bukkit.getPlayer(callee);
                Player c = caller == null ? null : Bukkit.getPlayer(caller);
                if (sec > timeout || t == null) {
                    ringing.remove(callee);
                    ringSeconds.remove(callee);
                    if (caller != null) outgoing.remove(caller);
                    if (c != null) plugin.msg().send(c, "call-no-answer");
                    if (caller != null) missed(callee, caller);
                    continue;
                }
                PhoneData td = plugin.data().get(callee);
                if (td != null) {
                    t.getWorld().playSound(t.getLocation(), td.ringtone().sound(), 1f, td.ringtone().pitch(sec));
                }
                t.sendActionBar(Msg.MM.deserialize("<aqua>☎ Incoming call... <gray>/aphone accept"));
                if (c != null) {
                    c.sendActionBar(Msg.MM.deserialize("<gray>☎ Ringing" + ".".repeat(1 + sec % 3)));
                    c.playSound(c.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.4f, 0.8f);
                }
            }
            int max = plugin.getConfig().getInt("call.max-duration", 0);
            Set<UUID> done = new HashSet<>();
            for (Map.Entry<UUID, UUID> e : active.entrySet()) {
                if (!done.add(e.getKey())) continue;
                done.add(e.getValue());
                long secs = (System.currentTimeMillis() - startedAt.getOrDefault(e.getKey(), System.currentTimeMillis())) / 1000;
                if (max > 0 && secs >= max) {
                    end(e.getKey(), e.getValue());
                    continue;
                }
                Component bar = Msg.MM.deserialize("<green>☎ In call <white>" + duration(secs));
                Player a = Bukkit.getPlayer(e.getKey());
                Player b = Bukkit.getPlayer(e.getValue());
                if (a != null) a.sendActionBar(bar);
                if (b != null) b.sendActionBar(bar);
            }
        }, 20L, 20L);
    }

    public static String duration(long secs) {
        return (secs / 60) + ":" + String.format("%02d", secs % 60);
    }
}
