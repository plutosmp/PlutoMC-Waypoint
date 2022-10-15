package top.plutomc.plugin.waypoint.utils;

import cc.carm.lib.easysql.api.SQLManager;
import cc.carm.lib.easysql.api.SQLQuery;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.bukkit.Material;
import top.plutomc.plugin.waypoint.WaypointPlugin;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class WaypointHelper {

    private final static Gson GSON = new Gson();

    private final static String SIGN_SOURCE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-_=+/@#!$%&\\0123456789";

    private static SQLManager setup() {
        SQLManager sqlManager = WaypointPlugin.getSqlManager();
        sqlManager.executeSQLBatch("use " + WaypointPlugin.Config.string("storage.mysql.database") + ";");
        return sqlManager;
    }

    private static String waypointTable() {
        return WaypointPlugin.Config.string("storage.mysql.table_prefix") + "waypoints";
    }

    public static Waypoint create(String name, Material icon, UUID creator, String world, int x, int y, int z) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("world", world);
        jsonObject.addProperty("x", x);
        jsonObject.addProperty("y", y);
        jsonObject.addProperty("z", z);
        String sign = generateSign();
        try {
            SQLManager sqlManager = setup();
            sqlManager.createReplace(waypointTable())
                    .setColumnNames("CREATOR_UUID", "WAYPOINT_NAME", "WAYPOINT_ICON", "WAYPOINT_INFO", "SIGN", "CREATE_DATE")
                    .setParams(creator.toString(), name, icon.name().toLowerCase(), GSON.toJson(jsonObject), sign, MiscUtils.date())
                    .execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return findBySign(sign);
    }

    public static Waypoint findById(long id) {
        Waypoint waypoint = null;
        SQLManager sqlManager = setup();
        try {
            ResultSet resultSet = sqlManager.createQuery()
                    .inTable(waypointTable())
                    .selectColumns("WAYPOINT_ID", "CREATOR_UUID", "WAYPOINT_NAME", "WAYPOINT_ICON", "WAYPOINT_INFO", "SIGN", "CREATE_DATE")
                    .addCondition("WAYPOINT_ID", id)
                    .build().execute().getResultSet();
            if (resultSet.next())
                waypoint = Waypoint.fromSql(resultSet.getLong("WAYPOINT_ID"),
                        resultSet.getString("WAYPOINT_NAME"),
                        Material.valueOf(resultSet.getString("WAYPOINT_ICON").toUpperCase()),
                        UUID.fromString(resultSet.getString("CREATOR_UUID")),
                        resultSet.getString("WAYPOINT_INFO"),
                        resultSet.getString("SIGN"),
                        resultSet.getString("CREATE_DATE")
                );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return waypoint;
    }

    public static Waypoint findByCreator(UUID uuid) {
        return findByCreator(uuid.toString());
    }

    public static Waypoint findByCreator(String uuid) {
        Waypoint waypoint = null;
        SQLManager sqlManager = setup();
        try {
            ResultSet resultSet = sqlManager.createQuery()
                    .inTable(waypointTable())
                    .selectColumns("WAYPOINT_ID", "CREATOR_UUID", "WAYPOINT_NAME", "WAYPOINT_ICON", "WAYPOINT_INFO", "SIGN", "CREATE_DATE")
                    .addCondition("CREATOR_UUID", uuid)
                    .build().execute().getResultSet();
            if (resultSet.next())
                waypoint = Waypoint.fromSql(resultSet.getLong("WAYPOINT_ID"),
                        resultSet.getString("WAYPOINT_NAME"),
                        Material.valueOf(resultSet.getString("WAYPOINT_ICON").toUpperCase()),
                        UUID.fromString(resultSet.getString("CREATOR_UUID")),
                        resultSet.getString("WAYPOINT_INFO"),
                        resultSet.getString("SIGN"),
                        resultSet.getString("CREATE_DATE")
                );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return waypoint;
    }

    public static Set<Waypoint> findByName(String name) {
        Set<Waypoint> waypoints = new HashSet<>();
        SQLManager sqlManager = setup();
        sqlManager.createQuery()
                .inTable(waypointTable())
                .selectColumns("WAYPOINT_ID", "CREATOR_UUID", "WAYPOINT_NAME", "WAYPOINT_ICON", "WAYPOINT_INFO", "SIGN", "CREATE_DATE")
                .addCondition("WAYPOINT_NAME", name)
                .build().execute(sqlQuery -> {
                    ResultSet resultSet = sqlQuery.getResultSet();
                    while (resultSet.next()) {
                        waypoints.add(Waypoint.fromSql(resultSet.getLong("WAYPOINT_ID"),
                                resultSet.getString("WAYPOINT_NAME"),
                                Material.valueOf(resultSet.getString("WAYPOINT_ICON").toUpperCase()),
                                UUID.fromString(resultSet.getString("CREATOR_UUID")),
                                resultSet.getString("WAYPOINT_INFO"),
                                resultSet.getString("SIGN"),
                                resultSet.getString("CREATE_DATE")
                                )
                        );
                    }
                    return resultSet;
                }, (exception, sqlAction) -> {
                    exception.printStackTrace();
                });
        return waypoints;
    }

    public static Waypoint findBySign(String sign) {
        Waypoint waypoint = null;
        SQLManager sqlManager = setup();
        try {
            ResultSet resultSet = sqlManager.createQuery()
                    .inTable(waypointTable())
                    .selectColumns("WAYPOINT_ID", "CREATOR_UUID", "WAYPOINT_NAME", "WAYPOINT_ICON", "WAYPOINT_INFO", "SIGN", "CREATE_DATE")
                    .addCondition("SIGN", sign)
                    .build().execute().getResultSet();
            if (resultSet.next())
                waypoint = Waypoint.fromSql(resultSet.getLong("WAYPOINT_ID"),
                        resultSet.getString("WAYPOINT_NAME"),
                        Material.valueOf(resultSet.getString("WAYPOINT_ICON").toUpperCase()),
                        UUID.fromString(resultSet.getString("CREATOR_UUID")),
                        resultSet.getString("WAYPOINT_INFO"),
                        resultSet.getString("SIGN"),
                        resultSet.getString("CREATE_DATE")
                );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return waypoint;
    }

    public static List<Waypoint> findAll() {
        List<Waypoint> waypoints = new ArrayList<>();
        SQLManager sqlManager = setup();
        try {
            SQLQuery query = sqlManager.createQuery()
                    .inTable(waypointTable())
                    .selectColumns("WAYPOINT_ID", "CREATOR_UUID", "WAYPOINT_NAME", "WAYPOINT_ICON", "WAYPOINT_INFO", "SIGN", "CREATE_DATE")
                    .build()
                    .execute();
            ResultSet resultSet = query.getResultSet();
            while (resultSet.next()) {
                waypoints.add(Waypoint.fromSql(resultSet.getLong("WAYPOINT_ID"),
                                resultSet.getString("WAYPOINT_NAME"),
                                Material.valueOf(resultSet.getString("WAYPOINT_ICON").toUpperCase()),
                                UUID.fromString(resultSet.getString("CREATOR_UUID")),
                                resultSet.getString("WAYPOINT_INFO"),
                                resultSet.getString("SIGN"),
                                resultSet.getString("CREATE_DATE")
                        )
                );
            }
            query.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return waypoints;
    }

    public static void updateName(int id, String name) {
        SQLManager sqlManager = setup();
        try {
            sqlManager.createUpdate(waypointTable())
                    .addColumnValue("WAYPOINT_NAME", name)
                    .addCondition("WAYPOINT_ID", id)
                    .build()
                    .execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void updateIcon(int id, Material type) {
        SQLManager sqlManager = setup();
        try {
            sqlManager.createUpdate(waypointTable())
                    .addColumnValue("WAYPOINT_ICON", type.name().toLowerCase())
                    .addCondition("WAYPOINT_ID", id)
                    .build()
                    .execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean containsId(long id) {
        SQLManager sqlManager = setup();
        try {
            return sqlManager.createQuery()
                    .inTable(waypointTable())
                    .selectColumns("WAYPOINT_ID")
                    .addCondition("WAYPOINT_ID", id)
                    .build().execute().getResultSet().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean containsSign(String sign) {
        SQLManager sqlManager = setup();
        try {
            return sqlManager.createQuery()
                    .inTable(waypointTable())
                    .selectColumns("SIGN")
                    .addCondition("SIGN", sign)
                    .build().execute().getResultSet().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static String generateSign() {
        String sign = randomSign();
        while (containsSign(sign))
            sign = randomSign();
        return sign;
    }

    private static String randomSign() {
        StringBuilder builder = new StringBuilder();
        Random random = new Random();
        int length = SIGN_SOURCE.length();
        for (int i = 0; i < 24; i++) {
            builder.append(SIGN_SOURCE.charAt(random.nextInt(length)));
        }
        return builder.toString();
    }

    private WaypointHelper() {}

}
