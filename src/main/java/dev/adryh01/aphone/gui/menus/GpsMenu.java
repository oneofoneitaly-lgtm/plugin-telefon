package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/** Maps app: shows coordinates, biome, facing; share location with someone. */
public final class GpsMenu extends Menu {

    public GpsMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Maps";
    }

    @Override
    protected void build() {
        Location l = player.getLocation();
        String biome = l.getBlock().getBiome().getKey().getKey().replace('_', ' ');
        set(13, Gui.icon(plugin, Icon.LOCATION, "<blue><bold>Your location",
                "<gray>X: <white>" + l.getBlockX(),
                "<gray>Y: <white>" + l.getBlockY(),
                "<gray>Z: <white>" + l.getBlockZ(),
                "<gray>World: <white>" + l.getWorld().getName(),
                "<gray>Biome: <white>" + biome,
                "<gray>Facing: <white>" + player.getFacing().name().toLowerCase(),
                "",
                "<yellow>Click <gray>to refresh"), c -> refresh());

        set(22, Gui.icon(plugin, Icon.APP_GPS, "<green><bold>Share location", "<gray>Send your position to someone"),
                c -> new PickerMenu(plugin, player, "Share location", to -> {
                    Actions.shareLocation(plugin, player, to);
                    player.closeInventory();
                }, () -> new GpsMenu(plugin, player).open()).open());

        set(31, Gui.icon(plugin, Icon.INFO, "<white>Print in chat", "<gray>Copy your coordinates"), c -> {
            player.closeInventory();
            plugin.msg().send(player, "gps-location", "x", String.valueOf(l.getBlockX()),
                    "y", String.valueOf(l.getBlockY()), "z", String.valueOf(l.getBlockZ()),
                    "world", l.getWorld().getName());
        });
        backButton(() -> new HomeMenu(plugin, player).open());
    }
}
