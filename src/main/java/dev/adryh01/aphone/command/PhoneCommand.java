package dev.adryh01.aphone.command;

import dev.adryh01.aphone.APhone;
import dev.adryh01.aphone.data.PhoneData;
import dev.adryh01.aphone.gui.Actions;
import dev.adryh01.aphone.gui.menus.HomeMenu;
import dev.adryh01.aphone.util.Msg;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * /aphone                     open the phone
 * /aphone number              show your number
 * /aphone call <number|name>  call someone
 * /aphone sms <number|name> <message...>
 * /aphone accept | decline | hangup
 * /aphone give [player]       (phone.admin)
 * /aphone reload              (phone.admin)
 */
public final class PhoneCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("help", "number", "call", "sms", "accept", "decline", "hangup", "give", "reload");
    private final APhone plugin;

    public PhoneCommand(APhone plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
        String sub = args.length == 0 ? "" : args[0].toLowerCase();

        if (sub.equals("give")) {
            if (!sender.hasPermission("aphone.admin")) return deny(sender);
            Player target = args.length > 1 ? Bukkit.getPlayerExact(args[1]) : (sender instanceof Player p ? p : null);
            if (target == null) {
                plugin.msg().send(sender, "player-not-found", "number", args.length > 1 ? args[1] : "?");
                return true;
            }
            PhoneData d = plugin.data().get(target);
            target.getInventory().addItem(plugin.phoneItem().create(d.number()));
            plugin.msg().send(sender, "phone-given", "player", target.getName());
            return true;
        }
        if (sub.equals("reload")) {
            if (!sender.hasPermission("aphone.admin")) return deny(sender);
            plugin.reload();
            plugin.msg().send(sender, "reloaded");
            return true;
        }

        if (!(sender instanceof Player p)) {
            plugin.msg().send(sender, "players-only");
            return true;
        }
        if (!p.hasPermission("aphone.use")) return deny(p);
        PhoneData me = plugin.data().get(p);

        switch (sub) {
            case "" -> {
                if (!requirePhone(p)) return true;
                new HomeMenu(plugin, p).open();
            }
            case "number" -> plugin.msg().send(p, "your-number", "number", me.number());
            case "accept" -> plugin.calls().accept(p);
            case "decline" -> plugin.calls().decline(p);
            case "hangup", "end" -> plugin.calls().hangup(p);
            case "call" -> {
                if (!requirePhone(p)) return true;
                if (args.length < 2) {
                    p.sendMessage(Msg.MM.deserialize("<red>/aphone call <number|player>"));
                    return true;
                }
                PhoneData t = find(args[1]);
                if (t == null) {
                    plugin.msg().send(p, "player-not-found", "number", args[1]);
                    return true;
                }
                plugin.calls().call(p, t);
            }
            case "sms" -> {
                if (!requirePhone(p)) return true;
                if (args.length < 3) {
                    p.sendMessage(Msg.MM.deserialize("<red>/aphone sms <number|player> <message>"));
                    return true;
                }
                PhoneData t = find(args[1]);
                if (t == null) {
                    plugin.msg().send(p, "player-not-found", "number", args[1]);
                    return true;
                }
                Actions.sendSms(plugin, p, t, String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            }
            default -> help(p);
        }
        return true;
    }

    private PhoneData find(String arg) {
        PhoneData d = plugin.data().byNumber(arg);
        return d != null ? d : plugin.data().byName(arg);
    }

    private boolean requirePhone(Player p) {
        if (plugin.phoneItem().hasPhone(p)) return true;
        plugin.msg().send(p, "need-phone");
        return false;
    }

    private boolean deny(CommandSender s) {
        plugin.msg().send(s, "no-permission");
        return true;
    }

    private void help(CommandSender s) {
        s.sendMessage(Msg.MM.deserialize(String.join("<newline>",
                "<aqua><bold>☎ Phone commands",
                "<white>/aphone <gray>- open your phone",
                "<white>/aphone number <gray>- show your number",
                "<white>/aphone call <number|player> <gray>- call",
                "<white>/aphone sms <number|player> <msg> <gray>- send SMS",
                "<white>/aphone accept <gray>/ <white>decline <gray>/ <white>hangup",
                "<white>/aphone give [player] <gray>- admin",
                "<white>/aphone reload <gray>- admin")));
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : SUBS) if (s.startsWith(args[0].toLowerCase())) out.add(s);
        } else if (args.length == 2 && List.of("call", "sms", "give").contains(args[0].toLowerCase())) {
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (o.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(o.getName());
            }
        }
        return out;
    }
}
