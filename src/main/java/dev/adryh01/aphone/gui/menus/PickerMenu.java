package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Generic "choose a person" screen (contacts first, then online players). */
public final class PickerMenu extends Menu {

    private final String title;
    private final Consumer<PhoneData> onPick;
    private final Runnable back;

    public PickerMenu(APhone plugin, Player player, String title, Consumer<PhoneData> onPick, Runnable back) {
        super(plugin, player);
        this.title = title;
        this.onPick = onPick;
        this.back = back;
    }

    @Override
    protected String title() {
        return title;
    }

    @Override
    protected void build() {
        list(reachable(plugin, player), d -> Gui.icon(plugin, Icon.CONTACT, "<white>" + d.name(),
                        "<gray>" + d.number(), "", "<aqua>Click to select"),
                (d, c) -> onPick.accept(d));
        backButton(back);
    }

    /** Contacts + online players that own a phone number, without duplicates and without yourself. */
    public static List<PhoneData> reachable(APhone plugin, Player player) {
        PhoneData me = plugin.data().get(player);
        Map<String, PhoneData> out = new LinkedHashMap<>();
        for (String n : me.contacts()) {
            PhoneData d = plugin.data().byNumber(n);
            if (d != null) out.put(d.number(), d);
        }
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.equals(player)) continue;
            PhoneData d = plugin.data().get(o);
            out.putIfAbsent(d.number(), d);
        }
        out.remove(me.number());
        return new ArrayList<>(out.values());
    }
}
