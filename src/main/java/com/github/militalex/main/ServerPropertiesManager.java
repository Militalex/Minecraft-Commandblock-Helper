package com.github.militalex.main;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * This class enables reading server.properties. Only one instance can exist of this class because it follows singleton pattern.
 *
 * @author Militalex
 * @version 1.2
 */
public class ServerPropertiesManager {

    // Singleton Pattern
    /**
     * The only one instance.
     */
    private static ServerPropertiesManager manager;

    /**
     * @return Returns the only one instance.
     */
    public static ServerPropertiesManager getInstance() {
        if (manager == null) manager = new ServerPropertiesManager();
        return manager;
    }
    // Singleton Pattern

    /**
     * Saves the server.properties as key value pairs.
     */
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Reading the server.properties and construct properties hash map.
     */
    private ServerPropertiesManager() {
        // Path to server.properties file
        final Path propertiesFiles = Paths.get("server.properties");

        // Read server.properties line by line
        final List<String> lines;
        try {
            lines = Files.readAllLines(propertiesFiles);
        } catch (IOException e) {
            CommandBlockHelper.LOGGER.log(Level.SEVERE, "Cannot load Server Properties! Disabling Plugin ...");
            throw new RuntimeException("Cannot load Server Properties!", e);
        }

        // Construct key value pairs from line by line, by splitting on "=".
        for (int i = 2; i < lines.size(); i++){
            final List<String> keyValue = Arrays.stream(lines.get(i).split("=")).toList();

            final String key = keyValue.get(0);
            final String value = (keyValue.size() == 1) ? null : keyValue.get(1);

            properties.put(key, value);
        }
    }

    /**
     * @return Returns value from server properties key or null if non-existent.
     */
    public @Nullable String getProperty(String key){
        return properties.get(key);
    }
}