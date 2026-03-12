package com.redismanager.app;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AppLauncherTest {
    @Test
    void launcherClassShouldExist() {
        assertDoesNotThrow(() -> Class.forName("com.redismanager.app.AppLauncher"));
    }
}
