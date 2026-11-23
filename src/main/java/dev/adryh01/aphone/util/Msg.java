package dev.adryh01.aphone.util;

import dev.adryh01.aphone.APhone;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Loads messages.yml and turns MiniMessage strings into components. */
public final class Msg {

    public static final MiniMessage MM = MiniMessage.miniMessage();

    private final APhone plugin;
    private YamlConfiguration cfg;

    public Msg(APhone plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) plugin.saveResource("messages.yml", false);
        cfg = YamlConfiguration.loadConfiguration(file);
        InputStream def = plugin.getResource("messages.yml");
        if (def != null) {
            cfg.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(def, StandardCharsets.UTF_8)));
        }
    }

    /** Raw string with placeholders replaced (values are escaped, so players can't inject tags). */
    public String raw(String key, String... ph) {
        String s = cfg.getString(key, key);
        for (int i = 0; i + 1 < ph.length; i += 2) {
            s = s.replace("%" + ph[i] + "%", MM.escapeTags(ph[i + 1] == null ? "" : ph[i + 1]));
        }
        return s;
    }

    public Component get(String key, String... ph) {
        return MM.deserialize(raw(key, ph));
    }

    public void send(CommandSender to, String key, String... ph) {
        to.sendMessage(MM.deserialize(cfg.getString("prefix", "") + raw(key, ph)));
    }

    public Component prefixed(Component c) {
        return MM.deserialize(cfg.getString("prefix", "")).append(c);
    }

    /** Parse a MiniMessage string as a non-italic item text. */
    public static Component item(String miniMessage) {
        return MM.deserialize("<!italic>" + miniMessage);
    }

    public static String esc(String s) {
        return MM.escapeTags(s == null ? "" : s);
    }
}
