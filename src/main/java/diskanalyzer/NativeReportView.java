package diskanalyzer;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.Glow;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class NativeReportView {

    private final StackPane rootStack;
    private final BorderPane contentPane;

    private final Stack<FileNode> history = new Stack<>();
    private FileNode currentNode;
    private final Runnable onBackToHome;
    private final Runnable onOpenSettings;

    private HBox breadcrumbBox;
    private ScrollPane breadcrumbScroll;

    private Button btnUp;
    private Button btnToggleSidebar;
    private Button btnSettings;

    private Button btnViewMode;
    private boolean isCategoryMode = false;

    private PieChart pieChart;
    private ListView<FileNode> listView;
    private Label centerSizeLabel;
    private Label centerTextLabel;

    private VBox floatingInfoBox;
    private Label floatName, floatSize, floatPercent;

    private VBox sidebar;
    private boolean isSidebarOpen = true;
    private final double SIDEBAR_WIDTH = 420;

    private double xOffset = 0;
    private double yOffset = 0;

    private static final PseudoClass CHART_HOVER_PSEUDO_CLASS = PseudoClass.getPseudoClass("chart-hover");

    public NativeReportView(FileNode rootNode, Runnable onBackToHome, Runnable onOpenSettings) {
        this.currentNode = rootNode;
        this.onBackToHome = onBackToHome;
        this.onOpenSettings = onOpenSettings;

        this.rootStack = new StackPane();
        this.contentPane = new BorderPane();

        initUI();
        initFloatingInfo();

        rootStack.getChildren().addAll(contentPane, floatingInfoBox);
        render(currentNode);
    }

    public Node getView() {
        return rootStack;
    }

    private void initFloatingInfo() {
        floatName = new Label();
        floatName.setStyle("-fx-text-fill: -fx-primary; -fx-font-weight: bold; -fx-font-size: 14px;");
        floatName.setMinWidth(Region.USE_PREF_SIZE);
        floatName.setMaxWidth(Double.MAX_VALUE);
        floatName.setWrapText(false);

        floatSize = new Label();
        floatSize.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-family: 'Consolas'; -fx-font-size: 13px;");
        floatSize.setMinWidth(Region.USE_PREF_SIZE);

        floatPercent = new Label();
        floatPercent.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px;");
        floatPercent.setMinWidth(Region.USE_PREF_SIZE);

        floatingInfoBox = new VBox(4, floatName, floatSize, floatPercent);

        floatingInfoBox.setStyle("""
            -fx-background-color: #000000; 
            -fx-background-radius: 10; 
            -fx-border-color: -fx-color-border; 
            -fx-border-radius: 10; 
            -fx-border-width: 1;
            -fx-padding: 12 16;
        """);

        floatingInfoBox.setEffect(new DropShadow(15, Color.BLACK));
        floatingInfoBox.setMouseTransparent(true);
        floatingInfoBox.setVisible(false);
        floatingInfoBox.setManaged(false);
        floatingInfoBox.setMaxWidth(Double.MAX_VALUE);
        floatingInfoBox.setPrefWidth(Region.USE_COMPUTED_SIZE);
    }

    private void showFloatingInfo(FileNode item) {
        floatName.setText(item.name);
        floatSize.setText(FileNode.formatSize(item.size));
        double p = (double)item.size / currentNode.size * 100.0;
        floatPercent.setText(String.format("%.2f%%", p));
        floatingInfoBox.autosize();
        floatingInfoBox.setVisible(true);
        floatingInfoBox.toFront();
    }

    private void updateFloatingPos(double screenX, double screenY) {
        javafx.geometry.Point2D local = rootStack.screenToLocal(screenX, screenY);
        if (local != null) {
            double x = local.getX() + 15;
            double y = local.getY() + 15;
            if (x + floatingInfoBox.getWidth() > rootStack.getWidth()) x = local.getX() - floatingInfoBox.getWidth() - 10;
            if (y + floatingInfoBox.getHeight() > rootStack.getHeight()) y = local.getY() - floatingInfoBox.getHeight() - 10;
            floatingInfoBox.setLayoutX(x);
            floatingInfoBox.setLayoutY(y);
        }
    }

    private void initUI() {
        HBox topBar = new HBox(10);
        topBar.setPadding(new Insets(10, 20, 10, 20));
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setStyle("-fx-background-color: transparent; -fx-border-color: -fx-color-border; -fx-border-width: 0 0 1 0;");

        Button btnHome = createSmallButton("🏠");
        btnHome.setTooltip(new Tooltip("新扫描"));
        btnHome.setOnAction(e -> onBackToHome.run());

        btnUp = createSmallButton("⬆");
        btnUp.setTooltip(new Tooltip("上一级"));
        btnUp.setOnAction(e -> goUp());

        btnSettings = createSmallButton("⚙");
        btnSettings.setTooltip(new Tooltip("设置"));
        btnSettings.setOnAction(e -> onOpenSettings.run());

        btnViewMode = createSmallButton("📂 目录");
        btnViewMode.setTooltip(new Tooltip("切换视图模式"));
        btnViewMode.setPrefWidth(100);
        btnViewMode.setOnAction(e -> toggleViewMode());

        breadcrumbBox = new HBox(2);
        breadcrumbBox.setAlignment(Pos.CENTER_LEFT);

        breadcrumbScroll = new ScrollPane(breadcrumbBox);
        breadcrumbScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        breadcrumbScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        breadcrumbScroll.setFitToHeight(true);
        breadcrumbScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-control-inner-background: transparent; -fx-background-insets: 0;");
        breadcrumbScroll.setPannable(true);
        breadcrumbScroll.getStyleClass().add("edge-to-edge");
        HBox.setHgrow(breadcrumbScroll, Priority.ALWAYS);

        btnToggleSidebar = createSmallButton("☰");
        btnToggleSidebar.setOnAction(e -> toggleSidebar());

        topBar.getChildren().addAll(btnHome, btnSettings, btnUp, btnViewMode, breadcrumbScroll, btnToggleSidebar);
        contentPane.setTop(topBar);

        listView = new ListView<>();
        listView.setCellFactory(param -> new FileListCell());
        listView.setStyle("-fx-background-color: transparent;");

        listView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                FileNode selected = listView.getSelectionModel().getSelectedItem();
                if (!isCategoryMode && selected != null && selected.isDir && !selected.isOther) {
                    drillDown(selected);
                }
            }
        });

        listView.addEventHandler(MouseEvent.MOUSE_MOVED, e -> updateFloatingPos(e.getScreenX(), e.getScreenY()));

        sidebar = new VBox(listView);
        sidebar.setPrefWidth(SIDEBAR_WIDTH);
        sidebar.setMinWidth(0);
        sidebar.setStyle("-fx-background-color: rgba(0,0,0,0.2); -fx-border-color: -fx-color-border; -fx-border-width: 0 0 0 1;");
        VBox.setVgrow(listView, Priority.ALWAYS);
        contentPane.setRight(sidebar);

        StackPane chartPane = new StackPane();
        chartPane.setPadding(new Insets(20));
        pieChart = new PieChart();
        pieChart.setLegendVisible(false);
        pieChart.setLabelsVisible(false);
        pieChart.setStartAngle(90);

        Circle hole = new Circle(100);
        hole.setStyle("-fx-fill: rgba(0,0,0);");

        VBox centerText = new VBox(5);
        centerText.setAlignment(Pos.CENTER);
        centerText.setMouseTransparent(true);
        centerSizeLabel = new Label();
        centerSizeLabel.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 28px; -fx-font-weight: bold;");
        centerTextLabel = new Label("总大小");
        centerTextLabel.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px; -fx-text-transform: uppercase;");
        centerText.getChildren().addAll(centerSizeLabel, centerTextLabel);

        chartPane.getChildren().addAll(pieChart, hole, centerText);
        contentPane.setCenter(chartPane);
    }

    private void toggleSidebar() {
        isSidebarOpen = !isSidebarOpen;
        Timeline timeline = new Timeline();
        double targetWidth = isSidebarOpen ? SIDEBAR_WIDTH : 0;
        KeyValue kv = new KeyValue(sidebar.prefWidthProperty(), targetWidth, javafx.animation.Interpolator.EASE_BOTH);
        KeyFrame kf = new KeyFrame(Duration.millis(300), kv);
        timeline.getKeyFrames().add(kf);
        timeline.play();
    }

    private void toggleViewMode() {
        isCategoryMode = !isCategoryMode;
        btnViewMode.setText(isCategoryMode ? "📊 类型" : "📂 目录");
        render(currentNode);
    }

    private void render(FileNode node) {
        this.currentNode = node;
        updateBreadcrumbs(node);
        btnUp.setDisable(history.isEmpty());
        centerSizeLabel.setText(FileNode.formatSize(node.size));
        List<String> chartColors = MainApp.currentPalette != null ? MainApp.currentPalette.chartColors : null;

        List<FileNode> displayNodes;
        if (isCategoryMode) {
            displayNodes = generateCategoryStats(node);
        } else {
            displayNodes = node.children;
        }

        pieChart.getData().clear();
        for (int i = 0; i < displayNodes.size(); i++) {
            FileNode child = displayNodes.get(i);
            PieChart.Data data = new PieChart.Data(child.name, child.size);
            pieChart.getData().add(data);

            String color;
            if (child.isOther) color = "#52525B";
            else if (chartColors != null && !chartColors.isEmpty()) color = chartColors.get(i % chartColors.size());
            else color = "#D9E878";

            Node sliceNode = data.getNode();
            sliceNode.setStyle("-fx-pie-color: " + color + "; -fx-border-color: -fx-bg-base; -fx-border-width: 2px;");

            int index = i;

            sliceNode.setOnMouseEntered(e -> {
                highlightSlice(sliceNode, true);
                listView.getSelectionModel().select(index);
                listView.scrollTo(index);
                showFloatingInfo(child);
            });

            sliceNode.setOnMouseExited(e -> {
                highlightSlice(sliceNode, false);
                listView.getSelectionModel().clearSelection();
                floatingInfoBox.setVisible(false);
            });

            sliceNode.setOnMouseMoved(e -> updateFloatingPos(e.getScreenX(), e.getScreenY()));

            sliceNode.setOnMouseClicked(e -> {
                if (e.getButton() == MouseButton.PRIMARY) {
                    if (!isCategoryMode && child.isDir && !child.isOther) {
                        drillDown(child);
                    }
                }
            });

            ContextMenu contextMenu = createContextMenu(child);
            sliceNode.setOnContextMenuRequested(e -> {
                contextMenu.show(sliceNode, e.getScreenX(), e.getScreenY());
            });
        }
        listView.setItems(FXCollections.observableArrayList(displayNodes));
    }

    private List<FileNode> generateCategoryStats(FileNode root) {
        Map<String, Long> categorySizes = new HashMap<>();
        aggregateFileStats(root, categorySizes);

        List<FileNode> result = new ArrayList<>();
        for (Map.Entry<String, Long> entry : categorySizes.entrySet()) {
            FileNode catNode = new FileNode(entry.getKey(), root.path, false);
            catNode.size = entry.getValue();
            result.add(catNode);
        }

        result.sort((a, b) -> Long.compare(b.size, a.size));
        return result;
    }

    private void aggregateFileStats(FileNode node, Map<String, Long> stats) {
        if (node.isDir) {
            for (FileNode child : node.children) {
                if (!child.name.equals("[Other Files]")) {
                    aggregateFileStats(child, stats);
                }
            }
        } else {
            String cat = getCategory(node.name);
            stats.put(cat, stats.getOrDefault(cat, 0L) + node.size);
        }
    }

    // ★★★ 汉化后的文件分类 ★★★
    private String getCategory(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex == -1) return "未知类型";
        String ext = filename.substring(dotIndex + 1).toLowerCase();

        // 1. 先查用户自定义
        for (Map.Entry<String, List<String>> entry : MainApp.FILE_CATEGORIES.entrySet()) {
            if (entry.getValue().contains(ext)) {
                return entry.getKey();
            }
        }

        // 2. 默认分类 (中文)
        return switch (ext) {
            case "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "3gp" -> "视频";
            case "jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff", "heic", "raw" -> "图片";
            case "mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "aiff", "mid" -> "音频";
            case "doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx", "md", "csv", "rtf", "odt" -> "文档";
            case "zip", "rar", "7z", "tar", "gz", "iso", "xz", "bz2", "jar", "war" -> "压缩包";
            case "java", "c", "cpp", "py", "js", "html", "css", "json", "xml", "php", "sql", "sh", "bat", "kt", "rs", "go", "ts" -> "代码/脚本";
            case "exe", "msi", "app", "dmg", "apk" -> "可执行程序";
            case "sys", "dll", "ini", "cfg", "log" -> "系统/配置";
            default -> "其他文件";
        };
    }

    private void updateBreadcrumbs(FileNode node) {
        breadcrumbBox.getChildren().clear();

        List<FileNode> chain = new ArrayList<>();
        FileNode temp = node;
        while (temp != null) {
            chain.add(0, temp);
            temp = temp.parent;
        }

        for (int i = 0; i < chain.size(); i++) {
            FileNode n = chain.get(i);
            boolean isLast = (i == chain.size() - 1);

            Button b = new Button(n.name);
            b.setStyle(isLast
                    ? "-fx-background-color: transparent; -fx-text-fill: -fx-primary; -fx-font-weight: bold; -fx-font-size: 14px;"
                    : "-fx-background-color: transparent; -fx-text-fill: -fx-text-secondary; -fx-font-size: 14px; -fx-cursor: hand;");

            if (!isLast) {
                b.setOnMouseEntered(e -> b.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-text-fill: -fx-primary; -fx-font-size: 14px; -fx-background-radius: 4; -fx-cursor: hand;"));
                b.setOnMouseExited(e -> b.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-text-secondary; -fx-font-size: 14px; -fx-cursor: hand;"));
                b.setOnAction(e -> jumpToNode(n));
            }

            breadcrumbBox.getChildren().add(b);

            if (!isLast) {
                Label sep = new Label("›");
                sep.getStyleClass().add("breadcrumb-sep");
                breadcrumbBox.getChildren().add(sep);
            }
        }

        breadcrumbScroll.setHvalue(1.0);
    }

    private void jumpToNode(FileNode target) {
        if (target == currentNode) return;

        history.clear();
        Stack<FileNode> tempStack = new Stack<>();
        FileNode p = target.parent;
        while(p != null) {
            tempStack.push(p);
            p = p.parent;
        }
        while(!tempStack.isEmpty()) {
            history.push(tempStack.pop());
        }

        render(target);
    }

    private void highlightSlice(Node node, boolean active) {
        ScaleTransition st = new ScaleTransition(Duration.millis(150), node);
        st.setToX(active ? 1.1 : 1.0);
        st.setToY(active ? 1.1 : 1.0);
        st.play();
        node.setEffect(active ? new Glow(0.5) : null);
    }

    private void highlightChartFromList(int index, boolean active) {
        if (index >= 0 && index < pieChart.getData().size()) {
            Node node = pieChart.getData().get(index).getNode();
            highlightSlice(node, active);
        }
    }

    private void drillDown(FileNode node) {
        history.push(currentNode);
        render(node);
    }
    private void goUp() { if (!history.isEmpty()) render(history.pop()); }

    private Button createSmallButton(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("btn-secondary");
        return btn;
    }

    private ContextMenu createContextMenu(FileNode node) {
        ContextMenu cm = new ContextMenu();

        boolean isRealNode = !isCategoryMode && !node.name.equals("[Other Files]");

        if (isRealNode) {
            MenuItem openItem = new MenuItem("📂 在资源管理器中打开");
            openItem.setOnAction(e -> openInExplorer(node));
            cm.getItems().add(openItem);
        }

        MenuItem detailsItem = new MenuItem("🛈 查看详细信息");
        detailsItem.setOnAction(e -> showDetailsDialog(node));
        cm.getItems().add(detailsItem);

        if (isRealNode) {
            MenuItem deleteItem = new MenuItem("🗑 删除");
            deleteItem.setStyle("-fx-text-fill: #FF6B6B;");
            deleteItem.setOnAction(e -> confirmAndDelete(node));
            cm.getItems().add(new SeparatorMenuItem());
            cm.getItems().add(deleteItem);
        }

        return cm;
    }

    private void openInExplorer(FileNode node) {
        try {
            File file = new File(node.path);
            if (!file.exists()) return;

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE_FILE_DIR)) {
                    desktop.browseFileDirectory(file);
                    return;
                }
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(file.isDirectory() ? file : file.getParentFile());
                }
            }
        } catch (Exception e) {
            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("explorer /select," + node.path);
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open -R " + node.path);
                }
            } catch (IOException ignored) {}
        }
    }

    private void confirmAndDelete(FileNode node) {
        Stage dialog = new Stage();
        dialog.initOwner(rootStack.getScene().getWindow());
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_LEFT);
        root.getStyleClass().add("glass-dialog");

        if (MainApp.currentPalette != null) {
            ThemeEngine.Palette p = MainApp.currentPalette;
            int r = (int)(p.surface.getRed()*255);
            int g = (int)(p.surface.getGreen()*255);
            int b = (int)(p.surface.getBlue()*255);
            String surfaceRgb = r + "," + g + "," + b;

            String style = String.format(
                    "-fx-bg-surface-solid: rgb(%s); " +
                            "-fx-bg-surface: rgba(%s, 0.95); " +
                            "-fx-text-primary: %s; " +
                            "-fx-text-secondary: %s; " +
                            "-fx-primary: %s; " +
                            "-fx-color-border: rgba(255,255,255,0.1);",
                    surfaceRgb, surfaceRgb,
                    ThemeEngine.toHex(p.textPrimary),
                    ThemeEngine.toHex(p.textSecondary),
                    ThemeEngine.toHex(p.primary)
            );
            root.setStyle(style);
        }

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            dialog.setX(event.getScreenX() - xOffset);
            dialog.setY(event.getScreenY() - yOffset);
        });

        Label titleLabel = new Label("删除 " + (node.isDir ? "文件夹" : "文件") + "?");
        titleLabel.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 20px; -fx-font-weight: bold;");

        VBox msgBox = new VBox(10);
        Label msg1 = new Label("确定要永久删除：");
        msg1.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 14px;");

        Label pathLabel = new Label(node.name);
        pathLabel.setWrapText(true);
        pathLabel.setMaxWidth(350);
        pathLabel.setStyle("-fx-text-fill: -fx-primary; -fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 5 10; -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 5;");

        Label msg2 = new Label("此操作无法撤销。");
        msg2.setStyle("-fx-text-fill: #FF6B6B; -fx-font-size: 12px; -fx-font-weight: bold;");

        msgBox.getChildren().addAll(msg1, pathLabel, msg2);

        HBox btnBox = new HBox(15);
        btnBox.setAlignment(Pos.CENTER_RIGHT);
        btnBox.setPadding(new Insets(10, 0, 0, 0));

        Button btnCancel = new Button("取消");
        btnCancel.getStyleClass().add("btn-secondary");
        btnCancel.setPrefWidth(80);
        btnCancel.setOnAction(e -> dialog.close());

        Button btnDelete = new Button("删除");
        btnDelete.setStyle("""
            -fx-background-color: #D32F2F; 
            -fx-text-fill: white; 
            -fx-font-weight: bold; 
            -fx-background-radius: 6;
            -fx-cursor: hand;
        """);
        btnDelete.setOnMouseEntered(e -> btnDelete.setStyle("-fx-background-color: #B71C1C; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;"));
        btnDelete.setOnMouseExited(e -> btnDelete.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;"));

        btnDelete.setPrefWidth(80);
        btnDelete.setOnAction(e -> {
            performDelete(node);
            dialog.close();
        });

        btnBox.getChildren().addAll(btnCancel, btnDelete);

        root.getChildren().addAll(titleLabel, msgBox, btnBox);

        Scene scene = new Scene(root);
        if (rootStack.getScene() != null) {
            scene.getStylesheets().setAll(rootStack.getScene().getStylesheets());
        }
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.show();

        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), root);
        st.setFromX(0.9); st.setFromY(0.9); st.setToX(1); st.setToY(1); st.play();
    }

    private void performDelete(FileNode node) {
        try {
            Path path = Path.of(node.path);
            if (node.isDir) {
                try (Stream<Path> walk = Files.walk(path)) {
                    walk.sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                }
            } else {
                Files.delete(path);
            }
            updateTreeAfterDelete(node);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateTreeAfterDelete(FileNode deletedNode) {
        FileNode parent = deletedNode.parent;
        if (parent != null) {
            parent.children.remove(deletedNode);
            long sizeToRemove = deletedNode.size;
            FileNode p = parent;
            while (p != null) {
                p.size -= sizeToRemove;
                p = p.parent;
            }
            render(currentNode);
        } else {
            onBackToHome.run();
        }
    }

    private void showDetailsDialog(FileNode node) {
        Stage dialog = new Stage();
        dialog.initOwner(rootStack.getScene().getWindow());
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);

        String createdTime = "未知";
        String lastAccess = "未知";
        String lastModified = "未知";
        String isHidden = "否";
        String canRead = "否";
        String canWrite = "否";
        String canExec = "否";
        String type = node.isDir ? "文件夹" : "文件";

        if (!node.isOther) {
            try {
                Path path = Path.of(node.path);
                File file = path.toFile();
                BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                createdTime = sdf.format(new Date(attrs.creationTime().toMillis()));
                lastAccess = sdf.format(new Date(attrs.lastAccessTime().toMillis()));
                lastModified = sdf.format(new Date(attrs.lastModifiedTime().toMillis()));
                isHidden = file.isHidden() ? "是" : "否";
                canRead = file.canRead() ? "是" : "否";
                canWrite = file.canWrite() ? "是" : "否";
                canExec = file.canExecute() ? "是" : "否";
                String mime = Files.probeContentType(path);
                if (mime != null) type = mime;
                else if (node.isDir) type = "文件夹";
                else {
                    int dotIndex = node.name.lastIndexOf('.');
                    if (dotIndex > 0) type = node.name.substring(dotIndex + 1).toUpperCase() + " 文件";
                }
            } catch (IOException e) { }
        }

        VBox root = new VBox(20);
        root.setPadding(new Insets(30));
        root.getStyleClass().add("details-window");

        if (MainApp.currentPalette != null) {
            ThemeEngine.Palette p = MainApp.currentPalette;
            int r = (int)(p.surface.getRed()*255);
            int g = (int)(p.surface.getGreen()*255);
            int b = (int)(p.surface.getBlue()*255);
            String surfaceRgb = r + "," + g + "," + b;

            String style = String.format(
                    "-fx-primary: %s; -fx-on-primary: %s; -fx-bg-base: %s; -fx-bg-surface: rgba(%s, 0.95); -fx-bg-surface-solid: rgb(%s); -fx-text-primary: %s; -fx-text-secondary: %s; -fx-selection-bg: rgba(%s, 0.15); -fx-color-border: rgba(255,255,255,0.1);",
                    ThemeEngine.toHex(p.primary), ThemeEngine.toHex(p.onPrimary), ThemeEngine.toHex(p.background), surfaceRgb, surfaceRgb, ThemeEngine.toHex(p.textPrimary), ThemeEngine.toHex(p.textSecondary), ThemeEngine.toHex(p.primary)
            );
            root.setStyle(style);
        }

        root.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        root.setOnMouseDragged(event -> {
            dialog.setX(event.getScreenX() - xOffset);
            dialog.setY(event.getScreenY() - yOffset);
        });

        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setMouseTransparent(true);

        Label iconLabel = new Label(node.isDir ? "📂" : "📄");
        iconLabel.setStyle("-fx-font-size: 40px;");

        VBox titleBox = new VBox(5);
        Label nameLabel = new Label(node.name);
        nameLabel.getStyleClass().add("details-title");
        Label typeLabel = new Label(type);
        typeLabel.getStyleClass().add("details-subtitle");
        titleBox.getChildren().addAll(nameLabel, typeLabel);

        header.getChildren().addAll(iconLabel, titleBox);

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(12);
        grid.setMouseTransparent(true);

        int row = 0;
        addDetailRow(grid, row++, "位置:", node.path);
        addDetailRow(grid, row++, "大小:", FileNode.formatSize(node.size) + " (" + String.format("%,d", node.size) + " 字节)");
        if (node.isDir) {
            addDetailRow(grid, row++, "包含:", node.children.size() + " 项");
        }

        Region line1 = new Region();
        line1.getStyleClass().add("details-separator");
        GridPane.setColumnSpan(line1, 2);
        grid.add(line1, 0, row++);

        addDetailRow(grid, row++, "创建时间:", createdTime);
        addDetailRow(grid, row++, "修改时间:", lastModified);
        addDetailRow(grid, row++, "访问时间:", lastAccess);

        Region line2 = new Region();
        line2.getStyleClass().add("details-separator");
        GridPane.setColumnSpan(line2, 2);
        grid.add(line2, 0, row++);

        addDetailRow(grid, row++, "属性:", "隐藏: " + isHidden);
        addDetailRow(grid, row++, "权限:", String.format("读:%s  写:%s  执行:%s", canRead, canWrite, canExec));

        Button btnClose = new Button("关闭");
        btnClose.getStyleClass().add("btn-primary");
        btnClose.setPrefWidth(100);
        btnClose.setOnAction(e -> dialog.close());

        HBox btnBox = new HBox(btnClose);
        btnBox.setAlignment(Pos.CENTER_RIGHT);

        root.getChildren().addAll(header, grid, btnBox);

        Scene scene = new Scene(root);
        if (rootStack.getScene() != null) {
            scene.getStylesheets().setAll(rootStack.getScene().getStylesheets());
        }
        scene.setFill(Color.TRANSPARENT);

        dialog.setScene(scene);
        dialog.show();

        FadeTransition ft = new FadeTransition(Duration.millis(200), root);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
        ScaleTransition st = new ScaleTransition(Duration.millis(200), root);
        st.setFromX(0.9); st.setFromY(0.9); st.setToX(1); st.setToY(1); st.play();
    }

    private void addDetailRow(GridPane grid, int row, String label, String value) {
        Label l = new Label(label);
        l.getStyleClass().add("details-label");
        l.setMinWidth(80);

        Label v = new Label(value);
        v.getStyleClass().add("details-value");
        v.setWrapText(true);
        v.setMaxWidth(350);

        grid.add(l, 0, row);
        grid.add(v, 1, row);
    }

    private class FileListCell extends ListCell<FileNode> {
        private final HBox root;
        private final Circle dot;
        private final Label nameLabel;
        private final Label sizeLabel;
        private final Label percentLabel;
        private final Region spacer;

        public FileListCell() {
            root = new HBox(10);
            root.setAlignment(Pos.CENTER_LEFT);
            root.setPadding(new Insets(8, 12, 8, 12));

            this.setOnMouseEntered(e -> {
                if (!isEmpty() && getItem() != null) {
                    highlightChartFromList(getIndex(), true);
                    showFloatingInfo(getItem());
                    pseudoClassStateChanged(CHART_HOVER_PSEUDO_CLASS, true);
                }
            });
            this.setOnMouseExited(e -> {
                if (!isEmpty()) {
                    highlightChartFromList(getIndex(), false);
                    floatingInfoBox.setVisible(false);
                    pseudoClassStateChanged(CHART_HOVER_PSEUDO_CLASS, false);
                }
            });
            this.setOnMouseMoved(e -> {
                if (!isEmpty() && floatingInfoBox.isVisible()) {
                    updateFloatingPos(e.getScreenX(), e.getScreenY());
                }
            });

            dot = new Circle(4);
            nameLabel = new Label();
            nameLabel.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-weight: bold;");
            nameLabel.setMaxWidth(200);
            nameLabel.setTextOverrun(OverrunStyle.ELLIPSIS);

            spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            sizeLabel = new Label();
            sizeLabel.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-family: 'Consolas'; -fx-font-weight: bold;");
            sizeLabel.setMinWidth(Region.USE_PREF_SIZE);

            percentLabel = new Label();
            percentLabel.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 11px;");
            percentLabel.setPrefWidth(45);
            percentLabel.setMinWidth(Region.USE_PREF_SIZE);
            percentLabel.setAlignment(Pos.CENTER_RIGHT);

            root.getChildren().addAll(dot, nameLabel, spacer, sizeLabel, percentLabel);
        }

        @Override
        protected void updateItem(FileNode item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) {
                setGraphic(null);
                setContextMenu(null);
                pseudoClassStateChanged(CHART_HOVER_PSEUDO_CLASS, false);
            } else {
                nameLabel.setText(item.name);
                sizeLabel.setText(FileNode.formatSize(item.size));

                String colorHex;
                if (item.isOther) colorHex = "#52525B";
                else if (isCategoryMode) {
                    List<String> chartColors = MainApp.currentPalette != null ? MainApp.currentPalette.chartColors : null;
                    if (chartColors != null) colorHex = chartColors.get(Math.abs(item.name.hashCode()) % chartColors.size());
                    else colorHex = "#D9E878";
                } else {
                    List<String> chartColors = MainApp.currentPalette != null ? MainApp.currentPalette.chartColors : null;
                    int index = getListView().getItems().indexOf(item);
                    if (chartColors != null && !chartColors.isEmpty()) {
                        colorHex = chartColors.get(index % chartColors.size());
                    } else {
                        colorHex = "#D9E878";
                    }
                }

                dot.setStyle("-fx-fill: " + colorHex);
                double percent = (double) item.size / currentNode.size * 100.0;
                percentLabel.setText(String.format("%.1f%%", percent));

                if (isSelected()) {
                    setStyle("-fx-background-color: rgba(255, 255, 255, 0.15);");
                } else {
                    setStyle("-fx-background-color: transparent;");
                }

                setContextMenu(createContextMenu(item));

                setGraphic(root);
            }
        }
    }
}