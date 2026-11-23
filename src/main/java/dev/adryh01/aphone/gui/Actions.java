package dev.adryh01.aphone.gui;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.util.Calculator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/** Phone features shared by menus and commands. */
public final class Actions {

    private Actions() {}

    public static void askSms(APhone plugin, Player from, PhoneData to) {
        plugin.chatInput().ask(from, "sms-type", text -> sendSms(plugin, from, to, text), "name", to.name());
    }

    public static void sendSms(APhone plugin, Player from, PhoneData to, String text) {
        PhoneData me = plugin.data().get(from);
        if (text.isBlank()) return;
        if (text.length() > 200) text = text.substring(0, 200);
        to.inbox().add(0, new PhoneData.Sms(me.number(), me.name(), text, System.currentTimeMillis()));
        int max = plugin.getConfig().getInt("sms.inbox-size", 45);
        while (to.inbox().size() > max) to.inbox().remove(to.inbox().size() - 1);
        to.unread(to.unread() + 1);
        plugin.data().save(to);
        plugin.msg().send(from, "sms-sent", "name", to.name());
        Player t = Bukkit.getPlayer(to.uuid());
        if (t != null) {
            plugin.msg().send(t, "sms-received", "name", me.name(), "message", text);
            if (!to.silent()) t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 2f);
        }
    }

    public static void askAddContact(APhone plugin, Player p) {
        plugin.chatInput().ask(p, "contact-type-number", input -> {
            PhoneData target = plugin.data().byNumber(input);
            if (target == null) target = plugin.data().byName(input);
            if (target == null) {
                plugin.msg().send(p, "player-not-found", "number", input);
                return;
            }
            PhoneData me = plugin.data().get(p);
            if (!me.contacts().add(target.number())) {
                plugin.msg().send(p, "contact-exists");
                return;
            }
            plugin.data().save(me);
            plugin.msg().send(p, "contact-added", "name", target.name());
        });
    }

    public static void askDial(APhone plugin, Player p) {
        plugin.chatInput().ask(p, "contact-type-number", input -> {
            PhoneData target = plugin.data().byNumber(input);
            if (target == null) {
                plugin.msg().send(p, "player-not-found", "number", input);
                return;
            }
            plugin.calls().call(p, target);
        });
    }

    public static void shareLocation(APhone plugin, Player from, PhoneData to) {
        Location l = from.getLocation();
        plugin.msg().send(from, "gps-shared", "name", to.name());
        Player t = Bukkit.getPlayer(to.uuid());
        if (t == null) {
            sendSms(plugin, from, to, "My location: X " + l.getBlockX() + " Y " + l.getBlockY() + " Z " + l.getBlockZ()
                    + " (" + l.getWorld().getName() + ")");
            return;
        }
        plugin.msg().send(t, "gps-received", "name", from.getName(),
                "x", String.valueOf(l.getBlockX()), "y", String.valueOf(l.getBlockY()),
                "z", String.valueOf(l.getBlockZ()), "world", l.getWorld().getName());
        t.playSound(t.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.5f);
    }

    public static void toggleFlashlight(APhone plugin, Player p) {
        PhoneData d = plugin.data().get(p);
        d.flashlight(!d.flashlight());
        applyFlashlight(plugin, p);
        plugin.msg().send(p, d.flashlight() ? "flashlight-on" : "flashlight-off");
        p.playSound(p.getLocation(), Sound.BLOCK_LEVER_CLICK, 0.6f, d.flashlight() ? 1.4f : 0.8f);
    }

    public static void applyFlashlight(APhone plugin, Player p) {
        PhoneData d = plugin.data().get(p);
        if (d.flashlight() && plugin.getConfig().getBoolean("flashlight.enabled", true)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 0, false, false, true));
        } else if (p.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            PotionEffect e = p.getPotionEffect(PotionEffectType.NIGHT_VISION);
            if (e != null && e.isInfinite()) p.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    public static void emergency(APhone plugin, Player p) {
        PhoneData d = plugin.data().get(p);
        long cd = plugin.getConfig().getLong("emergency.cooldown-seconds", 60) * 1000;
        long left = d.lastEmergency() + cd - System.currentTimeMillis();
        if (left > 0) {
            plugin.msg().send(p, "emergency-cooldown", "seconds", String.valueOf(left / 1000 + 1));
            return;
        }
        d.lastEmergency(System.currentTimeMillis());
        Location l = p.getLocation();
        String[] ph = {"name", p.getName(), "x", String.valueOf(l.getBlockX()), "y", String.valueOf(l.getBlockY()),
                "z", String.valueOf(l.getBlockZ()), "world", l.getWorld().getName()};
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.hasPermission("aphone.emergency.receive")) {
                plugin.msg().send(o, "emergency-alert", ph);
                o.playSound(o.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1f);
            }
        }
        plugin.msg().send(Bukkit.getConsoleSender(), "emergency-alert", ph);
        plugin.msg().send(p, "emergency-sent");
    }

    public static void askCalculator(APhone plugin, Player p) {
        plugin.chatInput().ask(p, "calc-type", expr -> {
            try {
                plugin.msg().send(p, "calc-result", "expr", expr, "result", Calculator.format(Calculator.eval(expr)));
            } catch (RuntimeException e) {
                plugin.msg().send(p, "calc-error");
            }
        });
    }

    public static void askNote(APhone plugin, Player p) {
        plugin.chatInput().ask(p, "note-type", text -> {
            PhoneData d = plugin.data().get(p);
            d.notes().add(0, text.length() > 200 ? text.substring(0, 200) : text);
            plugin.data().save(d);
            plugin.msg().send(p, "note-saved");
        });
    }

    public static String ago(long time) {
        long s = (System.currentTimeMillis() - time) / 1000;
        if (s < 60) return "just now";
        if (s < 3600) return (s / 60) + "m ago";
        if (s < 86400) return (s / 3600) + "h ago";
        return (s / 86400) + "d ago";
    }
}
