package top.plutomc.waypoint;

import cc.carm.lib.easysql.EasySQL;
import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.api.enums.NumberType;
import cc.carm.lib.easysql.hikari.HikariConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ChatArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import me.nostal853.framework.menu.MenuFramework;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.Website;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.bukkit.plugin.java.annotation.plugin.author.Authors;
import top.plutomc.waypoint.utils.GUIBuilder;
import top.plutomc.waypoint.utils.Waypoint;
import top.plutomc.waypoint.utils.WaypointHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Plugin(name = "waypoint", version = "1.00.0")
@ApiVersion(ApiVersion.Target.v1_19)
@Authors({
        @Author("PlutoMC"),
        @Author("DeeChael")
})
@Website("plutomc.top")
@Description("Waypoint plugin allows player to upload waypoints")
public final class WaypointPlugin extends JavaPlugin {

    public static final HikariConfig HIKARI_CONFIG = new HikariConfig();
    private static SQLManager sqlManager;
    private static Connection connection;

    @Override
    public void onEnable() {
        // Initialize config
        this.reloadConfig();

        if (isFirstTime()) {
            this.setDefaults();
            this.getLogger().warning("""
                    
                             -----------------------------------
                             | !!!!!!!!!!!ATTENTION!!!!!!!!!!! |
                             -----------------------------------
                             | Detected that this is the first |
                             | time you start with this plugin |
                             | Please set up mysql in config f |
                             | -ile then reload the plugin!    |
                             -----------------------------------
                             |                    PlutoMC Team |
                             -----------------------------------
                    """);
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        new MenuFramework(this); // 注册菜单框架。

        // Register permission
        Bukkit.getPluginManager().addPermission(new Permission("plutomc.waypoint.admin", PermissionDefault.OP));
        Bukkit.getPluginManager().addPermission(new Permission("plutomc.waypoint.command.waypoint.gui", PermissionDefault.TRUE));
        Bukkit.getPluginManager().addPermission(new Permission("plutomc.waypoint.command.waypoint.upload", PermissionDefault.TRUE));
        Bukkit.getPluginManager().addPermission(new Permission("plutomc.waypoint.command.waypoint.help", PermissionDefault.TRUE));

        // Setting up mysql
        HIKARI_CONFIG.setDriverClassName("com.mysql.cj.jdbc.Driver");
        HIKARI_CONFIG.setJdbcUrl("jdbc:mysql://" + Config.string("storage.mysql.host") + ":" + Config.integer("storage.mysql.port") + "/");
        HIKARI_CONFIG.setUsername(Config.string("storage.mysql.username"));
        HIKARI_CONFIG.setPassword(Config.string("storage.mysql.password"));
        sqlManager = EasySQL.createManager(HIKARI_CONFIG);
        try {
            connection = sqlManager.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        sqlManager.executeSQLBatch("use " + Config.string("storage.mysql.database") + ";");

        try {
            // The table to tore waypoints
            sqlManager.createTable(Config.string("storage.mysql.table_prefix") + "waypoints")
                    .addAutoIncrementColumn("WAYPOINT_ID", NumberType.BIGINT, true, true)
                    .addColumn("CREATOR_UUID", "TEXT")
                    .addColumn("WAYPOINT_NAME", "TEXT")
                    .addColumn("WAYPOINT_ICON", "TEXT")
                    .addColumn("WAYPOINT_INFO", "LONGTEXT")
                    .addColumn("SIGN", "TEXT")
                    .addColumn("CREATE_DATE", "TEXT")
                    .build()
                    .execute();
        } catch (SQLException e) {
            getLogger().severe("Failed to create table!");
        }

        // Register command
        new CommandAPICommand("waypoint")
                .withSubcommand(new CommandAPICommand("gui")
                        .withPermission("plutomc.waypoint.command.waypoint.gui")
                        .executesPlayer((player, args) -> {
                            if (player.hasPermission("plutomc.waypoint.admin")) {
                                GUIBuilder.openAdmin(player, 1);
                            } else {
                                GUIBuilder.open(player, 1);
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("open-with-id-directly")
                        .withPermission("plutomc.waypoint.command.waypoint.gui")
                        .withArguments(new LongArgument("id"))
                        .executesPlayer((player, args) -> {
                            if (!WaypointHelper.containsId((long) args[0])) {
                                player.sendMessage(
                                        MiniMessage
                                                .miniMessage()
                                                .deserialize("<red>该路径点不存在")
                                );
                                return;
                            }
                            Waypoint waypoint = WaypointHelper.findById((Long) args[0]);
                            if (player.hasPermission("plutomc.waypoint.admin") || player.getUniqueId() == waypoint.creator()) {
                                GUIBuilder.openInfoCreatorOrAdmin(player, waypoint, 1);
                            } else {
                                GUIBuilder.openInfo(player, waypoint, 1);
                            }
                        })
                )
                .withSubcommand(new CommandAPICommand("show-waypoint-information")
                        .withArguments(new LongArgument("id"))
                        .executesPlayer((player, args) -> {
                            if (!WaypointHelper.containsId((long) args[0])) {
                                player.sendMessage(
                                        MiniMessage
                                                .miniMessage()
                                                .deserialize("<red>该路径点不存在")
                                );
                                return;
                            }
                            Waypoint waypoint = WaypointHelper.findById((Long) args[0]);
                            // TODO
                        })
                )
                .withSubcommand(new CommandAPICommand("upload")
                        .withPermission("plutomc.waypoint.command.waypoint.upload")
                        .withArguments(new StringArgument("name"))
                        .executesPlayer((player, args) -> {
                            Location location = player.getLocation();
                            long id = WaypointHelper.create((String) args[0],
                                    Material.PAPER,
                                    player.getUniqueId(),
                                    location.getWorld().getName(),
                                    location.getBlockX(),
                                    location.getBlockY(),
                                    location.getBlockZ()).id();
                            player.sendMessage(
                                    MiniMessage
                                            .miniMessage()
                                            .deserialize("<green>成功创建路径点！<hover:show_text:\"<yellow>点击打开GUI进行查看\"><click:run_command:\"/waypoint open-with-id-directly " + id + "\"><aqua><bold>[点击查看]</click></hover>")
                            );
                        })
                )
                .withSubcommand(new CommandAPICommand("list")
                        .withArguments(new IntegerArgument("page", 1))
                        .executes((sender, args) -> {
                            // Page Limit: 20 waypoints per page
                            int currentPage = args.length > 0 ? (int) args[0] : 1;
                            List<Waypoint> waypoints = WaypointHelper.findAll();
                            int totalPage = waypoints.size() / 20 == 0 ? waypoints.size() / 20 : (waypoints.size() / 20) + 1;
                            if (currentPage > totalPage)
                                currentPage = totalPage;
                            // TODO
                            // 输出内容的格式:
                            // ==========[当前页码/总页码]==========
                            // [路径点名称] [路径点名称] [路径点名称]
                            // [路径点名称] [路径点名称] [路径点名称]
                            // [路径点名称] [路径点名称] [路径点名称]
                            // ================================== (根据页码的数字的位数自动补，比如一位数不补，两位数就再加一个=，以此类推
                            //
                            // [路径点名称]是可以点击的文本, 点击后输出文本
                            // waypoint.icon()回返回一个Bukkit的Material，Paper在ItemStack类里添加了方法可以将ItemStack转化为HoverEvent，
                            // 你将每一个[路径点名称]的HoverEvent设置为显示其对应路径点的图标
                            //
                            // 点击后输出文本实现方法: 每一个路径点都有一个独一无二的数字id, 然后将点击事件设置为执行指令: /waypoint show-waypoint-information id即可
                            //
                            // 点击后输出:
                            /* 路径点: waypoint.name()
                             * - <green>创建者: <yellow>waypoint.creator()
                             * - <green>创建日期: <yellow>waypoint.date()
                             * - <green>维度: <yellow>dimension(waypoint.world())
                             * - <green>坐标: <yellow>waypoint.x()<white>, <yellow>waypoint.y()<white>, <yellow>waypoint.z()
                             *
                             * waypoint.icon()回返回一个Bukkit的Material，Paper在ItemStack类里添加了方法可以将ItemStack转化为HoverEvent，
                             * 你将waypoint.name()的HoverEvent设置为显示该路径点的图标
                             */

                        })
                )
                .withSubcommand(new CommandAPICommand("help")
                        .withPermission("plutomc.waypoint.command.waypoint.help")
                        .withArguments(new ChatArgument("name"))
                        .executesPlayer((player, args) -> {
                            player.sendMessage(MiniMessage
                                    .miniMessage()
                                    .deserialize(
                                            """
                                                    <red>===============================
                                                    <white>/waypoint help - 获取帮助
                                                    <white>/waypoint gui - 打开GUI
                                                    <white>/waypoint list [page] - 列出路径点
                                                    <white>/waypoint upload <名称> - 创建一个路径点
                                                    <gold>提示：<>中表示必填，[]中表示选填，{a|b|c}表示选择
                                                    <red>==============================="""));
                        })
                )
                .register();

        // Register events listener
        // TODO
    }

    @Override
    public void onDisable() {
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static WaypointPlugin getInstance() {
        return JavaPlugin.getPlugin(WaypointPlugin.class);
    }

    public static SQLManager getSqlManager() {
        return sqlManager;
    }

    public static Connection getConnection() {
        return connection;
    }

    private boolean isFirstTime() {
        return !(this.getConfig().contains("storage.mysql.host") &&
                this.getConfig().contains("storage.mysql.port") &&
                this.getConfig().contains("storage.mysql.username") &&
                this.getConfig().contains("storage.mysql.password") &&
                this.getConfig().contains("storage.mysql.database") &&
                this.getConfig().contains("storage.mysql.table_prefix")
                );
    }

    private void setDefaults() {
        setDefault("storage.mysql.host", "localhost");
        setDefault("storage.mysql.port", 3306);
        setDefault("storage.mysql.username", "root");
        setDefault("storage.mysql.password", "123456");
        setDefault("storage.mysql.database", "minecraft");
        setDefault("storage.mysql.table-prefix", "waypoint_");
        setDefault("waypoint.allowed-icons", Arrays.asList("DIAMOND", "PAPER"));
        this.saveConfig();
    }

    private void setDefault(String path, Object value) {
        if (!this.getConfig().contains(path))
            this.getConfig().set(path, value);
    }

    public static class Config {

        public static String string(String path) {
            return WaypointPlugin.getInstance().getConfig().getString(path);
        }

        public static int integer(String path) {
            return WaypointPlugin.getInstance().getConfig().getInt(path);
        }

        public static int bool(String path) {
            return WaypointPlugin.getInstance().getConfig().getInt(path);
        }

        public static double double0(String path) {
            return WaypointPlugin.getInstance().getConfig().getDouble(path);
        }

        public static List<String> list(String path) {
            return WaypointPlugin.getInstance().getConfig().getStringList(path);
        }

    }

}
