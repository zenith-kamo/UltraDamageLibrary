package com.zenith.udl.manager;

import java.util.UUID;

public class TimeStopManager {
    public static boolean isTimeStopped = false;
    public static UUID exemptPlayerUuid = null;

    public static void stopTime(UUID playerUuid) {
        isTimeStopped = true;
        exemptPlayerUuid = playerUuid;
    }

    public static void resumeTime() {
        isTimeStopped = false;
        exemptPlayerUuid = null;
    }

    public static boolean isTimeStopped() {
        return isTimeStopped;
    }

    public static boolean isExempt(UUID uuid) {
        return uuid != null && uuid.equals(exemptPlayerUuid);
    }
}