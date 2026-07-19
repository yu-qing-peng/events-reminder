package com.eventreminder.app.util;

import java.util.concurrent.TimeUnit;

public class CountdownFormatter {

    public static String format(long diffMs) {
        if (diffMs < 0) {
            return "Passed";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }

    public static String formatShort(long diffMs) {
        if (diffMs < 0) {
            return "Passed";
        }

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs);
        if (minutes < 1) {
            return "<1m";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;
        long mins = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60;

        if (days > 0) {
            return days + "d " + hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + mins + "m";
        }
        return mins + "m";
    }

    public static String formatNotification(long diffMs) {
        if (diffMs < 0) {
            return "Happening now!";
        }

        long days = TimeUnit.MILLISECONDS.toDays(diffMs);
        long hours = TimeUnit.MILLISECONDS.toHours(diffMs) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diffMs) % 60;

        if (days > 0) {
            return days + "d " + hours + "h " + minutes + "m";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        }
        return minutes + "m " + seconds + "s";
    }

    public static boolean isSoon(long diffMs) {
        return diffMs > 0 && diffMs < 60 * 60 * 1000;
    }

    public static boolean isPast(long diffMs) {
        return diffMs < 0;
    }
}
