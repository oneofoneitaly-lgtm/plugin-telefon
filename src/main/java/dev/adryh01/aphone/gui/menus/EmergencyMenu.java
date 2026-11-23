package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.entity.Player;

/** 911 confirmation screen. */
public final class EmergencyMenu extends Menu {

    public EmergencyMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Emergency";
    }

    @Override
    protected void build() {
        set(13, Gui.icon(plugin, Icon.APP_EMERGENCY, "<dark_red><bold>CALL 911",
                "<gray>Staff will receive your",
                "<gray>name and coordinates.",
                "",
                "<red>Click to confirm"), c -> {
            player.closeInventory();
            Actions.emergency(plugin, player);
        });
        set(31, Gui.icon(plugin, Icon.CALL_END, "<white>Cancel"), c -> new HomeMenu(plugin, player).open());
        backButton(() -> new HomeMenu(plugin, player).open());
    }
}
