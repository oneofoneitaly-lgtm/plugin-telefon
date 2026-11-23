package dev.adryh01.aphone.data;

import dev.adryh01.aphone.APhone;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Stores one YAML file per player in plugins/APhone/players/. */
public final class DataManager {

    private final APhone plugin;
    private final File folder;
    private final Map<UUID, PhoneData> byUuid = new ConcurrentHashMap<>();
    private final Map<String, UUID> byNumber = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public DataManager(APhone plugin) {
        this.plugin = plugin;
        this.folder = new File(plugin.getDataFolder(), "players");
        if (!folder.exists() && !folder.mkdirs()) {
            plugin.getLogger().warning("Could not create players folder");
        }
    }

    public int size() { return byUuid.size(); }

    public void loadAll() {
        File[] files = folder.listFiles((d, n) -> n.endsWith(".yml"));
        if (files == null) return;
        for (File f : files) {
            try {
                UUID id = UUID.fromString(f.getName().replace(".yml", ""));
                PhoneData d = load(id, YamlConfiguration.loadConfiguration(f));
                byUuid.put(id, d);
                byNumber.put(d.number(), id);
            } catch (Exception e) {
                plugin.getLogger().warning("Skipping bad data file " + f.getName() + ": " + e.getMessage());
            }
        }
    }

    private PhoneData load(UUID id, YamlConfiguration y) {
        PhoneData d = new PhoneData(id, y.getString("name", "Unknown"), y.getString("number", generateNumber()));
        d.contacts().addAll(y.getStringList("contacts"));
        d.notes().addAll(y.getStringList("notes"));
        d.silent(y.getBoolean("silent"));
        d.flashlight(y.getBoolean("flashlight"));
        d.ringtone(Ringtone.parse(y.getString("ringtone", "CLASSIC")));
        d.receivedPhone(y.getBoolean("received-phone"));
        d.unread(y.getInt("unread"));
        d.missedCalls(y.getInt("missed-calls"));
        d.lastEmergency(y.getLong("last-emergency"));
        ConfigurationSection inbox = y.getConfigurationSection("inbox");
        if (inbox != null) {
            for (String k : inbox.getKeys(false)) {
                ConfigurationSection s = inbox.getConfigurationSection(k);
                if (s == null) continue;
                d.inbox().add(new PhoneData.Sms(s.getString("from-number", "?"), s.getString("from-name", "?"),
                        s.getString("text", ""), s.getLong("time")));
            }
        }
        return d;
    }

    public void save(PhoneData d) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", d.name());
        y.set("number", d.number());
        y.set("contacts", new ArrayList<>(d.contacts()));
        y.set("notes", d.notes());
        y.set("silent", d.silent());
        y.set("flashlight", d.flashlight());
        y.set("ringtone", d.ringtone().name());
        y.set("received-phone", d.receivedPhone());
        y.set("unread", d.unread());
        y.set("missed-calls", d.missedCalls());
        y.set("last-emergency", d.lastEmergency());
        List<PhoneData.Sms> inbox = d.inbox();
        for (int i = 0; i < inbox.size(); i++) {
            PhoneData.Sms s = inbox.get(i);
            String p = "inbox." + i + ".";
            y.set(p + "from-number", s.fromNumber());
            y.set(p + "from-name", s.fromName());
            y.set(p + "text", s.text());
            y.set(p + "time", s.time());
        }
        try {
            y.save(new File(folder, d.uuid() + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Could not save phone data of " + d.name() + ": " + e.getMessage());
        }
    }

    public void saveAll() {
        byUuid.values().forEach(this::save);
    }

    /** Returns the player's phone data, creating a new number on first use. */
    public PhoneData get(Player p) {
        PhoneData d = byUuid.computeIfAbsent(p.getUniqueId(), id -> {
            PhoneData nd = new PhoneData(id, p.getName(), generateNumber());
            byNumber.put(nd.number(), id);
            return nd;
        });
        if (!p.getName().equals(d.name())) d.name(p.getName());
        return d;
    }

    public PhoneData get(UUID id) {
        return byUuid.get(id);
    }

    public PhoneData byNumber(String number) {
        String n = normalize(number);
        if (n == null) return null;
        UUID id = byNumber.get(n);
        return id == null ? null : byUuid.get(id);
    }

    public PhoneData byName(String name) {
        for (PhoneData d : byUuid.values()) {
            if (d.name().equalsIgnoreCase(name)) return d;
        }
        return null;
    }

    /** Accepts "5551234", "555-1234" or just "1234" and returns "555-1234". */
    public String normalize(String input) {
        if (input == null) return null;
        String digits = input.replaceAll("[^0-9]", "");
        String prefix = plugin.getConfig().getString("number-prefix", "555");
        if (digits.length() == 4) return prefix + "-" + digits;
        if (digits.length() == prefix.length() + 4) {
            return digits.substring(0, prefix.length()) + "-" + digits.substring(prefix.length());
        }
        return null;
    }

    private String generateNumber() {
        String prefix = plugin.getConfig().getString("number-prefix", "555");
        String n;
        do {
            n = prefix + "-" + String.format("%04d", random.nextInt(10000));
        } while (byNumber.containsKey(n));
        return n;
    }
}
