package com.redismanager.app;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.service.command.CommandHistoryService;
import com.redismanager.service.connection.ConnectionProfileService;
import com.redismanager.service.connection.ConnectionSessionService;
import com.redismanager.service.connection.ConnectionTestService;
import com.redismanager.ui.shell.MainShell;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public final class RedisManagerApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(RedisManagerApplication.class);
    private final ApplicationBootstrap bootstrap = new ApplicationBootstrap();

    @Override
    public void start(Stage stage) {
        bootstrap.initialize();
        CommandHistoryService commandHistoryService = bootstrap.commandHistoryService();
        ConnectionProfileService connectionProfileService = bootstrap.connectionProfileService();
        ConnectionTestService connectionTestService = bootstrap.connectionTestService();
        ConnectionSessionService connectionSessionService = bootstrap.connectionSessionService();
        List<ConnectionProfile> profiles = connectionProfileService.listProfiles();

        MainShell shell = new MainShell(
            connectionProfileService,
            connectionTestService,
            connectionSessionService,
            commandHistoryService,
            profiles
        );
        Scene scene = new Scene(shell.getRoot(), 1400, 900);
        scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());

        stage.setTitle("RedisManager");
        stage.setMinWidth(1100);
        stage.setMinHeight(720);
        stage.setScene(scene);
        stage.show();

        log.info("RedisManager UI started with {} saved connection profiles", profiles.size());
    }
}
