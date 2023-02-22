package com.github.militalex.main;

import com.github.militalex.util.datapack.DatapackManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * This class can read the server.properties.
 *
 * @author Militalex
 * @version 1.2
 */
public class ServerPropertiesManager {

    // Singleton Pattern
    private static ServerPropertiesManager manager;

    public static ServerPropertiesManager getInstance() {
        if (manager == null) manager = new ServerPropertiesManager();
        return manager;
    }
    // Singleton Pattern

    private final HashMap<String, String> properties = new HashMap<>();

    private ServerPropertiesManager() {
        final Path propertiesFiles = Paths.get("server.properties");
        final List<String> lines;

        try {
            lines = Files.readAllLines(propertiesFiles);
        } catch (IOException e) {
            CommandBlockHelper.LOGGER.log(Level.SEVERE, "Cannot load Server Properties! Disabling Plugin ...");
            throw new RuntimeException("Cannot load Server Properties!", e);
        }

        for (int i = 2; i < lines.size(); i++){
            final List<String> keyValue = Arrays.stream(lines.get(i).split("=")).toList();

            final String key = keyValue.get(0);
            final String value = (keyValue.size() == 1) ? null : keyValue.get(1);

            properties.put(key, value);
        }
    }

    public String getProperty(String key){
        return properties.get(key);
    }
}