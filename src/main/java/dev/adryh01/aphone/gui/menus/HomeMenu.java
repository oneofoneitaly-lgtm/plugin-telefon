package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Home screen, like a real phone (phone mode = narrow dispenser window, columns 2-6):
 * <pre>
 *   dispenser row 0:      Maps        Weather          raw 0, 2
 *   dispenser row 2:      Notes       Calc             raw 6, 8
 *   inventory row 0:   Light   Settings    911        raw 11, 13, 15
 *   dock (hotbar):     Phone  Messages  Contacts       raw 38, 40, 42 (no labels)
 *   call controls: inventory row 2 (raw 30 / 31 / 32)
 * </pre>
 * Labels are drawn in the texture: positions MUST match tools/generate_pack.py (HOME_LABELS).
 *
 * APhone by @Adryh01
 */
public final class HomeMenu extends Menu {

    public HomeMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Home";
    }

    @Override
    protected boolean isHome() {
        return true;
    }

    @Override
    protected void build() {
        String unread = data.unread() > 0 ? " <red>(" + data.unread() + ")" : "";
        String missed = data.missedCalls() > 0 ? " <red>(" + data.missedCalls() + ")" : "";

        // app grid
        set(0, Gui.icon(plugin, Icon.APP_GPS, "<blue><bold>Maps", "<gray>Your location, share it"),
                c -> new GpsMenu(plugin, player).open());
        set(2, Gui.icon(plugin, Icon.APP_WEATHER, "<white><bold>Weather", "<gray>Time, weather and day"),
                c -> new WeatherMenu(plugin, player).open());
        set(6, Gui.icon(plugin, Icon.APP_NOTES, "<yellow><bold>Notes", "<gray>Write personal notes"),
                c -> new NotesMenu(plugin, player).open());
        set(8, Gui.icon(plugin, Icon.APP_CALCULATOR, "<gray><bold>Calculator", "<gray>Solve math in chat"),
                c -> Actions.askCalculator(plugin, player));
        set(inv(0, 2), Gui.icon(plugin, Icon.APP_FLASHLIGHT, "<yellow><bold>Flashlight",
                        data.flashlight() ? "<green>ON <gray>- click to turn off" : "<red>OFF <gray>- click to turn on"),
                c -> { Actions.toggleFlashlight(plugin, player); refresh(); });
        set(inv(0, 6), Gui.icon(plugin, Icon.APP_EMERGENCY, "<red><bold>911", "<gray>Alert emergency services"),
                c -> new EmergencyMenu(plugin, player).open());
        set(inv(0, 4), Gui.icon(plugin, Icon.APP_SETTINGS, "<white><bold>Settings",
                        "<gray>Number: <aqua>" + data.number(),
                        "<gray>Silent mode, ringtone",
                        "",
                        "<dark_gray>APhone by @Adryh01"),
                c -> new SettingsMenu(plugin, player).open());

        // dock
        set(bar(2), Gui.icon(plugin, Icon.APP_PHONE, "<green><bold>Phone" + missed, "<gray>Call players and contacts"),
                c -> new DialerMenu(plugin, player).open());
        set(bar(4), Gui.icon(plugin, Icon.APP_MESSAGES, "<aqua><bold>Messages" + unread, "<gray>Read and send SMS"),
                c -> new MessagesMenu(plugin, player).open());
        set(bar(6), Gui.icon(plugin, Icon.APP_CONTACTS, "<gold><bold>Contacts", "<gray>Your saved numbers"),
                c -> new ContactsMenu(plugin, player).open());

        // call controls (inventory row 2, above the dock)
        var id = player.getUniqueId();
        if (plugin.calls().isRinging(id)) {
            set(inv(2, 3), Gui.icon(plugin, Icon.CALL_ACCEPT, "<green><bold>Accept call"),
                    c -> { plugin.calls().accept(player); player.closeInventory(); });
            set(inv(2, 5), Gui.icon(plugin, Icon.CALL_END, "<red><bold>Decline"),
                    c -> { plugin.calls().decline(player); refresh(); });
        } else if (plugin.calls().inCall(id) || plugin.calls().isCalling(id)) {
            set(inv(2, 4), Gui.icon(plugin, Icon.CALL_END, "<red><bold>Hang up"),
                    c -> { plugin.calls().hangup(player); refresh(); });
        }
    }

    /** kept for other classes */
    public static String clock(World w) {
        return Menu.clock(w);
    }
}
