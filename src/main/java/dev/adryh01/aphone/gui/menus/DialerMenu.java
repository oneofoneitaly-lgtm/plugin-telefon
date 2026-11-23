package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.entity.Player;

/** "Phone" app: contacts + everyone online, click to call. */
public final class DialerMenu extends Menu {

    public DialerMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Phone";
    }

    @Override
    protected void build() {
        data.missedCalls(0);
        list(PickerMenu.reachable(plugin, player), d -> Gui.icon(plugin, Icon.CONTACT,
                        "<white>" + d.name(),
                        "<gray>" + d.number(),
                        data.contacts().contains(d.number()) ? "<gold>★ Contact" : "<dark_gray>Online",
                        "",
                        "<green>Click to call"),
                (d, click) -> {
                    player.closeInventory();
                    plugin.calls().call(player, d);
                });
        backButton(() -> new HomeMenu(plugin, player).open());
        set(ACTION, Gui.icon(plugin, Icon.ADD, "<green><bold>Dial a number", "<gray>Type a number in chat"),
                c -> Actions.askDial(plugin, player));
    }

    @SuppressWarnings("unused")
    private static String label(PhoneData d) {
        return d.name() + " (" + d.number() + ")";
    }
}
