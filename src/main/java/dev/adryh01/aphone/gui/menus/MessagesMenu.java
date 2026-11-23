package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** SMS inbox: left = reply, right = delete. */
public final class MessagesMenu extends Menu {

    public MessagesMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Messages";
    }

    @Override
    protected void build() {
        if (data.unread() > 0) {
            data.unread(0);
            plugin.data().save(data);
        }
        list(new ArrayList<>(data.inbox()), sms -> {
            List<String> lore = new ArrayList<>();
            lore.add("<dark_gray>" + sms.fromNumber() + " • " + Actions.ago(sms.time()));
            lore.add("");
            lore.addAll(Gui.wrap(sms.text(), "<white>"));
            lore.add("");
            lore.add("<yellow>Left-click <gray>to reply");
            lore.add("<yellow>Right-click <gray>to delete");
            return Gui.icon(plugin, Icon.SMS, "<aqua>" + sms.fromName(), lore);
        }, (sms, click) -> {
            if (click.isRightClick()) {
                data.inbox().remove(sms);
                plugin.data().save(data);
                refresh();
                return;
            }
            PhoneData from = plugin.data().byNumber(sms.fromNumber());
            if (from == null) {
                plugin.msg().send(player, "player-not-found", "number", sms.fromNumber());
                return;
            }
            Actions.askSms(plugin, player, from);
        });
        backButton(() -> new HomeMenu(plugin, player).open());
        set(ACTION, Gui.icon(plugin, Icon.ADD, "<green><bold>New message", "<gray>Choose who to text"),
                c -> new PickerMenu(plugin, player, "New SMS",
                        to -> Actions.askSms(plugin, player, to),
                        () -> new MessagesMenu(plugin, player).open()).open());
    }
}
