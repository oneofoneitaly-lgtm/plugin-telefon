package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Notes app: right-click to delete. */
public final class NotesMenu extends Menu {

    public NotesMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Notes";
    }

    @Override
    protected void build() {
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < data.notes().size(); i++) idx.add(i);
        list(idx, i -> {
            String note = data.notes().get(i);
            List<String> lore = new ArrayList<>(Gui.wrap(note, "<white>"));
            lore.add("");
            lore.add("<yellow>Right-click <gray>to delete");
            String head = note.length() > 18 ? note.substring(0, 18) + "..." : note;
            return Gui.icon(plugin, Icon.NOTE, "<yellow>" + dev.adryh01.aphone.util.Msg.esc(head), lore);
        }, (i, click) -> {
            if (click.isRightClick() && i < data.notes().size()) {
                data.notes().remove((int) i);
                plugin.data().save(data);
                refresh();
            }
        });
        backButton(() -> new HomeMenu(plugin, player).open());
        set(ACTION, Gui.icon(plugin, Icon.ADD, "<green><bold>New note", "<gray>Type it in chat"),
                c -> Actions.askNote(plugin, player));
    }
}
