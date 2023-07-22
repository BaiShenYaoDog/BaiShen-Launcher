package cn.ChengZhiYa.BaiShenLauncher.countly;

import cn.ChengZhiYa.BaiShenLauncher.util.Pair;
import cn.ChengZhiYa.BaiShenLauncher.util.io.HttpRequest;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class Countly {

    private static final String APP_KEY = "";
    private String deviceId;
    private String endpoint;
    private String serverURL;

    private static int getTimezoneOffset() {
        return TimeZone.getDefault().getOffset(new Date().getTime()) / 60000;
    }

    private static String getLocale() {
        final Locale locale = Locale.getDefault();
        return locale.getLanguage() + "_" + locale.getCountry();
    }

    private static int currentHour() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    public void sendMetric(String metrics) throws IOException {
        HttpRequest.GET(serverURL + endpoint,
                        Pair.pair("begin_session", "1"),
                        Pair.pair("session_id", "1"),
                        Pair.pair("metrics", metrics),
                        Pair.pair("device_id", deviceId),
                        Pair.pair("timestamp", Long.toString(System.currentTimeMillis())),
                        Pair.pair("tz", Integer.toString(TimeZone.getDefault().getOffset(new Date().getTime()) / 60000)),
                        Pair.pair("hour", Integer.toString(currentHour())),
                        Pair.pair("dow", Integer.toString(currentDayOfWeek())),
                        Pair.pair("app_key", APP_KEY),
                        Pair.pair("sdk_name", "java-native"),
                        Pair.pair("sdk_version", "20.11.1"))
                .getString();
    }

    private int currentDayOfWeek() {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.SUNDAY:
                return 0;
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
        }
        return 0;
    }
}
