package dev.adryh01.aphone.gui;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.menus.HomeMenu;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Base phone screen.
 * <p>
 * Full-screen mode (resource pack + hide-player-inventory): the window is a 9x5 chest and the
 * player's inventory is hidden, so the phone uses ALL the slots of the window:
 * <pre>
 *  raw 0..44   chest rows 0-4        (top of the screen)
 *  raw 45..71  player inventory rows (middle of the screen)
 *  raw 72..80  hotbar row            (bottom bar / dock)
 * </pre>
 * Without full-screen the bottom bar falls back to chest row 4.
 *
 * APhone by @Adryh01
 */
public abstract class Menu implements InventoryHolder {

    public static final int SIZE = 45;

    protected final APhone plugin;
    protected final Player player;
    protected final PhoneData data;
    protected final boolean fullScreen;
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
        this.fullScreen = plugin.hider().enabled();
        BACK = bar(0);
        PREV = bar(2);
        HOME = bar(4);
        NEXT = bar(6);
        ACTION = bar(8);
        if (fullScreen) {
            screen = new int[72]; // chest 0..44 + player rows 45..71
            for (int i = 0; i < 72; i++) screen[i] = i;
        } else {
            screen = new int[36];
            for (int i = 0; i < 36; i++) screen[i] = i;
        }
    }

    /** Raw slot of column {@code col} in the bottom bar. */
    protected int bar(int col) {
        return fullScreen ? 72 + col : 36 + col;
    }

    /** App name shown in the header. */
    protected abstract String title();

    protected abstract void build();

    /** true only for the home screen (different background). */
    protected boolean isHome() {
        return false;
    }

    public void open() {
        plugin.hider().hide(player);
        inventory = Bukkit.createInventory(this, SIZE, Gui.title(plugin, title(), isHome(), clock(player.getWorld())));
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
        if (raw < SIZE) {
            inventory.setItem(raw, item);
        } else {
            // player inventory part: only when it is hidden (never overwrite real items)
            if (!plugin.hider().isHidden(player)) return;
            int index = raw < 72 ? raw - 45 + 9 : raw - 72;
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
