package dev.adryh01.aphone.gui;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.util.Msg;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI helpers. The phone is drawn entirely through the inventory title:
 * <ul>
 *   <li>font {@code aphone:gui}: glyph  = full phone texture (176x230, covers the whole 9x4
 *       chest AND the player inventory area) + negative/positive space characters</li>
 *   <li>fonts {@code aphone:status/clock/line1/line2/line3}: the vanilla ascii.png drawn at other heights
 *       and positions, used for the live status-bar clock and the big lock-screen widget</li>
 * </ul>
 * APhone by @Adryh01
 */
public final class Gui {

    public static final Key FONT_GUI = Key.key("aphone", "gui");
    public static final Key FONT_STATUS = Key.key("aphone", "status");
    private static final Key FONT_DEFAULT = Key.key("minecraft", "default");

    /** App screen background (header + navigation bar). */
    public static final String BG_APP = "\uE000";
    /** Home screen background (app labels + dock). */
    public static final String BG_HOME = "\uE001";
    /** both backgrounds are 176 px wide (a 1px almost invisible pixel at x=175) -> advance 177 */
    private static final int BACKGROUND_ADVANCE = 177;

    private Gui() {}

    /** X (GUI px) of the status-bar clock. */
    private static final int STATUS_X = 45;

    /**
     * Builds the inventory title that draws the whole phone.
     * The dispenser screen CENTERS its title, so the components are built to have a total
     * width of exactly 0: the title then starts at x=88 and every position is known.
     * (Only glyphs with exact known widths are used: spaces, the background and the status digits.)
     *
     * @param appName plain title used without the resource pack
     * @param home    true = home background
     * @param status  status bar clock, e.g. "12:30"
     */
    public static Component title(APhone plugin, String appName, boolean home, String status) {
        if (!plugin.getConfig().getBoolean("gui.custom-background", true)) {
            return Component.text("\u260E APhone" + (home ? "" : " \u00BB " + appName));
        }
        String time = status.replaceAll("[^0-9:]", "");
        int x = 88;                                     // title start (centered, total width 0)
        StringBuilder gui = new StringBuilder();
        gui.append(spaces(-x));                         // -> x = 0
        gui.append(home ? BG_HOME : BG_APP);            // phone, advance 177
        x = BACKGROUND_ADVANCE;
        gui.append(spaces(STATUS_X - x));               // -> status bar
        x = STATUS_X;
        Component out = Component.text(gui.toString()).font(FONT_GUI).color(NamedTextColor.WHITE);
        out = out.append(Component.text(time).font(FONT_STATUS).color(NamedTextColor.WHITE));
        x += statusWidth(time);
        out = out.append(Component.text(spaces(88 - x)).font(FONT_GUI)); // back to start: total width 0
        return out;
    }

    /**
     * Space characters defined in assets/aphone/font/gui.json:
     * .. = -1,-2,-4,...,-128   .. = +1,+2,+4,...,+128
     */
    public static String spaces(int px) {
        StringBuilder sb = new StringBuilder();
        char base = px < 0 ? '' : '';
        int left = Math.abs(px);
        for (int bit = 7; bit >= 0; bit--) {
            int v = 1 << bit;
            while (left >= v) {
                sb.append((char) (base + bit));
                left -= v;
            }
        }
        return sb.toString();
    }

    /** Width of the status-bar pixel font (assets/aphone/textures/font/status.png): digits 3px, ':' 1px, +1 spacing. */
    public static int statusWidth(String s) {
        int w = 0;
        for (char c : s.toCharArray()) w += c == ':' ? 2 : 4;
        return w;
    }

    /** Width in GUI pixels of a string in the vanilla font scaled by {@code scale}. */
    public static int width(String s) {
        double scale = 1;
        int w = 0;
        for (char c : s.toCharArray()) {
            if (c == ' ') {
                w += (int) Math.round(4 * scale);
                continue;
            }
            int adv = vanillaAdvance(c);
            w += (int) (0.5 + (adv - 1) * scale) + 1;
        }
        return w;
    }

    private static int vanillaAdvance(char c) {
        switch (c) {
            case '!': case ',': case '.': case ':': case ';': case 'i': case '|': case '\'':
                return 2;
            case 'l': case '`':
                return 3;
            case 'I': case '[': case ']': case 't':
                return 4;
            case '"': case '(': case ')': case '*': case '<': case '>': case 'f': case 'k': case '{': case '}':
                return 5;
            case '@': case '~':
                return 7;
            case '•':
                return 3;
            default:
                return 6;
        }
    }

    // ---------------------------------------------------------------- items

    public static ItemStack icon(APhone plugin, Icon icon, String name, List<String> lore) {
        boolean pack = plugin.getConfig().getBoolean("gui.custom-background", true);
        ItemStack item = new ItemStack(pack ? Material.PAPER : icon.fallback());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Msg.item(name));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(Msg.item(s));
        meta.lore(l);
        if (pack) meta.setCustomModelData(icon.modelData());
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        plugin.phoneItem().markUi(item);
        return item;
    }

    public static ItemStack icon(APhone plugin, Icon icon, String name, String... lore) {
        return icon(plugin, icon, name, List.of(lore));
    }

    /** Wraps a long text into lore lines of ~28 chars. */
    public static List<String> wrap(String text, String color) {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() + word.length() > 28 && !line.isEmpty()) {
                out.add(color + Msg.esc(line.toString()));
                line.setLength(0);
            }
            if (!line.isEmpty()) line.append(' ');
            line.append(word);
        }
        if (!line.isEmpty()) out.add(color + Msg.esc(line.toString()));
        return out;
    }
}
