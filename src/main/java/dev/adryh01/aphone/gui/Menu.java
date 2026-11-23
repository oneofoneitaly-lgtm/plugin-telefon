package dev.adryh01.aphone.gui;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.menus.HomeMenu;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base phone screen.
 * <p>
 * Phone mode (resource pack + hide-player-inventory): the window is a DISPENSER (its texture is made
 * transparent by the resource pack) and the player's inventory is hidden, so only a narrow,
 * real-phone-shaped screen is visible over the 5 middle columns:
 * <pre>
 *            col 2  3  4  5  6
 *  raw 0..8      [ 3x3 dispenser ]      top of the screen   (x 62..115)
 *  raw 9..35     player inventory rows  middle              (only cols 2-6 are on the phone)
 *  raw 36..44    hotbar                 dock / bottom bar   (only cols 2-6 are on the phone)
 * </pre>
 * Without phone mode a normal 6-row chest is used (bottom bar = row 5).
 *
 * APhone by @Adryh01
 */
public abstract class Menu implements InventoryHolder {

    protected final APhone plugin;
    protected final Player player;
    protected final PhoneData data;
    protected final boolean phoneMode;
    /** number of slots of the top inventory */
    protected final int size;
    protected int page;

    /** Bottom bar slots (raw slot ids). */
    protected final int BACK, PREV, HOME, NEXT, ACTION;
    /** Slots used by paginated lists. */
    private final int[] screen;

    private final Map<Integer, Consumer<ClickType>> actions = new HashMap<>();
    private Inventory inventory;

    protected Menu(APhone plugin, Player player) {
        this.plugin = plugin;
        this.player = player;
        this.data = plugin.data().get(player);
        this.phoneMode = plugin.hider().enabled();
        this.size = phoneMode ? 9 : 54;
        BACK = bar(2);
        PREV = bar(3);
        HOME = bar(4);
        NEXT = bar(5);
        ACTION = bar(6);
        List<Integer> s = new ArrayList<>();
        if (phoneMode) {
            for (int i = 0; i < 9; i++) s.add(i);                       // dispenser 3x3
            for (int r = 0; r < 3; r++) for (int c = 2; c <= 6; c++) s.add(9 + r * 9 + c); // inventory, cols 2-6
        } else {
            for (int i = 0; i < 45; i++) s.add(i);
        }
        screen = s.stream().mapToInt(Integer::intValue).toArray();
    }

    /** Raw slot of column {@code col} (0-8) in the bottom bar. */
    protected int bar(int col) {
        return phoneMode ? 36 + col : 45 + col;
    }

    /** Raw slot of the hidden player inventory, row 0-2, column 0-8 (phone mode). */
    protected int inv(int row, int col) {
        return phoneMode ? 9 + row * 9 + col : 9 + row * 9 + col;
    }

    /** App name (used as plain title without the resource pack). */
    protected abstract String title();

    protected abstract void build();

    /** true only for the home screen (different background). */
    protected boolean isHome() {
        return false;
    }

    public void open() {
        plugin.hider().hide(player);
        var title = Gui.title(plugin, title(), isHome(), clock(player.getWorld()));
        inventory = phoneMode
                ? Bukkit.createInventory(this, InventoryType.DISPENSER, title)
                : Bukkit.createInventory(this, 54, title);
        refresh();
        player.openInventory(inventory);
    }

    /** Redraws the items without reopening. */
    public void refresh() {
        if (inventory == null) return;
        inventory.clear();
        if (plugin.hider().isHidden(player)) plugin.phoneItem().stripUi(player);
        actions.clear();
        build();
        navigation();
    }

    protected void navigation() {
        if (!actions.containsKey(HOME) && !isHome()) {
            set(HOME, Gui.icon(plugin, Icon.HOME, "<white>Home", "<gray>Back to home screen"),
                    c -> new HomeMenu(plugin, player).open());
        }
    }

    protected void backButton(Runnable action) {
        set(BACK, Gui.icon(plugin, Icon.BACK, "<white>Back"), c -> action.run());
    }

    protected void set(int raw, ItemStack item, Consumer<ClickType> action) {
        if (raw < size) {
            inventory.setItem(raw, item);
        } else {
            // player inventory part: only when it is hidden (never overwrite real items)
            if (!plugin.hider().isHidden(player)) return;
            int index = raw < size + 27 ? raw - size + 9 : raw - size - 27;
            player.getInventory().setItem(index, item);
        }
        if (action != null) actions.put(raw, action);
        else actions.remove(raw);
    }

    protected void set(int raw, ItemStack item) {
        set(raw, item, null);
    }

    /** Paginated list on the screen slots. */
    protected <T> void list(List<T> items, Function<T, ItemStack> render, BiConsumer<T, ClickType> onClick) {
        int per = screen.length;
        int pages = Math.max(1, (items.size() + per - 1) / per);
        page = Math.max(0, Math.min(page, pages - 1));
        for (int i = 0; i < per; i++) {
            int idx = page * per + i;
            if (idx >= items.size()) break;
            T t = items.get(idx);
            set(screen[i], render.apply(t), c -> onClick.accept(t, c));
        }
        if (page > 0) {
            set(PREV, Gui.icon(plugin, Icon.PREV, "<white>Previous page"), c -> { page--; refresh(); });
        }
        if (page < pages - 1) {
            set(NEXT, Gui.icon(plugin, Icon.NEXT, "<white>Next page", "<gray>Page " + (page + 1) + "/" + pages),
                    c -> { page++; refresh(); });
        }
    }

    public void handleClick(int raw, ClickType type) {
        Consumer<ClickType> a = actions.get(raw);
        if (a == null) return;
        click();
        a.accept(type);
    }

    protected void click() {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1.6f);
    }

    public static String clock(World w) {
        long t = w.getTime();
        long hours = (t / 1000 + 6) % 24;
        long minutes = (t % 1000) * 60 / 1000;
        return String.format("%02d:%02d", hours, minutes);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
