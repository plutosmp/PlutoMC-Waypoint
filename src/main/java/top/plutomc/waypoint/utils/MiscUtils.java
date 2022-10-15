package top.plutomc.waypoint.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class MiscUtils {

    private final static SimpleDateFormat SIMPLE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static String date() {
        return SIMPLE_FORMAT.format(new Date());
    }

    private MiscUtils() {}

}
