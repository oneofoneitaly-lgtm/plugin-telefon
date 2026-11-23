package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.entity.Player;

/** Settings: silent mode, ringtone, phone number. */
public final class SettingsMenu extends Menu {

    public SettingsMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Settings";
    }

    @Override
    protected void build() {
        set(12, Gui.icon(plugin, data.silent() ? Icon.TOGGLE_ON : Icon.TOGGLE_OFF,
                "<white><bold>Silent mode",
                data.silent() ? "<green>ON" : "<red>OFF",
                "<gray>Blocks calls and sounds",
                "",
                "<yellow>Click to toggle"), c -> {
            data.silent(!data.silent());
            plugin.data().save(data);
            plugin.msg().send(player, data.silent() ? "silent-on" : "silent-off");
            refresh();
        });

        set(14, Gui.icon(plugin, Icon.APP_SETTINGS, "<white><bold>Ringtone",
                "<gray>Current: <aqua>" + data.ringtone().pretty(),
                "",
                "<yellow>Left-click <gray>next ringtone",
                "<yellow>Right-click <gray>preview"), c -> {
            if (!c.isRightClick()) {
                data.ringtone(data.ringtone().next());
                plugin.data().save(data);
                plugin.msg().send(player, "ringtone-set", "ringtone", data.ringtone().pretty());
            }
            for (int i = 0; i < 4; i++) {
                final int step = i;
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        player.playSound(player.getLocation(), data.ringtone().sound(), 1f, data.ringtone().pitch(step)), i * 4L);
            }
            refresh();
        });

        set(31, Gui.icon(plugin, Icon.INFO, "<white><bold>About phone",
                "<gray>Number: <aqua>" + data.number(),
                "<gray>Contacts: <white>" + data.contacts().size(),
                "<gray>Messages: <white>" + data.inbox().size(),
                "<gray>Notes: <white>" + data.notes().size(),
                "",
                "<dark_gray>APhone v" + plugin.getPluginMeta().getVersion(),
                "<dark_gray>Made by <aqua>@Adryh01"));
        backButton(() -> new HomeMenu(plugin, player).open());
    }
}
