package dev.adryh01.aphone.gui.menus;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.gui.Gui;
import dev.adryh01.aphone.gui.Icon;
import dev.adryh01.aphone.gui.Menu;
import org.bukkit.World;
import org.bukkit.entity.Player;

/** Weather & clock app. */
public final class WeatherMenu extends Menu {

    public WeatherMenu(APhone plugin, Player player) {
        super(plugin, player);
    }

    @Override
    protected String title() {
        return "Weather";
    }

    @Override
    protected void build() {
        World w = player.getWorld();
        String weather;
        if (w.getEnvironment() != World.Environment.NORMAL) weather = "<dark_purple>No sky here";
        else if (w.isThundering()) weather = "<dark_gray>⛈ Thunderstorm";
        else if (w.hasStorm()) weather = "<blue>☔ Rain";
        else weather = "<yellow>☀ Clear";
        long time = w.getTime();
        boolean day = time < 12300 || time > 23850;

        set(13, Gui.icon(plugin, Icon.APP_WEATHER, "<white><bold>" + HomeMenu.clock(w),
                "<gray>Day <white>" + (w.getFullTime() / 24000 + 1),
                day ? "<yellow>Daytime" : "<dark_aqua>Night"), c -> refresh());
        set(22, Gui.icon(plugin, Icon.INFO, "<white><bold>Forecast",
                weather,
                "<gray>Biome temp: <white>" + String.format("%.1f", player.getLocation().getBlock().getTemperature())));
        backButton(() -> new HomeMenu(plugin, player).open());
    }
}
