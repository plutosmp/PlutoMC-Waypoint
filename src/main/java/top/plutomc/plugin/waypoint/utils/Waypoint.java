package top.plutomc.plugin.waypoint.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Material;

import java.util.UUID;

public record Waypoint(long id,
                       String name,
                       Material icon,
                       UUID creator,
                       String world,
                       int x,
                       int y,
                       int z,
                       String sign,
                       String date) {

    public JsonObject serialise() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("world", world);
        jsonObject.addProperty("x", x);
        jsonObject.addProperty("y", y);
        jsonObject.addProperty("z", z);
        return jsonObject;
    }

    public static Waypoint fromSql(long id, String name, Material icon, UUID creator, String info, String sign, String date) {
        JsonObject jsonObject = JsonParser.parseString(info).getAsJsonObject();
        return new Waypoint(id,
                name,
                icon,
                creator,
                jsonObject.get("world").getAsString(),
                jsonObject.get("x").getAsInt(),
                jsonObject.get("y").getAsInt(),
                jsonObject.get("z").getAsInt(),
                sign,
                date);
    }

}
