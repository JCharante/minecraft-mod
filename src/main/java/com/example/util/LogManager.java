package com.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class LogManager {

    private static final Logger LOGGER = LoggerFactory.getLogger("mymod");
    private static final ArrayList<String> activeLoggers = new ArrayList<>();
    private static final Map<String, Long> lastLoggedTimes = new HashMap<>();
    private static final long DEBOUNCE_TIME = 1000; // Time in milliseconds


    // Log message if the loggerID is active and message is not debounced
    public static void info(String loggerID, String message) {
        if (shouldLogMessage(loggerID, message)) {
            LOGGER.info("[" + loggerID + "] " + message);
            lastLoggedTimes.put(generateKey(loggerID, message), System.currentTimeMillis());
        }
    }


    // Generate a unique key for loggerID and message combination
    private static String generateKey(String loggerID, String message) {
        return loggerID + "||" + message;
    }

    // Activate a logger
    public static void logOn(String loggerID) {
        if (!activeLoggers.contains(loggerID)) {
            activeLoggers.add(loggerID);
        }
    }

    // Deactivate a logger
    public static void logOff(String loggerID) {
        activeLoggers.remove(loggerID);
    }

    // Check if a message should be logged based on debounce logic
    private static boolean shouldLogMessage(String loggerID, String message) {
//        // Check if loggerID is active
//        if (!activeLoggers.contains(loggerID)) {
//            return false;
//        }

        // Check if the message has been logged recently
        String key = generateKey(loggerID, message);
        Long lastLoggedTime = lastLoggedTimes.get(key);

        // If the message hasn't been logged before, or the last logged time is older than DEBOUNCE_TIME
        if (lastLoggedTime == null || System.currentTimeMillis() - lastLoggedTime > DEBOUNCE_TIME) {
            return true;
        }

        return false;
    }
}

