package com.redismanager.app;

import javafx.application.Application;

public final class AppLauncher {
    private AppLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(RedisManagerApplication.class, args);
    }
}
