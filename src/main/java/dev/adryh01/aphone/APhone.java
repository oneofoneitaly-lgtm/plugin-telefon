package dev.adryh01.aphone;

import dev.adryh01.aphone.call.CallManager;
import dev.adryh01.aphone.command.PhoneCommand;
import dev.adryh01.aphone.data.DataManager;
import dev.adryh01.aphone.listener.ChatListener;
import dev.adryh01.aphone.listener.MenuListener;
import dev.adryh01.aphone.listener.PhoneListener;
import dev.adryh01.aphone.util.ChatInput;
import dev.adryh01.aphone.util.InventoryHider;
import dev.adryh01.aphone.util.Msg;
import dev.adryh01.aphone.util.PhoneItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * APhone - a full smartphone for Minecraft 1.21.1
 * Made by @Adryh01
 */
public final class APhone extends JavaPlugin {

    public static final String AUTHOR = "@Adryh01";

    private Msg msg;
    private DataManager data;
    private CallManager calls;
    private ChatInput chatInput;
    private PhoneItem phoneItem;
    private InventoryHider hider;
    private NamespacedKey recipeKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        msg = new Msg(this);
        data = new DataManager(this);
        data.loadAll();
        phoneItem = new PhoneItem(this);
        calls = new CallManager(this);
        chatInput = new ChatInput(this);
        hider = new InventoryHider(this);

        Bukkit.getPluginManager().registerEvents(new PhoneListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        PluginCommand cmd = getCommand("aphone");
        if (cmd != null) {
            PhoneCommand executor = new PhoneCommand(this);
            cmd.setExecutor(executor);
            cmd.setTabCompleter(executor);
        }

        registerRecipe();
        calls.startTicker();
        // players already online after a /reload
        Bukkit.getOnlinePlayers().forEach(hider::recover);

        getLogger().info("  _   ___ _");
        getLogger().info(" /_\\ | _ \\ |_  ___ _ _  ___");
        getLogger().info("/ _ \\|  _/ ' \\/ _ \\ ' \\/ -_)");
        getLogger().info("/_/ \\_\\_| |_||_\\___/_||_\\___|  v" + getPluginMeta().getVersion());
        getLogger().info("APhone by " + AUTHOR + " - " + data.size() + " phone numbers loaded.");
    }

    @Override
    public void onDisable() {
        if (hider != null) hider.restoreAll();
        if (calls != null) calls.endAll();
        if (data != null) data.saveAll();
        getLogger().info("APhone by " + AUTHOR + " disabled.");
    }

    public void reload() {
        reloadConfig();
        msg.load();
        registerRecipe();
    }

    private void registerRecipe() {
        recipeKey = new NamespacedKey(this, "aphone");
        Bukkit.removeRecipe(recipeKey);
        if (!getConfig().getBoolean("phone.craftable", true)) return;
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, phoneItem.create(null));
        recipe.shape("IGI", "IRI", "III");
        recipe.setIngredient('I', Material.IRON_INGOT);
        recipe.setIngredient('G', Material.GLASS_PANE);
        recipe.setIngredient('R', Material.REDSTONE);
        Bukkit.addRecipe(recipe);
    }

    public Msg msg() { return msg; }
    public DataManager data() { return data; }
    public CallManager calls() { return calls; }
    public ChatInput chatInput() { return chatInput; }
    public PhoneItem phoneItem() { return phoneItem; }
    public InventoryHider hider() { return hider; }
}
