package com.redismanager.ui.shell;

import com.redismanager.domain.connection.ConnectionProfile;
import com.redismanager.domain.connection.ConnectionProfileDraft;
import com.redismanager.domain.redis.CommandHistoryEntry;
import com.redismanager.domain.redis.KeySummary;
import com.redismanager.domain.redis.KeyType;
import com.redismanager.domain.redis.PingResult;
import com.redismanager.domain.redis.CommandRequest;
import com.redismanager.domain.redis.CommandResult;
import com.redismanager.domain.redis.ScanRequest;
import com.redismanager.domain.redis.SortedSetEntry;
import com.redismanager.domain.redis.ValueEnvelope;
import com.redismanager.domain.redis.ValueMutation;
import com.redismanager.redis.api.RedisSession;
import com.redismanager.service.command.CommandHistoryService;
import com.redismanager.service.connection.ConnectionProfileService;
import com.redismanager.service.connection.ConnectionSessionService;
import com.redismanager.service.connection.ConnectionTestResult;
import com.redismanager.service.connection.ConnectionTestService;
import com.redismanager.support.redis.CommandLineParser;
import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class MainShell {
    private static final int DEFAULT_SCAN_COUNT = 200;
    private static final Set<String> WRITE_COMMANDS = Set.of(
        "APPEND",
        "COPY",
        "DECR",
        "DECRBY",
        "DEL",
        "EXPIRE",
        "EXPIREAT",
        "FLUSHALL",
        "FLUSHDB",
        "GETDEL",
        "GETEX",
        "HDEL",
        "HINCRBY",
        "HINCRBYFLOAT",
        "HMSET",
        "HSET",
        "HSETNX",
        "INCR",
        "INCRBY",
        "INCRBYFLOAT",
        "LINSERT",
        "LPOP",
        "LPUSH",
        "LPUSHX",
        "LREM",
        "LSET",
        "LTRIM",
        "MIGRATE",
        "MOVE",
        "MSET",
        "MSETNX",
        "PERSIST",
        "PEXPIRE",
        "PEXPIREAT",
        "PFMERGE",
        "PSETEX",
        "RENAME",
        "RENAMENX",
        "RESTORE",
        "RPOP",
        "RPUSH",
        "RPUSHX",
        "SADD",
        "SET",
        "SETEX",
        "SETNX",
        "SETRANGE",
        "SMOVE",
        "SPOP",
        "SREM",
        "SUNIONSTORE",
        "UNLINK",
        "XACK",
        "XADD",
        "XCLAIM",
        "XDEL",
        "XGROUP",
        "ZADD",
        "ZINCRBY",
        "ZPOPMAX",
        "ZPOPMIN",
        "ZREM",
        "ZREMRANGEBYLEX",
        "ZREMRANGEBYRANK",
        "ZREMRANGEBYSCORE",
        "ZUNIONSTORE"
    );

    private final BorderPane root;
    private final ConnectionProfileService connectionProfileService;
    private final ConnectionTestService connectionTestService;
    private final ConnectionSessionService connectionSessionService;
    private final CommandHistoryService commandHistoryService;
    private final ListView<ConnectionListItem> connectionList;
    private final ListView<KeySummary> keyList;
    private final ListView<CommandHistoryEntry> commandHistoryList;
    private final TextArea valuePreviewArea;
    private final TableView<HashFieldRow> hashTable;
    private final TableView<CollectionItemRow> collectionTable;
    private final ListView<String> inspectorList;
    private final Label connectionLabel;
    private final Label valueTitleLabel;
    private final TextField searchField;
    private final TextField ttlField;
    private final Button editConnectionButton;
    private final Button deleteConnectionButton;
    private final Button saveValueButton;
    private final Button reloadValueButton;
    private final Button addHashFieldButton;
    private final Button removeHashFieldButton;
    private final Button loadMoreKeysButton;
    private final Button applyTtlButton;
    private final Button persistTtlButton;
    private final Button deleteKeyButton;
    private final Button runCommandButton;
    private final Button clearHistoryButton;
    private final Label keyStatusLabel;
    private final TextArea consoleOutputArea;
    private final TextField commandInput;
    private final Label sessionModeLabel;
    private final Label statusLabel;

    private RedisSession currentSession;
    private KeySummary selectedKeySummary;
    private KeyType selectedKeyType = KeyType.UNKNOWN;
    private long currentLoadedTtl = -2;
    private String nextScanCursor = "0";
    private boolean scanFinished = true;

    public MainShell(
        ConnectionProfileService connectionProfileService,
        ConnectionTestService connectionTestService,
        ConnectionSessionService connectionSessionService,
        CommandHistoryService commandHistoryService,
        List<ConnectionProfile> connectionProfiles
    ) {
        this.connectionProfileService = connectionProfileService;
        this.connectionTestService = connectionTestService;
        this.connectionSessionService = connectionSessionService;
        this.commandHistoryService = commandHistoryService;
        this.connectionList = new ListView<>();
        this.keyList = new ListView<>();
        this.commandHistoryList = new ListView<>();
        this.valuePreviewArea = new TextArea();
        this.hashTable = new TableView<>();
        this.collectionTable = new TableView<>();
        this.inspectorList = new ListView<>();
        this.connectionLabel = new Label("当前连接：未连接");
        this.valueTitleLabel = new Label("Value 预览");
        this.searchField = new TextField();
        this.ttlField = new TextField();
        this.editConnectionButton = new Button("编辑连接");
        this.deleteConnectionButton = new Button("删除连接");
        this.saveValueButton = new Button("保存 Value");
        this.reloadValueButton = new Button("重新加载");
        this.addHashFieldButton = new Button("新增字段");
        this.removeHashFieldButton = new Button("删除字段");
        this.loadMoreKeysButton = new Button("继续加载");
        this.applyTtlButton = new Button("应用 TTL");
        this.persistTtlButton = new Button("移除 TTL");
        this.deleteKeyButton = new Button("删除 Key");
        this.runCommandButton = new Button("执行命令");
        this.clearHistoryButton = new Button("清空历史");
        this.keyStatusLabel = new Label("尚未连接 Redis");
        this.consoleOutputArea = new TextArea();
        this.commandInput = new TextField();
        this.sessionModeLabel = new Label("模式：未连接");
        this.statusLabel = new Label("状态：等待操作");

        this.root = new BorderPane();
        this.root.getStyleClass().add("app-root");
        this.root.setTop(buildToolbar());
        this.root.setCenter(buildCenterPane());
        this.root.setBottom(buildBottomPane());

        configureLists();
        seedPlaceholderData(connectionProfiles);
    }

    public Parent getRoot() {
        return root;
    }

    private ToolBar buildToolbar() {
        Label title = new Label("RedisManager");
        title.getStyleClass().add("app-title");

        searchField.setPromptText("搜索 Key，使用 SCAN MATCH");
        searchField.setPrefWidth(280);
        searchField.setOnAction(event -> reloadKeys());

        connectionLabel.getStyleClass().add("muted-label");

        Button connectButton = new Button("新建连接");
        connectButton.setOnAction(event -> openCreateConnectionDialog());

        editConnectionButton.setDisable(true);
        editConnectionButton.setOnAction(event -> openEditConnectionDialog());

        deleteConnectionButton.setDisable(true);
        deleteConnectionButton.setOnAction(event -> deleteSelectedConnectionProfile());

        Button refreshButton = new Button("刷新");
        refreshButton.setOnAction(event -> {
            refreshConnectionProfiles();
            reloadKeys();
        });

        Button openButton = new Button("打开所选连接");
        openButton.setOnAction(event -> openSelectedConnection());

        ToolBar toolBar = new ToolBar(
            title,
            new Separator(Orientation.VERTICAL),
            connectionLabel,
            new Separator(Orientation.VERTICAL),
            searchField,
            refreshButton,
            openButton,
            editConnectionButton,
            deleteConnectionButton,
            connectButton
        );
        toolBar.getStyleClass().add("main-toolbar");
        return toolBar;
    }

    private SplitPane buildCenterPane() {
        VBox leftPane = buildBrowserPane();
        VBox centerPane = buildValuePane();
        VBox rightPane = buildInspectorPane();

        SplitPane splitPane = new SplitPane(leftPane, centerPane, rightPane);
        splitPane.setDividerPositions(0.25, 0.73);
        splitPane.getStyleClass().add("main-split-pane");
        return splitPane;
    }

    private VBox buildBrowserPane() {
        Label title = new Label("连接与 Key 浏览");
        title.getStyleClass().add("section-title");

        Label connectionsTitle = new Label("已保存连接");
        connectionsTitle.getStyleClass().add("subsection-title");
        VBox.setVgrow(connectionList, Priority.ALWAYS);

        Label keysTitle = new Label("当前连接 Key");
        keysTitle.getStyleClass().add("subsection-title");
        VBox.setVgrow(keyList, Priority.ALWAYS);

        keyStatusLabel.getStyleClass().add("muted-label");
        loadMoreKeysButton.setDisable(true);
        loadMoreKeysButton.setOnAction(event -> loadMoreKeys());
        ToolBar keyActions = new ToolBar(loadMoreKeysButton);

        VBox container = new VBox(10, title, connectionsTitle, connectionList, keysTitle, keyStatusLabel, keyActions, keyList);
        container.setPadding(new Insets(16));
        container.getStyleClass().add("content-panel");
        return container;
    }

    private VBox buildValuePane() {
        valueTitleLabel.getStyleClass().add("section-title");

        Label hint = new Label("打开连接并选择一个 Key 后，这里展示值预览。当前阶段优先支持 String。");
        hint.getStyleClass().add("muted-label");

        saveValueButton.setDisable(true);
        saveValueButton.setOnAction(event -> saveCurrentValue());

        reloadValueButton.setDisable(true);
        reloadValueButton.setOnAction(event -> reloadSelectedKey());

        addHashFieldButton.setDisable(true);
        addHashFieldButton.setOnAction(event -> addValueEntry());

        removeHashFieldButton.setDisable(true);
        removeHashFieldButton.setOnAction(event -> removeSelectedValueEntry());

        valuePreviewArea.setEditable(false);
        valuePreviewArea.setPromptText("Value 预览区");
        VBox.setVgrow(valuePreviewArea, Priority.ALWAYS);

        configureHashTable();
        hashTable.setVisible(false);
        hashTable.setManaged(false);
        VBox.setVgrow(hashTable, Priority.ALWAYS);

        configureCollectionTable();
        collectionTable.setVisible(false);
        collectionTable.setManaged(false);
        VBox.setVgrow(collectionTable, Priority.ALWAYS);

        ToolBar actions = new ToolBar(saveValueButton, reloadValueButton, addHashFieldButton, removeHashFieldButton);

        VBox container = new VBox(10, valueTitleLabel, hint, actions, valuePreviewArea, hashTable, collectionTable);
        container.setPadding(new Insets(16));
        container.getStyleClass().add("content-panel");
        return container;
    }

    private VBox buildInspectorPane() {
        Label title = new Label("详情信息");
        title.getStyleClass().add("section-title");

        Label hint = new Label("TTL、类型、大小摘要和加载状态在这里展示");
        hint.getStyleClass().add("muted-label");

        ttlField.setPromptText("TTL 秒数");
        ttlField.setDisable(true);
        ttlField.setOnAction(event -> applyTtlToSelectedKey());

        applyTtlButton.setDisable(true);
        applyTtlButton.setOnAction(event -> applyTtlToSelectedKey());

        persistTtlButton.setDisable(true);
        persistTtlButton.setOnAction(event -> persistSelectedKey());

        deleteKeyButton.setDisable(true);
        deleteKeyButton.setOnAction(event -> deleteSelectedKey());

        ToolBar ttlActions = new ToolBar(ttlField, applyTtlButton, persistTtlButton, deleteKeyButton);
        VBox.setVgrow(inspectorList, Priority.ALWAYS);

        VBox container = new VBox(10, title, hint, ttlActions, inspectorList);
        container.setPadding(new Insets(16));
        container.getStyleClass().add("content-panel");
        return container;
    }

    private VBox buildBottomPane() {
        VBox consolePane = buildConsolePane();

        sessionModeLabel.getStyleClass().add("status-label");
        statusLabel.getStyleClass().add("status-label");
        ToolBar statusBar = new ToolBar(sessionModeLabel, new Separator(Orientation.VERTICAL), statusLabel);
        statusBar.getStyleClass().add("status-bar");

        return new VBox(consolePane, statusBar);
    }

    private VBox buildConsolePane() {
        Label title = new Label("命令控制台");
        title.getStyleClass().add("section-title");

        consoleOutputArea.setEditable(false);
        consoleOutputArea.setPromptText("命令结果输出区");
        VBox.setVgrow(consoleOutputArea, Priority.ALWAYS);
        VBox.setVgrow(commandHistoryList, Priority.ALWAYS);

        commandInput.setPromptText("输入 Redis 命令，例如: GET user:1");
        commandInput.setOnAction(event -> executeConsoleCommand());

        runCommandButton.setOnAction(event -> executeConsoleCommand());
        clearHistoryButton.setOnAction(event -> clearCommandHistory());
        ToolBar actions = new ToolBar(runCommandButton, clearHistoryButton);
        Label historyTitle = new Label("最近命令");
        historyTitle.getStyleClass().add("subsection-title");

        VBox container = new VBox(10, title, consoleOutputArea, actions, commandInput, historyTitle, commandHistoryList);
        container.setPadding(new Insets(12, 16, 16, 16));
        container.getStyleClass().add("console-pane");
        container.setPrefHeight(320);
        return container;
    }

    private void configureLists() {
        connectionList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(ConnectionListItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.label());
            }
        });
        connectionList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            boolean editable = newValue != null && newValue.profile() != null;
            editConnectionButton.setDisable(!editable);
            deleteConnectionButton.setDisable(!editable);
        });

        keyList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(KeySummary item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.key() + "  [" + item.type() + ", TTL=" + formatTtl(item.ttlSeconds()) + "]");
            }
        });

        keyList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> handleKeySelection(newValue));

        commandHistoryList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(CommandHistoryEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText((item.success() ? "[OK] " : "[ERR] ") + item.commandText());
            }
        });
        commandHistoryList.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                commandInput.setText(newValue.commandText());
            }
        });
    }

    private void seedPlaceholderData(List<ConnectionProfile> connectionProfiles) {
        connectionList.getItems().clear();
        keyList.getItems().clear();
        valueTitleLabel.setText("Value 预览");
        valuePreviewArea.clear();
        valuePreviewArea.setEditable(false);
        valuePreviewArea.setVisible(true);
        valuePreviewArea.setManaged(true);
        hashTable.getItems().clear();
        hashTable.setVisible(false);
        hashTable.setManaged(false);
        collectionTable.getItems().clear();
        collectionTable.setVisible(false);
        collectionTable.setManaged(false);
        saveValueButton.setDisable(true);
        reloadValueButton.setDisable(true);
        addHashFieldButton.setDisable(true);
        removeHashFieldButton.setDisable(true);
        loadMoreKeysButton.setDisable(true);
        keyStatusLabel.setText("尚未连接 Redis");
        selectedKeySummary = null;
        selectedKeyType = KeyType.UNKNOWN;
        currentLoadedTtl = -2;
        nextScanCursor = "0";
        scanFinished = true;
        resetInspectorActions();
        updateSessionMode();
        setStatus("等待操作");

        if (connectionProfiles == null || connectionProfiles.isEmpty()) {
            connectionList.getItems().setAll(
                ConnectionListItem.section("连接管理"),
                ConnectionListItem.placeholder("当前没有已保存连接"),
                ConnectionListItem.placeholder("下一步接入连接表单与持久化")
            );
        } else {
            connectionList.getItems().add(ConnectionListItem.section("连接管理"));
            connectionProfiles.stream()
                .map(ConnectionListItem::profile)
                .forEach(connectionList.getItems()::add);
            connectionList.getItems().add(ConnectionListItem.placeholder("选中连接后点击“打开所选连接”"));
        }

        inspectorList.getItems().setAll(
            "TTL: -",
            "Type: -",
            "Encoding: -",
            "Size Hint: -",
            "Last Refresh: -"
        );
        commandHistoryList.getItems().clear();
    }

    private void refreshConnectionProfiles() {
        seedPlaceholderData(connectionProfileService.listProfiles());
    }

    private void deleteSelectedConnectionProfile() {
        ConnectionProfile selectedProfile = selectedConnectionProfile();
        if (selectedProfile == null) {
            showError("删除连接失败", "请先选择一个已保存连接");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除连接");
        alert.setHeaderText("确认删除当前连接配置");
        alert.setContentText(selectedProfile.name() + " / " + selectedProfile.host() + ":" + selectedProfile.port());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            connectionProfileService.deleteProfile(selectedProfile.id());
            if (currentSession != null && currentSession.profile().id().equals(selectedProfile.id())) {
                closeCurrentSessionAndReset();
            } else {
                refreshConnectionProfiles();
            }
        } catch (RuntimeException exception) {
            showError("删除连接失败", exception.getMessage());
        }
    }

    private void openSelectedConnection() {
        ConnectionListItem selectedItem = connectionList.getSelectionModel().getSelectedItem();
        if (selectedItem == null || selectedItem.profile() == null) {
            showError("打开连接失败", "请先在左侧选中一个已保存的连接");
            return;
        }

        connectionLabel.setText("当前连接：正在连接 " + selectedItem.profile().name());
        setStatus("正在连接 " + selectedItem.profile().name());
        inspectorList.getItems().setAll(
            "TTL: -",
            "Type: 正在建立会话",
            "Encoding: -",
            "Size Hint: -",
            "Last Refresh: 连接中"
        );

        connectionSessionService.open(selectedItem.profile()).whenComplete((session, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                connectionLabel.setText("当前连接：未连接");
                showError("打开连接失败", throwable.getMessage());
                return;
            }

            replaceCurrentSession(session);
            session.ping().whenComplete((pingResult, pingThrowable) -> Platform.runLater(() -> handlePingResult(session, pingResult, pingThrowable)));
        }));
    }

    private void replaceCurrentSession(RedisSession session) {
        if (currentSession != null) {
            currentSession.close();
        }
        currentSession = session;
        updateSessionMode();
    }

    private void closeCurrentSessionAndReset() {
        if (currentSession != null) {
            currentSession.close();
            currentSession = null;
        }
        connectionLabel.setText("当前连接：未连接");
        consoleOutputArea.clear();
        commandInput.clear();
        updateSessionMode();
        setStatus("连接已关闭");
        refreshConnectionProfiles();
    }

    private void handlePingResult(RedisSession session, PingResult pingResult, Throwable throwable) {
        if (throwable != null) {
            connectionLabel.setText("当前连接：连接失败");
            showError("连接验证失败", throwable.getMessage());
            return;
        }

        connectionLabel.setText("当前连接：" + session.profile().name() + " / " + session.profile().host() + ":" + session.profile().port());
        keyStatusLabel.setText("连接已建立，等待扫描");
        updateSessionMode();
        setStatus("已连接到 " + session.profile().name());
        inspectorList.getItems().setAll(
            "TTL: -",
            "Type: 会话已建立",
            "Encoding: -",
            "Size Hint: -",
            "Last Refresh: PING -> " + pingResult.message()
        );
        loadCommandHistory();
        reloadKeys();
    }

    private void reloadKeys() {
        if (currentSession == null) {
            keyList.getItems().clear();
            keyStatusLabel.setText("尚未连接 Redis");
            loadMoreKeysButton.setDisable(true);
            return;
        }

        keyList.getItems().clear();
        valueTitleLabel.setText("Value 预览");
        valuePreviewArea.clear();
        valuePreviewArea.setEditable(false);
        valuePreviewArea.setVisible(true);
        valuePreviewArea.setManaged(true);
        hashTable.getItems().clear();
        hashTable.setVisible(false);
        hashTable.setManaged(false);
        collectionTable.getItems().clear();
        collectionTable.setVisible(false);
        collectionTable.setManaged(false);
        saveValueButton.setDisable(true);
        reloadValueButton.setDisable(true);
        addHashFieldButton.setDisable(true);
        removeHashFieldButton.setDisable(true);
        loadMoreKeysButton.setDisable(true);
        selectedKeySummary = null;
        selectedKeyType = KeyType.UNKNOWN;
        currentLoadedTtl = -2;
        nextScanCursor = "0";
        scanFinished = false;
        keyStatusLabel.setText("正在扫描第一页...");
        setStatus("正在扫描 Key");
        resetInspectorActions();
        inspectorList.getItems().setAll(
            "TTL: -",
            "Type: 正在扫描",
            "Encoding: -",
            "Size Hint: count=" + DEFAULT_SCAN_COUNT,
            "Last Refresh: SCAN"
        );

        currentSession.scan(new ScanRequest(nextScanCursor, blankToNull(searchField.getText()), DEFAULT_SCAN_COUNT))
            .whenComplete((page, throwable) -> Platform.runLater(() -> {
                if (throwable != null) {
                    showError("加载 Key 失败", throwable.getMessage());
                    keyStatusLabel.setText("SCAN 失败");
                    setStatus("Key 扫描失败");
                    inspectorList.getItems().setAll(
                        "TTL: -",
                        "Type: 加载失败",
                        "Encoding: -",
                        "Size Hint: -",
                        "Last Refresh: SCAN 失败"
                    );
                    return;
                }

                appendScanPage(page, false);
            }));
    }

    private void loadMoreKeys() {
        if (currentSession == null || scanFinished) {
            return;
        }

        loadMoreKeysButton.setDisable(true);
        keyStatusLabel.setText("正在继续扫描...");
        setStatus("继续扫描 Key");
        currentSession.scan(new ScanRequest(nextScanCursor, blankToNull(searchField.getText()), DEFAULT_SCAN_COUNT))
            .whenComplete((page, throwable) -> Platform.runLater(() -> {
                if (throwable != null) {
                    showError("继续加载 Key 失败", throwable.getMessage());
                    keyStatusLabel.setText("继续加载失败");
                    setStatus("继续扫描失败");
                    if (!scanFinished) {
                        loadMoreKeysButton.setDisable(false);
                    }
                    return;
                }

                appendScanPage(page, true);
            }));
    }

    private void appendScanPage(com.redismanager.domain.redis.ScanPage<KeySummary> page, boolean append) {
        if (!append) {
            keyList.getItems().setAll(page.items());
        } else {
            keyList.getItems().addAll(page.items());
        }

        nextScanCursor = page.nextCursor();
        scanFinished = page.finished();
        loadMoreKeysButton.setDisable(scanFinished);
        keyStatusLabel.setText(scanFinished
            ? "已加载 " + keyList.getItems().size() + " 个 Key，扫描结束"
            : "已加载 " + keyList.getItems().size() + " 个 Key，可继续加载");
        setStatus(scanFinished
            ? "Key 扫描完成"
            : "Key 扫描分页完成");

        inspectorList.getItems().setAll(
            "TTL: -",
            "Type: 已加载 " + keyList.getItems().size() + " 个 Key",
            "Encoding: -",
            "Size Hint: Cursor -> " + page.nextCursor(),
            "Last Refresh: " + (scanFinished ? "SCAN 完成" : "SCAN 分页完成")
        );
    }

    private void handleKeySelection(KeySummary keySummary) {
        if (keySummary == null || currentSession == null) {
            return;
        }

        selectedKeySummary = keySummary;
        selectedKeyType = KeyType.UNKNOWN;
        currentLoadedTtl = keySummary.ttlSeconds();
        saveValueButton.setDisable(true);
        reloadValueButton.setDisable(false);
        addHashFieldButton.setDisable(true);
        removeHashFieldButton.setDisable(true);
        valuePreviewArea.setEditable(false);
        valuePreviewArea.setVisible(true);
        valuePreviewArea.setManaged(true);
        hashTable.getItems().clear();
        hashTable.setVisible(false);
        hashTable.setManaged(false);
        collectionTable.getItems().clear();
        collectionTable.setVisible(false);
        collectionTable.setManaged(false);
        valueTitleLabel.setText("Value 预览: " + keySummary.key());
        valuePreviewArea.setText("正在加载 Key 详情...");
        inspectorList.getItems().setAll(
            "TTL: 加载中",
            "Type: 加载中",
            "Encoding: -",
            "Size Hint: -",
            "Last Refresh: 读取 Key"
        );
        enableInspectorActions();

        currentSession.type(keySummary.key()).whenComplete((type, throwable) -> Platform.runLater(() -> {
            if (throwable != null) {
                valuePreviewArea.setText("读取 Key 类型失败: " + throwable.getMessage());
                showError("读取 Key 失败", throwable.getMessage());
                return;
            }

            selectedKeyType = type;
            if (type != KeyType.STRING && type != KeyType.HASH && type != KeyType.LIST && type != KeyType.SET && type != KeyType.ZSET) {
                renderUnsupportedValue(keySummary, type);
                return;
            }

            currentSession.loadValue(keySummary.key())
                .whenComplete((valueEnvelope, valueThrowable) -> Platform.runLater(() -> renderValueResult(keySummary, type, valueEnvelope, valueThrowable)));
        }));
    }

    private void renderUnsupportedValue(KeySummary keySummary, KeyType type) {
        saveValueButton.setDisable(true);
        reloadValueButton.setDisable(false);
        addHashFieldButton.setDisable(true);
        removeHashFieldButton.setDisable(true);
        valuePreviewArea.setEditable(false);
        valuePreviewArea.setVisible(true);
        valuePreviewArea.setManaged(true);
        hashTable.setVisible(false);
        hashTable.setManaged(false);
        collectionTable.setVisible(false);
        collectionTable.setManaged(false);
        valuePreviewArea.setText("当前阶段已实现类型识别，但值预览优先支持 String。\n\nKey: " + keySummary.key() + "\nType: " + type);
        inspectorList.getItems().setAll(
            "TTL: " + formatTtl(keySummary.ttlSeconds()),
            "Type: " + type,
            "Encoding: -",
            "Size Hint: -",
            "Last Refresh: 类型已识别"
        );
        currentLoadedTtl = keySummary.ttlSeconds();
        updateTtlField();
    }

    private void renderValueResult(KeySummary keySummary, KeyType type, ValueEnvelope valueEnvelope, Throwable throwable) {
        if (throwable != null) {
            valuePreviewArea.setText("读取 Value 失败: " + throwable.getMessage());
            showError("读取 Value 失败", throwable.getMessage());
            return;
        }

        if (type == KeyType.STRING) {
            renderStringValue(keySummary, valueEnvelope);
            return;
        }
        if (type == KeyType.HASH) {
            renderHashValue(keySummary, valueEnvelope);
            return;
        }
        if (type == KeyType.LIST || type == KeyType.SET || type == KeyType.ZSET) {
            renderCollectionValue(keySummary, type, valueEnvelope);
            return;
        }
        renderUnsupportedValue(keySummary, type);
    }

    private void saveCurrentValue() {
        if (currentSession == null || selectedKeySummary == null) {
            showError("保存失败", "请先选择一个 Key");
            return;
        }
        if (!guardWritableOperation("保存 Value")) {
            return;
        }

        Object payload;
        if (selectedKeyType == KeyType.STRING) {
            payload = valuePreviewArea.getText();
        } else if (selectedKeyType == KeyType.HASH) {
            payload = collectHashPayload();
        } else if (selectedKeyType == KeyType.LIST || selectedKeyType == KeyType.SET) {
            payload = collectCollectionPayload();
        } else if (selectedKeyType == KeyType.ZSET) {
            try {
                payload = collectSortedSetPayload();
            } catch (IllegalArgumentException exception) {
                showError("保存失败", exception.getMessage());
                return;
            }
        } else {
            showError("保存失败", "当前仅支持 String、Hash、List、Set 和 ZSet 类型保存");
            return;
        }

        saveValueButton.setDisable(true);
        setStatus("正在保存 Key " + selectedKeySummary.key());
        currentSession.saveValue(new ValueMutation(selectedKeySummary.key(), selectedKeyType, payload))
            .whenComplete((unused, throwable) -> Platform.runLater(() -> {
                saveValueButton.setDisable(false);
                if (throwable != null) {
                    setStatus("保存失败");
                    showError("保存 Value 失败", throwable.getMessage());
                    return;
                }

                setStatus("Key 已保存");
                showInfo("保存成功", "已写回 Key: " + selectedKeySummary.key());
                reloadSelectedKey();
            }));
    }

    private void reloadSelectedKey() {
        if (selectedKeySummary == null) {
            return;
        }
        handleKeySelection(selectedKeySummary);
    }

    private void renderStringValue(KeySummary keySummary, ValueEnvelope valueEnvelope) {
        String value = String.valueOf(valueEnvelope.payload());
        boolean readOnly = isReadOnlySession();
        valuePreviewArea.setVisible(true);
        valuePreviewArea.setManaged(true);
        valuePreviewArea.setText(value);
        valuePreviewArea.setEditable(!readOnly);
        hashTable.setVisible(false);
        hashTable.setManaged(false);
        hashTable.getItems().clear();
        collectionTable.setVisible(false);
        collectionTable.setManaged(false);
        collectionTable.getItems().clear();
        saveValueButton.setDisable(readOnly);
        reloadValueButton.setDisable(false);
        addHashFieldButton.setDisable(true);
        removeHashFieldButton.setDisable(true);
        addHashFieldButton.setText("新增字段");
        removeHashFieldButton.setText("删除字段");
        valueTitleLabel.setText("Value 预览: " + keySummary.key());
        currentLoadedTtl = valueEnvelope.ttlSeconds();
        updateTtlField();
        inspectorList.getItems().setAll(
            "TTL: " + formatTtl(valueEnvelope.ttlSeconds()),
            "Type: " + valueEnvelope.type(),
            "Encoding: -",
            "Size Hint: " + value.length() + " chars",
            "Last Refresh: " + valueEnvelope.loadedAt()
        );
    }

    private void renderHashValue(KeySummary keySummary, ValueEnvelope valueEnvelope) {
        boolean readOnly = isReadOnlySession();
        valuePreviewArea.clear();
        valuePreviewArea.setEditable(false);
        valuePreviewArea.setVisible(false);
        valuePreviewArea.setManaged(false);
        collectionTable.setVisible(false);
        collectionTable.setManaged(false);
        collectionTable.getItems().clear();
        hashTable.getItems().setAll(mapToRows(valueEnvelope.payload()));
        hashTable.setVisible(true);
        hashTable.setManaged(true);
        hashTable.setEditable(!readOnly);
        saveValueButton.setDisable(readOnly);
        reloadValueButton.setDisable(false);
        addHashFieldButton.setDisable(readOnly);
        removeHashFieldButton.setDisable(readOnly || hashTable.getSelectionModel().getSelectedItem() == null);
        addHashFieldButton.setText("新增字段");
        removeHashFieldButton.setText("删除字段");
        valueTitleLabel.setText("Hash 编辑器: " + keySummary.key());
        currentLoadedTtl = valueEnvelope.ttlSeconds();
        updateTtlField();
        inspectorList.getItems().setAll(
            "TTL: " + formatTtl(valueEnvelope.ttlSeconds()),
            "Type: " + valueEnvelope.type(),
            "Encoding: -",
            "Size Hint: " + hashTable.getItems().size() + " fields",
            "Last Refresh: " + valueEnvelope.loadedAt()
        );
    }

    private void renderCollectionValue(KeySummary keySummary, KeyType type, ValueEnvelope valueEnvelope) {
        boolean readOnly = isReadOnlySession();
        valuePreviewArea.clear();
        valuePreviewArea.setEditable(false);
        valuePreviewArea.setVisible(false);
        valuePreviewArea.setManaged(false);
        hashTable.getItems().clear();
        hashTable.setVisible(false);
        hashTable.setManaged(false);
        collectionTable.getItems().setAll(mapToCollectionRows(type, valueEnvelope.payload()));
        collectionTable.setVisible(true);
        collectionTable.setManaged(true);
        configureCollectionColumns(type);
        collectionTable.setEditable(!readOnly);
        saveValueButton.setDisable(readOnly);
        reloadValueButton.setDisable(false);
        addHashFieldButton.setDisable(readOnly);
        removeHashFieldButton.setDisable(readOnly || collectionTable.getSelectionModel().getSelectedItem() == null);
        addHashFieldButton.setText(type == KeyType.ZSET ? "新增成员" : "新增项");
        removeHashFieldButton.setText(type == KeyType.ZSET ? "删除成员" : "删除项");
        valueTitleLabel.setText(type + " 编辑器: " + keySummary.key());
        currentLoadedTtl = valueEnvelope.ttlSeconds();
        updateTtlField();
        inspectorList.getItems().setAll(
            "TTL: " + formatTtl(valueEnvelope.ttlSeconds()),
            "Type: " + valueEnvelope.type(),
            "Encoding: -",
            "Size Hint: " + collectionTable.getItems().size() + " items",
            "Last Refresh: " + valueEnvelope.loadedAt()
        );
    }

    private void configureHashTable() {
        hashTable.setEditable(true);
        hashTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<HashFieldRow, String> fieldColumn = new TableColumn<>("Field");
        fieldColumn.setCellValueFactory(cell -> cell.getValue().fieldProperty());
        fieldColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        fieldColumn.setOnEditCommit(event -> event.getRowValue().setField(event.getNewValue()));

        TableColumn<HashFieldRow, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(cell -> cell.getValue().valueProperty());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> event.getRowValue().setValue(event.getNewValue()));

        hashTable.getColumns().setAll(fieldColumn, valueColumn);
        hashTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
            removeHashFieldButton.setDisable(isReadOnlySession() || selectedKeyType != KeyType.HASH || newValue == null));
    }

    private void configureCollectionTable() {
        collectionTable.setEditable(true);
        collectionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<CollectionItemRow, String> indexColumn = new TableColumn<>("Index");
        indexColumn.setCellValueFactory(cell -> cell.getValue().indexProperty());
        indexColumn.setEditable(false);

        TableColumn<CollectionItemRow, String> valueColumn = new TableColumn<>("Value");
        valueColumn.setCellValueFactory(cell -> cell.getValue().valueProperty());
        valueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        valueColumn.setOnEditCommit(event -> event.getRowValue().setValue(event.getNewValue()));

        TableColumn<CollectionItemRow, String> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(cell -> cell.getValue().scoreProperty());
        scoreColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        scoreColumn.setOnEditCommit(event -> event.getRowValue().setScore(event.getNewValue()));

        collectionTable.getColumns().setAll(indexColumn, valueColumn, scoreColumn);
        collectionTable.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) ->
            removeHashFieldButton.setDisable(
                isReadOnlySession()
                    || !(selectedKeyType == KeyType.LIST || selectedKeyType == KeyType.SET || selectedKeyType == KeyType.ZSET)
                    || newValue == null
            ));
    }

    private void configureCollectionColumns(KeyType type) {
        if (collectionTable.getColumns().size() < 3) {
            return;
        }
        collectionTable.getColumns().get(0).setVisible(type == KeyType.LIST);
        collectionTable.getColumns().get(2).setVisible(type == KeyType.ZSET);
    }

    private List<HashFieldRow> mapToRows(Object payload) {
        if (!(payload instanceof Map<?, ?> rawMap)) {
            return List.of();
        }

        List<HashFieldRow> rows = new ArrayList<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            rows.add(new HashFieldRow(String.valueOf(entry.getKey()), String.valueOf(entry.getValue())));
        }
        return rows;
    }

    private Map<String, String> collectHashPayload() {
        Map<String, String> payload = new LinkedHashMap<>();
        for (HashFieldRow row : hashTable.getItems()) {
            if (row.field() == null || row.field().isBlank()) {
                continue;
            }
            payload.put(row.field().trim(), row.value() == null ? "" : row.value());
        }
        return payload;
    }

    private List<CollectionItemRow> mapToCollectionRows(KeyType type, Object payload) {
        List<CollectionItemRow> rows = new ArrayList<>();
        if ((type == KeyType.LIST || type == KeyType.SET) && payload instanceof List<?> list) {
            for (int index = 0; index < list.size(); index++) {
                rows.add(new CollectionItemRow(String.valueOf(index), String.valueOf(list.get(index)), ""));
            }
            return rows;
        }
        if (type == KeyType.ZSET && payload instanceof List<?> list) {
            int index = 0;
            for (Object item : list) {
                if (item instanceof SortedSetEntry entry) {
                    rows.add(new CollectionItemRow(String.valueOf(index++), entry.member(), String.valueOf(entry.score())));
                }
            }
        }
        return rows;
    }

    private List<String> collectCollectionPayload() {
        List<String> payload = new ArrayList<>();
        for (CollectionItemRow row : collectionTable.getItems()) {
            if (row.value() == null || row.value().isBlank()) {
                continue;
            }
            payload.add(row.value());
        }
        return List.copyOf(payload);
    }

    private List<SortedSetEntry> collectSortedSetPayload() {
        List<SortedSetEntry> payload = new ArrayList<>();
        for (CollectionItemRow row : collectionTable.getItems()) {
            if (row.value() == null || row.value().isBlank()) {
                continue;
            }

            double score;
            try {
                score = Double.parseDouble(row.score().isBlank() ? "0" : row.score());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("ZSet score 必须是数字: " + row.score());
            }
            payload.add(new SortedSetEntry(row.value(), score));
        }
        return List.copyOf(payload);
    }

    private void addHashField() {
        if (selectedKeyType != KeyType.HASH) {
            return;
        }
        HashFieldRow row = new HashFieldRow("", "");
        hashTable.getItems().add(row);
        hashTable.getSelectionModel().select(row);
        removeHashFieldButton.setDisable(false);
    }

    private void removeSelectedHashField() {
        if (selectedKeyType != KeyType.HASH) {
            return;
        }
        HashFieldRow selectedRow = hashTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            return;
        }
        hashTable.getItems().remove(selectedRow);
        removeHashFieldButton.setDisable(hashTable.getItems().isEmpty());
    }

    private void addCollectionItem() {
        if (!(selectedKeyType == KeyType.LIST || selectedKeyType == KeyType.SET || selectedKeyType == KeyType.ZSET)) {
            return;
        }
        CollectionItemRow row = new CollectionItemRow(
            selectedKeyType == KeyType.LIST ? String.valueOf(collectionTable.getItems().size()) : "",
            "",
            selectedKeyType == KeyType.ZSET ? "0" : ""
        );
        collectionTable.getItems().add(row);
        collectionTable.getSelectionModel().select(row);
        removeHashFieldButton.setDisable(false);
        refreshCollectionIndexes();
    }

    private void removeSelectedCollectionItem() {
        if (!(selectedKeyType == KeyType.LIST || selectedKeyType == KeyType.SET || selectedKeyType == KeyType.ZSET)) {
            return;
        }
        CollectionItemRow selectedRow = collectionTable.getSelectionModel().getSelectedItem();
        if (selectedRow == null) {
            return;
        }
        collectionTable.getItems().remove(selectedRow);
        removeHashFieldButton.setDisable(collectionTable.getItems().isEmpty());
        refreshCollectionIndexes();
    }

    private void addValueEntry() {
        if (!guardWritableOperation("新增条目")) {
            return;
        }
        if (selectedKeyType == KeyType.HASH) {
            addHashField();
            return;
        }
        addCollectionItem();
    }

    private void removeSelectedValueEntry() {
        if (!guardWritableOperation("删除条目")) {
            return;
        }
        if (selectedKeyType == KeyType.HASH) {
            removeSelectedHashField();
            return;
        }
        removeSelectedCollectionItem();
    }

    private void refreshCollectionIndexes() {
        if (selectedKeyType != KeyType.LIST) {
            return;
        }
        for (int index = 0; index < collectionTable.getItems().size(); index++) {
            collectionTable.getItems().get(index).setIndex(String.valueOf(index));
        }
    }

    private void applyTtlToSelectedKey() {
        if (currentSession == null || selectedKeySummary == null) {
            showError("修改 TTL 失败", "请先选择一个 Key");
            return;
        }
        if (!guardWritableOperation("修改 TTL")) {
            return;
        }

        long ttlSeconds;
        try {
            ttlSeconds = Long.parseLong(ttlField.getText().trim());
        } catch (RuntimeException exception) {
            showError("修改 TTL 失败", "TTL 必须是正整数秒数");
            return;
        }

        if (ttlSeconds <= 0) {
            showError("修改 TTL 失败", "TTL 必须大于 0");
            return;
        }

        setInspectorActionsDisabled(true);
        setStatus("正在更新 TTL");
        currentSession.expire(selectedKeySummary.key(), ttlSeconds)
            .whenComplete((updated, throwable) -> Platform.runLater(() -> {
                setInspectorActionsDisabled(false);
                if (throwable != null) {
                    setStatus("TTL 更新失败");
                    showError("修改 TTL 失败", throwable.getMessage());
                    return;
                }
                if (!updated) {
                    setStatus("TTL 更新失败");
                    showError("修改 TTL 失败", "Redis 未接受 TTL 更新");
                    return;
                }

                currentLoadedTtl = ttlSeconds;
                updateTtlField();
                setStatus("TTL 已更新");
                showInfo("TTL 已更新", "Key " + selectedKeySummary.key() + " 的 TTL 已更新为 " + ttlSeconds + " 秒");
                reloadSelectedKey();
            }));
    }

    private void persistSelectedKey() {
        if (currentSession == null || selectedKeySummary == null) {
            showError("移除 TTL 失败", "请先选择一个 Key");
            return;
        }
        if (!guardWritableOperation("移除 TTL")) {
            return;
        }

        setInspectorActionsDisabled(true);
        setStatus("正在移除 TTL");
        currentSession.persist(selectedKeySummary.key())
            .whenComplete((updated, throwable) -> Platform.runLater(() -> {
                setInspectorActionsDisabled(false);
                if (throwable != null) {
                    setStatus("移除 TTL 失败");
                    showError("移除 TTL 失败", throwable.getMessage());
                    return;
                }
                if (!updated) {
                    setStatus("移除 TTL 失败");
                    showError("移除 TTL 失败", "Redis 未接受 TTL 变更");
                    return;
                }

                currentLoadedTtl = -1;
                updateTtlField();
                setStatus("TTL 已移除");
                showInfo("TTL 已移除", "Key " + selectedKeySummary.key() + " 现在为永久保存");
                reloadSelectedKey();
            }));
    }

    private void deleteSelectedKey() {
        if (currentSession == null || selectedKeySummary == null) {
            showError("删除失败", "请先选择一个 Key");
            return;
        }
        if (!guardWritableOperation("删除 Key")) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("删除 Key");
        alert.setHeaderText("确认删除当前 Key");
        alert.setContentText(selectedKeySummary.key());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        setInspectorActionsDisabled(true);
        setStatus("正在删除 Key");
        currentSession.deleteKeys(List.of(selectedKeySummary.key()))
            .whenComplete((deletedCount, throwable) -> Platform.runLater(() -> {
                setInspectorActionsDisabled(false);
                if (throwable != null) {
                    setStatus("删除 Key 失败");
                    showError("删除失败", throwable.getMessage());
                    return;
                }
                if (deletedCount == null || deletedCount <= 0) {
                    setStatus("删除 Key 失败");
                    showError("删除失败", "Redis 未删除任何 Key");
                    return;
                }

                setStatus("Key 已删除");
                showInfo("删除成功", "已删除 Key: " + selectedKeySummary.key());
                selectedKeySummary = null;
                selectedKeyType = KeyType.UNKNOWN;
                currentLoadedTtl = -2;
                reloadKeys();
            }));
    }

    private void openCreateConnectionDialog() {
        Dialog<ConnectionProfileDraft> dialog = new Dialog<>();
        dialog.setTitle("新建连接");
        dialog.setHeaderText("创建一个新的 Redis 单机连接配置");

        ButtonType testButtonType = new ButtonType("测试连接", ButtonBar.ButtonData.APPLY);
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(testButtonType, saveButtonType, ButtonType.CANCEL);

        ConnectionFormFields fields = new ConnectionFormFields();
        dialog.getDialogPane().setContent(buildConnectionForm(dialog, fields, null));
        configureTestButton(dialog, testButtonType, fields);

        Optional<ConnectionProfileDraft> result = dialog.showAndWait();
        result.ifPresent(draft -> {
            try {
                connectionProfileService.createProfile(draft);
                refreshConnectionProfiles();
            } catch (RuntimeException exception) {
                showError("保存连接失败", exception.getMessage());
            }
        });
    }

    private void openEditConnectionDialog() {
        ConnectionProfile selectedProfile = selectedConnectionProfile();
        if (selectedProfile == null) {
            showError("编辑连接失败", "请先选择一个已保存连接");
            return;
        }

        Dialog<ConnectionProfileDraft> dialog = new Dialog<>();
        dialog.setTitle("编辑连接");
        dialog.setHeaderText("修改 Redis 连接配置");

        ButtonType testButtonType = new ButtonType("测试连接", ButtonBar.ButtonData.APPLY);
        ButtonType saveButtonType = new ButtonType("保存", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(testButtonType, saveButtonType, ButtonType.CANCEL);

        ConnectionFormFields fields = new ConnectionFormFields();
        dialog.getDialogPane().setContent(buildConnectionForm(dialog, fields, toDraft(selectedProfile)));
        configureTestButton(dialog, testButtonType, fields);

        Optional<ConnectionProfileDraft> result = dialog.showAndWait();
        result.ifPresent(draft -> {
            try {
                connectionProfileService.updateProfile(selectedProfile.id(), draft);
                refreshConnectionProfiles();
                if (currentSession != null && currentSession.profile().id().equals(selectedProfile.id())) {
                    connectionLabel.setText("当前连接：配置已更新，请重新打开连接");
                }
            } catch (RuntimeException exception) {
                showError("更新连接失败", exception.getMessage());
            }
        });
    }

    private Parent buildConnectionForm(Dialog<ConnectionProfileDraft> dialog, ConnectionFormFields fields, ConnectionProfileDraft initialDraft) {
        fields.nameField.setPromptText("例如：开发环境");
        if (initialDraft == null) {
            fields.hostField.setText("127.0.0.1");
            fields.portField.setText("6379");
            fields.databaseField.setText("0");
            fields.timeoutField.setText("3000");
        } else {
            fields.nameField.setText(initialDraft.name());
            fields.hostField.setText(initialDraft.host());
            fields.portField.setText(String.valueOf(initialDraft.port()));
            fields.usernameField.setText(nullToEmpty(initialDraft.username()));
            fields.passwordRefField.setText(nullToEmpty(initialDraft.passwordRef()));
            fields.databaseField.setText(String.valueOf(initialDraft.database()));
            fields.timeoutField.setText(String.valueOf(initialDraft.connectTimeoutMs()));
            fields.tagsField.setText(String.join(", ", initialDraft.tags()));
            fields.sslCheckBox.setSelected(initialDraft.sslEnabled());
            fields.readOnlyCheckBox.setSelected(initialDraft.readOnly());
        }

        GridPane gridPane = new GridPane();
        gridPane.setHgap(12);
        gridPane.setVgap(10);
        gridPane.setPadding(new Insets(12));

        int row = 0;
        gridPane.add(new Label("名称"), 0, row);
        gridPane.add(fields.nameField, 1, row++);
        gridPane.add(new Label("主机"), 0, row);
        gridPane.add(fields.hostField, 1, row++);
        gridPane.add(new Label("端口"), 0, row);
        gridPane.add(fields.portField, 1, row++);
        gridPane.add(new Label("用户名"), 0, row);
        gridPane.add(fields.usernameField, 1, row++);
        gridPane.add(new Label("密码引用"), 0, row);
        gridPane.add(fields.passwordRefField, 1, row++);
        gridPane.add(new Label("数据库"), 0, row);
        gridPane.add(fields.databaseField, 1, row++);
        gridPane.add(new Label("超时(ms)"), 0, row);
        gridPane.add(fields.timeoutField, 1, row++);
        gridPane.add(new Label("标签"), 0, row);
        gridPane.add(fields.tagsField, 1, row++);
        gridPane.add(fields.sslCheckBox, 1, row++);
        gridPane.add(fields.readOnlyCheckBox, 1, row++);
        gridPane.add(fields.statusLabel, 1, row++);
        gridPane.add(fields.progressIndicator, 1, row);

        dialog.setResultConverter(buttonType -> {
            if (buttonType.getButtonData() != ButtonBar.ButtonData.OK_DONE) {
                return null;
            }
            return fields.toDraft();
        });

        return gridPane;
    }

    private void configureTestButton(Dialog<ConnectionProfileDraft> dialog, ButtonType testButtonType, ConnectionFormFields fields) {
        Button testButton = (Button) dialog.getDialogPane().lookupButton(testButtonType);
        testButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            event.consume();
            ConnectionProfileDraft draft = fields.toDraft();
            fields.setTesting(true);

            connectionTestService.test(draft).whenComplete((result, throwable) -> Platform.runLater(() -> {
                fields.setTesting(false);
                if (throwable != null) {
                    fields.statusLabel.setText("测试失败：" + throwable.getMessage());
                    showError("测试连接失败", throwable.getMessage());
                    return;
                }

                updateTestResult(fields, result);
            }));
        });
    }

    private void updateTestResult(ConnectionFormFields fields, ConnectionTestResult result) {
        if (result.success()) {
            fields.statusLabel.setText("连接成功，耗时 " + result.durationMs() + " ms");
            showInfo("连接测试成功", result.message());
            return;
        }

        fields.statusLabel.setText("连接失败，耗时 " + result.durationMs() + " ms");
        showError("连接测试失败", result.message());
    }

    private List<String> splitTags(String tagsText) {
        if (tagsText == null || tagsText.isBlank()) {
            return List.of();
        }

        List<String> tags = new ArrayList<>();
        for (String part : tagsText.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }
        return List.copyOf(tags);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private ConnectionProfile selectedConnectionProfile() {
        ConnectionListItem selectedItem = connectionList.getSelectionModel().getSelectedItem();
        return selectedItem == null ? null : selectedItem.profile();
    }

    private ConnectionProfileDraft toDraft(ConnectionProfile profile) {
        return new ConnectionProfileDraft(
            profile.name(),
            profile.host(),
            profile.port(),
            profile.username(),
            profile.passwordRef(),
            profile.database(),
            profile.sslEnabled(),
            profile.connectTimeoutMs(),
            profile.readOnly(),
            profile.tags()
        );
    }

    private String formatTtl(long ttlSeconds) {
        if (ttlSeconds == -1) {
            return "永久";
        }
        if (ttlSeconds == -2) {
            return "不存在";
        }
        return ttlSeconds + "s";
    }

    private void updateTtlField() {
        if (currentLoadedTtl == -1) {
            ttlField.setText("");
            return;
        }
        if (currentLoadedTtl < 0) {
            ttlField.setText("");
            return;
        }
        ttlField.setText(String.valueOf(currentLoadedTtl));
    }

    private void enableInspectorActions() {
        boolean readOnly = isReadOnlySession();
        ttlField.setDisable(readOnly);
        applyTtlButton.setDisable(readOnly);
        persistTtlButton.setDisable(readOnly);
        deleteKeyButton.setDisable(readOnly);
        updateTtlField();
    }

    private void resetInspectorActions() {
        ttlField.clear();
        ttlField.setDisable(true);
        applyTtlButton.setDisable(true);
        persistTtlButton.setDisable(true);
        deleteKeyButton.setDisable(true);
    }

    private void setInspectorActionsDisabled(boolean disabled) {
        ttlField.setDisable(disabled);
        applyTtlButton.setDisable(disabled);
        persistTtlButton.setDisable(disabled);
        deleteKeyButton.setDisable(disabled);
    }

    private void executeConsoleCommand() {
        if (currentSession == null) {
            showError("命令执行失败", "请先打开一个 Redis 连接");
            return;
        }

        String rawCommand = commandInput.getText();
        CommandRequest request;
        try {
            request = CommandLineParser.parse(rawCommand);
        } catch (IllegalArgumentException exception) {
            showError("命令解析失败", exception.getMessage());
            return;
        }
        if (isReadOnlySession() && isWriteCommand(request.command())) {
            setStatus("已拦截写命令");
            showError("命令执行失败", "当前连接为只读模式，已阻止写命令: " + request.command());
            return;
        }

        if (requiresDangerConfirmation(request.command()) && !confirmDangerousCommand(rawCommand)) {
            return;
        }

        runCommandButton.setDisable(true);
        setStatus("正在执行命令 " + request.command().toUpperCase());
        consoleOutputArea.setText("正在执行命令...");

        currentSession.execute(request).whenComplete((result, throwable) -> Platform.runLater(() -> {
            runCommandButton.setDisable(false);
            if (throwable != null) {
                consoleOutputArea.setText("执行失败: " + throwable.getMessage());
                setStatus("命令执行失败");
                showError("命令执行失败", throwable.getMessage());
                recordCommandHistory(rawCommand, 0, false);
                return;
            }

            recordCommandHistory(rawCommand, result.durationMs(), result.success());
            renderCommandResult(rawCommand, result);
        }));
    }

    private void clearCommandHistory() {
        if (currentSession == null) {
            showError("清空历史失败", "请先打开一个 Redis 连接");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("清空命令历史");
        alert.setHeaderText("确认清空当前连接的命令历史");
        alert.setContentText(currentSession.profile().name());
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        commandHistoryService.clear(currentSession.profile().id());
        loadCommandHistory();
        setStatus("命令历史已清空");
    }

    private void loadCommandHistory() {
        if (currentSession == null) {
            commandHistoryList.getItems().clear();
            return;
        }
        commandHistoryList.getItems().setAll(commandHistoryService.recentCommands(currentSession.profile().id(), 20));
    }

    private void recordCommandHistory(String rawCommand, long durationMs, boolean success) {
        if (currentSession == null || rawCommand == null || rawCommand.isBlank()) {
            return;
        }
        commandHistoryService.record(currentSession.profile().id(), rawCommand.trim(), durationMs, success);
        loadCommandHistory();
    }

    private void renderCommandResult(String rawCommand, CommandResult result) {
        StringBuilder builder = new StringBuilder();
        builder.append("> ").append(rawCommand).append(System.lineSeparator());
        builder.append("耗时: ").append(result.durationMs()).append(" ms").append(System.lineSeparator());
        builder.append(System.lineSeparator());

        if (result.success()) {
            builder.append(result.formattedOutput());
        } else {
            builder.append("ERROR: ").append(result.errorMessage());
        }

        consoleOutputArea.setText(builder.toString());
        setStatus(result.success() ? "命令执行完成" : "命令执行失败");

        String commandName = rawCommand == null ? "" : rawCommand.trim().split("\\s+")[0].toUpperCase();
        if (result.success() && shouldRefreshAfterCommand(commandName)) {
            reloadKeys();
        }
    }

    private boolean requiresDangerConfirmation(String command) {
        String normalized = command.trim().toUpperCase();
        return normalized.equals("DEL")
            || normalized.equals("UNLINK")
            || normalized.equals("FLUSHDB")
            || normalized.equals("FLUSHALL")
            || normalized.equals("RENAME")
            || normalized.equals("RENAMENX");
    }

    private boolean confirmDangerousCommand(String rawCommand) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("危险命令确认");
        alert.setHeaderText("确认执行危险 Redis 命令");
        alert.setContentText(rawCommand);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private boolean shouldRefreshAfterCommand(String command) {
        return command.equals("SET")
            || command.equals("DEL")
            || command.equals("UNLINK")
            || command.equals("HSET")
            || command.equals("HDEL")
            || command.equals("EXPIRE")
            || command.equals("PERSIST")
            || command.equals("RENAME")
            || command.equals("RENAMENX");
    }

    private void setStatus(String message) {
        statusLabel.setText("状态：" + (message == null || message.isBlank() ? "等待操作" : message));
    }

    private void updateSessionMode() {
        if (currentSession == null) {
            sessionModeLabel.setText("模式：未连接");
            return;
        }
        sessionModeLabel.setText(currentSession.profile().readOnly() ? "模式：只读" : "模式：读写");
    }

    private boolean isReadOnlySession() {
        return currentSession != null && currentSession.profile().readOnly();
    }

    private boolean guardWritableOperation(String actionName) {
        if (!isReadOnlySession()) {
            return true;
        }
        setStatus("当前连接为只读");
        showError(actionName + "失败", "当前连接为只读模式，不允许执行写操作");
        return false;
    }

    private boolean isWriteCommand(String command) {
        if (command == null || command.isBlank()) {
            return false;
        }
        return WRITE_COMMANDS.contains(command.trim().toUpperCase());
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private final class ConnectionFormFields {
        private final TextField nameField = new TextField();
        private final TextField hostField = new TextField();
        private final TextField portField = new TextField();
        private final TextField usernameField = new TextField();
        private final TextField passwordRefField = new TextField();
        private final TextField databaseField = new TextField();
        private final TextField timeoutField = new TextField();
        private final TextField tagsField = new TextField();
        private final CheckBox sslCheckBox = new CheckBox("启用 SSL");
        private final CheckBox readOnlyCheckBox = new CheckBox("只读模式");
        private final Label statusLabel = new Label("可以先测试连接，再保存");
        private final ProgressIndicator progressIndicator = new ProgressIndicator();

        private ConnectionFormFields() {
            statusLabel.getStyleClass().add("muted-label");
            progressIndicator.setVisible(false);
            progressIndicator.setPrefSize(18, 18);
        }

        private ConnectionProfileDraft toDraft() {
            return new ConnectionProfileDraft(
                nameField.getText(),
                hostField.getText(),
                parseInt(portField.getText(), 6379),
                usernameField.getText(),
                passwordRefField.getText(),
                parseInt(databaseField.getText(), 0),
                sslCheckBox.isSelected(),
                parseInt(timeoutField.getText(), 3000),
                readOnlyCheckBox.isSelected(),
                splitTags(tagsField.getText())
            );
        }

        private void setTesting(boolean testing) {
            progressIndicator.setVisible(testing);
            statusLabel.setText(testing ? "正在测试连接..." : statusLabel.getText());
        }
    }

    private static final class HashFieldRow {
        private final SimpleStringProperty field;
        private final SimpleStringProperty value;

        private HashFieldRow(String field, String value) {
            this.field = new SimpleStringProperty(field);
            this.value = new SimpleStringProperty(value);
        }

        private String field() {
            return field.get();
        }

        private void setField(String value) {
            field.set(value == null ? "" : value);
        }

        private SimpleStringProperty fieldProperty() {
            return field;
        }

        private String value() {
            return value.get();
        }

        private void setValue(String newValue) {
            value.set(newValue == null ? "" : newValue);
        }

        private SimpleStringProperty valueProperty() {
            return value;
        }
    }

    private static final class CollectionItemRow {
        private final SimpleStringProperty index;
        private final SimpleStringProperty value;
        private final SimpleStringProperty score;

        private CollectionItemRow(String index, String value, String score) {
            this.index = new SimpleStringProperty(index);
            this.value = new SimpleStringProperty(value);
            this.score = new SimpleStringProperty(score);
        }

        private String index() {
            return index.get();
        }

        private void setIndex(String newIndex) {
            index.set(newIndex == null ? "" : newIndex);
        }

        private SimpleStringProperty indexProperty() {
            return index;
        }

        private String value() {
            return value.get();
        }

        private void setValue(String newValue) {
            value.set(newValue == null ? "" : newValue);
        }

        private SimpleStringProperty valueProperty() {
            return value;
        }

        private String score() {
            return score.get();
        }

        private void setScore(String newScore) {
            score.set(newScore == null ? "" : newScore);
        }

        private SimpleStringProperty scoreProperty() {
            return score;
        }
    }

    private record ConnectionListItem(String label, ConnectionProfile profile) {
        private static ConnectionListItem section(String label) {
            return new ConnectionListItem(label, null);
        }

        private static ConnectionListItem placeholder(String label) {
            return new ConnectionListItem(label, null);
        }

        private static ConnectionListItem profile(ConnectionProfile profile) {
            return new ConnectionListItem(profile.name() + " / " + profile.host() + ":" + profile.port(), profile);
        }
    }
}
