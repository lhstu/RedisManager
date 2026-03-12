package com.redismanager.support;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AppDirectories {
    private static final String APP_DIR_NAME = ".redismanager";
    private static final String DATA_DIR_NAME = "data";
    private static final String LOG_DIR_NAME = "logs";

    private AppDirectories() {
    }

    public static Path root() {
        return Path.of(System.getProperty("user.home"), APP_DIR_NAME);
    }

    public static Path data() {
        return root().resolve(DATA_DIR_NAME);
    }

    public static Path logs() {
        return root().resolve(LOG_DIR_NAME);
    }

    public static Path databasePath() {
        return data().resolve("redismanager.db");
    }

    public static void ensureCreated() throws IOException {
        Files.createDirectories(root());
        Files.createDirectories(data());
        Files.createDirectories(logs());
    }
}
