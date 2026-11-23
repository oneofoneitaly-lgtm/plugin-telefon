package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.ArrayList;
import java.util.List;

/** Contacts app: left = call, right = SMS, shift+right = delete. */
public final class ContactsMenu extends Menu {

    public ContactsMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Contacts";
    }

    @Override
    protected void build() {
        List<PhoneData> contacts = new ArrayList<>();
        for (String n : data.contacts()) {
            PhoneData d = plugin.data().byNumber(n);
            if (d != null) contacts.add(d);
        }
        list(contacts, d -> Gui.icon(plugin, Icon.CONTACT, "<white>" + d.name(),
                        "<gray>" + d.number(),
                        Bukkit.getPlayer(d.uuid()) != null ? "<green>● Online" : "<red>● Offline",
                        "",
                        "<yellow>Left-click <gray>to call",
                        "<yellow>Right-click <gray>to send SMS",
                        "<yellow>Shift+Right <gray>to delete"),
                (d, click) -> {
                    if (click == ClickType.SHIFT_RIGHT) {
                        data.contacts().remove(d.number());
                        plugin.data().save(data);
                        plugin.msg().send(player, "contact-removed", "name", d.name());
                        refresh();
                    } else if (click.isRightClick()) {
                        Actions.askSms(plugin, player, d);
                    } else {
                        player.closeInventory();
                        plugin.calls().call(player, d);
                    }
                });
        backButton(() -> new HomeMenu(plugin, player).open());
        set(ACTION, Gui.icon(plugin, Icon.ADD, "<green><bold>Add contact", "<gray>Type a number or name in chat"),
                c -> Actions.askAddContact(plugin, player));
    }
}
