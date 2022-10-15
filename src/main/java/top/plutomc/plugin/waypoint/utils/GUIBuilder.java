package top.plutomc.plugin.waypoint.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GUIBuilder {

    /*
     * GUI 格式介绍
     *
     * 当玩家输入/waypoint gui时，如果玩家有权限plutomc.waypoint.admin则会调用openAdmin方法开启管理员菜单，否则调用open方法开启普通玩家菜单
     * 区别仅有物品的区别而已
     *
     * 菜单格式
     *     Title: | 路径点列表 |
     *     当waypoint数量小于等于28个（一页）时
     *         #########
     *         #       #
     *         #       #
     *         #       #
     *         #       #
     *         ###Q#C###
     *     当waypoint数量大于28个（超过一页）时
     *         第一页
     *             #########
     *             #       #
     *             #       #
     *             #       #
     *             #       #
     *             ###Q#C##>
     *         中间任意一页
     *             #########
     *             #       #
     *             #       #
     *             #       #
     *             #       #
     *             <##Q#C##>
     *         最后一页
     *             #########
     *             #       #
     *             #       #
     *             #       #
     *             #       #
     *             <##Q#C###
     *     物品Pattern:
     *         #: 边框物品 [BLACK_STAINED_GLASS_PANE]
     *         <: 上一页 [ARROW]
     *         >: 下一页 [ARROW]
     *         Q: 搜索按钮 [OAK_SIGN]
     *         C: createWaypoint(Player, backPage) [DIAMOND]
     *     普通玩家菜单内路径点物品格式:
     *         DisplayName: 支持Adventure格式，直接使用玩家设置的路径点名称
     *         Type: waypoint.icon()
     *         Lore:
     *           - <green>创建者: <yellow>waypoint.creator()
     *           - <green>创建日期: <yellow>waypoint.date()
     *           - <green>维度: <yellow>dimension(waypoint.world())
     *           - <green>坐标: <yellow>waypoint.x()<white>, <yellow>waypoint.y()<white>, <yellow>waypoint.z()
     *           - <newline>
     *           - <aqua><bold>[左键] 查看详细
     *         Click:
     *             Left: openInfo(Player, Waypoint, int)
     *     管理员菜单内路径点物品格式:
     *         DisplayName: 支持Adventure格式，直接使用玩家设置的路径点名称
     *         Type: waypoint.icon()
     *         Lore:
     *           - <green>创建者: <yellow>waypoint.creator()
     *           - <green>创建日期: <yellow>waypoint.date()
     *           - <green>维度: <yellow>dimension(waypoint.world())
     *           - <green>坐标: <yellow>waypoint.x()<white>, <yellow>waypoint.y()<white>, <yellow>waypoint.z()
     *           - <newline>
     *           - <aqua><bold>[左键] 查看详细
     *           - <red><bold>[右键] 删除路径点
     *         Click:
     *             Left: openInfo(Player, Waypoint, int)
     *             Right: deleteConfirm(Player, Waypoint, int)
     *     路径点详细页格式:
     *         Title: | waypoint.name() |
     *         普通用户格式:
     *             R########
     *             #A##B##C#
     *             #########
     *         创建者或管理员格式:
     *             R########
     *             #A##B##C#
     *             #########
     *             #D##E##F#
     *             #########
     *         Patterns:
     *             R: 开启菜单: open(Player, int) or openAdmin(Player, int) [ARROW]
     *             A: 显示信息:
     *                   Type: waypoint.icon()
     *                   DisplayName: 路径点图标
     *             B: 显示信息:
     *                   Type: waypoint.creator()玩家的头颅
     *                   DisplayName: 创建者
     *             C: 显示信息:
     *                   Type: COMPASS
     *                   DisplayName: 位置
     *                   Lore:
     *                     - <green>维度: <yellow>dimension(waypoint.world())
     *                     - <green>坐标: <yellow>waypoint.x()<white>, <yellow>waypoint.y()<white>, <yellow>waypoint.z()
     *             D: 显示信息:
     *                   Type: 海龟蛋
     *                   DisplayName: 修改图标
     *                   Click: iconChooser(Player, Waypoint, int);
     *             E: 显示信息:
     *                   Type: NAMETAG
     *                   DisplayName: 修改名称
     *                   Click: renamer(Player, Waypoint, int);
     *             F: 显示信息:
     *                   Type: 红色黏土
     *                   DisplayName: 删除路径点
     *                   Click: deleteConfirm(Player, Waypoint, int);
     *             #: AIR
     *     Icon Chooser:
     *         格式和菜单格式相同，里面的物品从config中读取"waypoint.allowed-icons"然后用Material.valueOf()直接获取物品即可，
     *         不需要修改任何ItemMeta，直接关联点击事件然后再返回路径点详细页即可
     *     Renamer:
     *         推荐用铁砧做: https://github.com/WesJD/AnvilGUI
     *         铁砧界面，左边的输入放一个纸，将其名称设置为waypoint.name()，为了支持adventure，检测物品名称被修改（其实就是输出物品被修改事件），
     *         将输出物品的名称用Adventure来parse一下格式重新设置输出物品，检测点击输出物品的事件，然后点击了以后就返回路径点详细页，
     *         别忘了检测页面关闭事件，关闭了就当作是取消重命名
     *     Delete Confirm:
     *         两个按钮，确认和取消，取消就返回路径点详细页，确认就通过输入的backPage打开对应的路径点列表的页面
     *     Create Waypoint:
     *         推荐用铁砧做: https://github.com/WesJD/AnvilGUI
     *         铁砧界面，左边的输入放一个纸，将其名称设置为 ”新路径点“，为了支持adventure，检测物品名称被修改（其实就是输出物品被修改事件），
     *         将输出物品的名称用Adventure来parse一下格式重新设置输出物品，检测点击输出物品的事件，然后点击了以后就创建然后直接进入详细页，
     *         别忘了检测页面关闭事件，关闭了就当作是取消创建
     */

    private final static ItemStack PREVIOUS = new ItemStack(Material.ARROW);
    private final static ItemStack NEXT = new ItemStack(Material.ARROW);

    static {
        {
            ItemMeta itemMeta = PREVIOUS.getItemMeta();
            if (itemMeta != null) {
                itemMeta.displayName(mini("<green>上一页"));
                PREVIOUS.setItemMeta(itemMeta);
            }
        }
        {
            ItemMeta itemMeta = NEXT.getItemMeta();
            if (itemMeta != null) {
                itemMeta.displayName(mini("<green>下一页"));
                NEXT.setItemMeta(itemMeta);
            }
        }
    }

    public static void open(Player player, int page) {
    }

    public static void openAdmin(Player player, int page) {
    }

    public static void openInfo(Player player, Waypoint waypoint, int backPage) {
    }

    public static void openInfoCreatorOrAdmin(Player player, Waypoint waypoint, int backPage) {
    }

    public static void iconChooser(Player player, Waypoint waypoint, int backPage) {
    }

    public static void renamer(Player player, Waypoint waypoint, int backPage) {
        // 推荐用铁砧做: https://github.com/WesJD/AnvilGUI
    }

    public static void deleteConfirm(Player player, Waypoint waypoint, int backPage) {
    }

    public static void searchPage(Player player, int backPage) {
    }

    public static void createWaypoint(Player player, int backPage) {
    }

    private static String dimension(String worldName) {
        worldName = worldName.toLowerCase();
        if (worldName.endsWith("_nether")) {
            return "下界";
        } else if (worldName.endsWith("_the_end")) {
            return "末地";
        } else {
            return "主世界";
        }
    }

    private static Component mini(String string) {
        return MiniMessage.miniMessage().deserialize(string);
    }

    private GUIBuilder() {}

}
