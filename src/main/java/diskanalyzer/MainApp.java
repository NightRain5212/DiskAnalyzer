package diskanalyzer;

import javafx.animation.*;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

public class MainApp extends Application {

    private Stage primaryStage;
    private StackPane rootContainer;

    private Image currentBgImage;
    private ThemeStyle currentStyle = ThemeStyle.AUTO;

    public static ThemeEngine.Palette currentPalette;

    // 全局文件分类配置
    public static final Map<String, List<String>> FILE_CATEGORIES = new LinkedHashMap<>();

    static {
        FILE_CATEGORIES.put("视频", new ArrayList<>(List.of("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v")));
        FILE_CATEGORIES.put("图片", new ArrayList<>(List.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "ico", "tiff")));
        FILE_CATEGORIES.put("音频", new ArrayList<>(List.of("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a")));
        FILE_CATEGORIES.put("文档", new ArrayList<>(List.of("doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx", "md", "csv")));
        FILE_CATEGORIES.put("压缩包", new ArrayList<>(List.of("zip", "rar", "7z", "tar", "gz", "iso", "jar")));
        FILE_CATEGORIES.put("代码", new ArrayList<>(List.of("java", "c", "cpp", "py", "js", "html", "css", "json", "xml", "php")));
        FILE_CATEGORIES.put("程序", new ArrayList<>(List.of("exe", "msi", "app", "dmg", "apk", "bat", "sh")));
    }

    @Override
    public void start(Stage stage) {
        System.setProperty("prism.order", "sw");
        System.setProperty("prism.text", "t2k");

        this.primaryStage = stage;

        // 1. 显示启动动画
        showSplashScreen();
    }

    // --- 启动动画 (优化版：渐变背景、丝滑过渡) ---
    private void showSplashScreen() {
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.TRANSPARENT);

        StackPane splashRoot = new StackPane();
        splashRoot.setPrefSize(600, 400);

        // 加载壁纸尝试
        Image bgImage = null;
        try {
            bgImage = new Image(getClass().getResourceAsStream("/background.png"));
        } catch (Exception e) { }

        // ★★★ 修改 1: 背景处理 ★★★
        if (bgImage != null) {
            // 如果有壁纸，显示壁纸
            BackgroundImage bg = new BackgroundImage(
                    bgImage, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER, new BackgroundSize(1.0, 1.0, true, true, false, true)
            );
            splashRoot.setBackground(new Background(bg));
        } else {
            // 如果没有壁纸，使用高级的深色线性渐变 (Deep Space Gradient)
            // 从深灰蓝 (#141E30) 到 深金属蓝 (#243B55)
            splashRoot.setStyle("-fx-background-color: linear-gradient(to bottom right, #141E30, #243B55);");
        }

        // 内容层
        VBox contentBox = new VBox(15);
        contentBox.setAlignment(Pos.CENTER);
        contentBox.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // ★★★ 修改 2: 内容遮罩改为渐变，增加纵深感 ★★★
        // 从上部的微透明(0.3) 到 下部的较深色(0.7)，比纯色遮罩更有质感
        contentBox.setStyle("-fx-background-color: linear-gradient(to bottom, rgba(0,0,0,0.3), rgba(0,0,0,0.7));");

        Label logo = new Label("Disk Analyzer");
        logo.setStyle("-fx-text-fill: white; -fx-font-family: 'Segoe UI'; -fx-font-size: 42px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, rgba(255,255,255,0.3), 15, 0.3, 0, 0);");

        Label version = new Label("v.1.1.0");
        version.setStyle("-fx-text-fill: rgba(255,255,255,0.6); -fx-font-size: 14px; -fx-letter-spacing: 3px;");

        contentBox.getChildren().addAll(logo, version);
        splashRoot.getChildren().add(contentBox);

        // 圆形遮罩动画
        Circle clip = new Circle(0);
        clip.setCenterX(300);
        clip.setCenterY(200);
        splashRoot.setClip(clip);

        Scene scene = new Scene(splashRoot, Color.TRANSPARENT);
        splashStage.setScene(scene);
        splashStage.centerOnScreen();
        splashStage.show();

        // 1. 扩散动画 (使用 EASE_OUT 显得更轻快)
        double maxRadius = Math.sqrt(300*300 + 200*200) + 50;
        Timeline spreadAnim = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(clip.radiusProperty(), 0)),
                new KeyFrame(Duration.millis(1100), new KeyValue(clip.radiusProperty(), maxRadius, Interpolator.EASE_OUT))
        );

        // 2. 文字上浮淡入
        logo.setTranslateY(30); logo.setOpacity(0);
        version.setOpacity(0);

        Timeline contentAnim = new Timeline(
                new KeyFrame(Duration.millis(300),
                        new KeyValue(logo.translateYProperty(), 30),
                        new KeyValue(logo.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(logo.translateYProperty(), 0, Interpolator.EASE_OUT),
                        new KeyValue(logo.opacityProperty(), 1),
                        new KeyValue(version.opacityProperty(), 1)
                )
        );

        // 3. 淡出并进入主界面
        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), splashRoot);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(1800));

        fadeOut.setOnFinished(e -> {
            splashStage.close();
            initMainInterface();
        });

        spreadAnim.play();
        contentAnim.play();
        fadeOut.play();
    }

    // --- 主程序初始化 ---
    private void initMainInterface() {
        this.rootContainer = new StackPane();

        try {
            currentBgImage = new Image(getClass().getResourceAsStream("/background.png"));
            updateWallpaperAndTheme(currentBgImage);
        } catch (Exception e) {
            updateWallpaperAndTheme(null);
        }

        Scene scene = new Scene(rootContainer, 1024, 768);
        String cssData = "data:text/css;base64," + Base64.getEncoder().encodeToString(GLOBAL_CSS.getBytes(StandardCharsets.UTF_8));
        scene.getStylesheets().add(cssData);

        primaryStage.setTitle("Disk Analyzer v1.1.0");
        primaryStage.setScene(scene);

        // 默认最大化
        primaryStage.setMaximized(true);
        primaryStage.show();

        // 主界面淡入
        rootContainer.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(600), rootContainer);
        ft.setToValue(1);
        ft.play();

        showHomeView();
    }

    public void updateWallpaperAndTheme(Image image) {
        this.currentBgImage = image;
        if (image != null) {
            BackgroundImage bg = new BackgroundImage(
                    image, BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                    BackgroundPosition.CENTER, new BackgroundSize(1.0, 1.0, true, true, false, true)
            );
            rootContainer.setBackground(new Background(bg));
        } else {
            rootContainer.setBackground(new Background(new BackgroundFill(Color.web("#121212"), null, null)));
        }
        Color dominant = ThemeEngine.extractDominantColor(image);
        ThemeEngine.Palette palette = ThemeEngine.generatePalette(dominant, currentStyle, true);
        applyPalette(palette);
    }

    public void updateThemeStyle(ThemeStyle style) {
        this.currentStyle = style;
        Color dominant = ThemeEngine.extractDominantColor(currentBgImage);
        ThemeEngine.Palette palette = ThemeEngine.generatePalette(dominant, currentStyle, true);
        applyPalette(palette);
    }

    private void applyPalette(ThemeEngine.Palette p) {
        currentPalette = p;
        String surfaceRgba = toRgba(p.surface, 0.85);
        String primaryRgba = toRgba(p.primary, 0.15);

        String style = String.format(
                "-fx-primary: %s; -fx-on-primary: %s; -fx-primary-container: %s; -fx-on-primary-container: %s; " +
                        "-fx-secondary: %s; -fx-on-secondary: %s; -fx-secondary-container: %s; " +
                        "-fx-tertiary: %s; -fx-on-tertiary: %s; " +
                        "-fx-bg-base: %s; -fx-on-background: %s; " +
                        "-fx-bg-surface: %s; -fx-bg-surface-solid: %s; -fx-on-surface: %s; -fx-surface-variant: %s; -fx-on-surface-variant: %s; " +
                        "-fx-outline: %s; -fx-outline-variant: %s; " +
                        "-fx-selection-bg: %s;",
                ThemeEngine.toHex(p.primary), ThemeEngine.toHex(p.onPrimary), ThemeEngine.toHex(p.primaryContainer), ThemeEngine.toHex(p.onPrimaryContainer),
                ThemeEngine.toHex(p.secondary), ThemeEngine.toHex(p.onSecondary), ThemeEngine.toHex(p.secondaryContainer),
                ThemeEngine.toHex(p.tertiary), ThemeEngine.toHex(p.onTertiary),
                ThemeEngine.toHex(p.background), ThemeEngine.toHex(p.onBackground),
                surfaceRgba, ThemeEngine.toHex(p.surface), ThemeEngine.toHex(p.onSurface), ThemeEngine.toHex(p.surfaceVariant), ThemeEngine.toHex(p.onSurfaceVariant),
                ThemeEngine.toHex(p.outline), ThemeEngine.toHex(p.outlineVariant),
                primaryRgba
        );
        rootContainer.setStyle(style);
    }

    private String toRgba(Color c, double alpha) {
        return String.format("rgba(%d, %d, %d, %.2f)", (int)(c.getRed()*255), (int)(c.getGreen()*255), (int)(c.getBlue()*255), alpha);
    }

    // --- Views ---

    private void showHomeView() {
        StackPane homeRoot = new StackPane();
        VBox layout = new VBox(30);
        layout.setAlignment(Pos.CENTER);

        StackPane glassCard = new StackPane(layout);
        glassCard.setMaxSize(500, 300);
        glassCard.getStyleClass().add("glass-card");

        Label title = new Label("Disk Analyzer");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: -fx-primary; -fx-effect: dropshadow(gaussian, -fx-primary, 10, 0.3, 0, 0);");

        Button btnScan = new Button("📂 选择扫描目录");
        btnScan.getStyleClass().add("btn-primary");
        btnScan.setOnAction(e -> {
            DirectoryChooser dc = new DirectoryChooser();
            dc.setTitle("选择要扫描的文件夹");
            File dir = dc.showDialog(primaryStage);
            if (dir != null) showLoadingView(dir);
        });

        layout.getChildren().addAll(title, btnScan);

        Button btnSettings = new Button("⚙ 设置");
        btnSettings.getStyleClass().add("btn-icon");
        btnSettings.setOnAction(e -> showSettingsView(() -> showHomeView()));

        StackPane.setAlignment(btnSettings, Pos.TOP_RIGHT);
        StackPane.setMargin(btnSettings, new Insets(20));

        homeRoot.getChildren().addAll(glassCard, btnSettings);
        rootContainer.getChildren().setAll(homeRoot);
    }

    // --- 设置界面 ---
    private void showSettingsView(Runnable returnAction) {
        BorderPane root = new BorderPane();
        root.setMaxSize(800, 500);
        root.getStyleClass().add("dialog-window");

        VBox sidebar = new VBox(10);
        sidebar.setPadding(new Insets(20));
        sidebar.setPrefWidth(200);
        sidebar.setStyle("-fx-border-color: transparent -fx-outline-variant transparent transparent; -fx-border-width: 1;");

        Label lblTitle = new Label("设置");
        lblTitle.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: -fx-on-surface; -fx-padding: 0 0 20 0;");

        ToggleGroup group = new ToggleGroup();
        ToggleButton btnGeneral = createNavButton("外观设置", group);
        ToggleButton btnCats = createNavButton("分类管理", group);
        ToggleButton btnAbout = createNavButton("关于软件", group);

        sidebar.getChildren().addAll(lblTitle, btnGeneral, btnCats, btnAbout);

        StackPane contentArea = new StackPane();
        contentArea.setPadding(new Insets(20, 40, 20, 40));
        contentArea.setAlignment(Pos.TOP_LEFT);

        btnGeneral.setOnAction(e -> { if (btnGeneral.isSelected()) contentArea.getChildren().setAll(createAppearanceContent(returnAction)); });
        btnCats.setOnAction(e -> { if (btnCats.isSelected()) contentArea.getChildren().setAll(createCategoryContent()); });
        btnAbout.setOnAction(e -> { if (btnAbout.isSelected()) contentArea.getChildren().setAll(createAboutContent()); });

        btnGeneral.setSelected(true);
        contentArea.getChildren().setAll(createAppearanceContent(returnAction));

        Button btnClose = new Button("✕");
        btnClose.getStyleClass().add("btn-icon");
        btnClose.setOnAction(e -> returnAction.run());
        StackPane.setAlignment(btnClose, Pos.TOP_RIGHT);
        StackPane.setMargin(btnClose, new Insets(15));

        root.setLeft(sidebar);
        root.setCenter(contentArea);

        StackPane mask = new StackPane(root);
        mask.getChildren().add(btnClose);
        mask.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        rootContainer.getChildren().setAll(mask);
    }

    private ToggleButton createNavButton(String text, ToggleGroup group) {
        ToggleButton btn = new ToggleButton(text);
        btn.setToggleGroup(group);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.getStyleClass().add("settings-nav-btn");
        return btn;
    }

    private Node createAppearanceContent(Runnable returnAction) {
        VBox layout = new VBox(25);
        layout.setAlignment(Pos.TOP_LEFT);

        Label header = new Label("外观设置");
        header.setStyle("-fx-text-fill: -fx-primary; -fx-font-size: 18px; -fx-font-weight: bold;");

        VBox wallpaperBox = new VBox(10);
        Label lblWp = new Label("壁纸与主题");
        lblWp.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-weight: bold;");

        HBox wpRow = new HBox(15);
        wpRow.setAlignment(Pos.CENTER_LEFT);
        Label lblPath = new Label(currentBgImage != null ? "当前：自定义壁纸" : "当前：默认主题");
        lblPath.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-style: italic;");

        Button btnChange = new Button("选择图片...");
        btnChange.getStyleClass().add("btn-secondary");
        btnChange.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("图片文件", "*.png", "*.jpg", "*.jpeg"));
            File file = fc.showOpenDialog(primaryStage);
            if (file != null) {
                try {
                    updateWallpaperAndTheme(new Image(file.toURI().toString()));
                    lblPath.setText("已选: " + file.getName());
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        wpRow.getChildren().addAll(btnChange, lblPath);
        wallpaperBox.getChildren().addAll(lblWp, wpRow);

        VBox styleBox = new VBox(10);
        Label lblStyle = new Label("配色风格");
        lblStyle.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-weight: bold;");

        ComboBox<ThemeStyle> styleCombo = new ComboBox<>();
        styleCombo.getItems().addAll(ThemeStyle.values());
        styleCombo.setValue(currentStyle);
        styleCombo.setMaxWidth(300);
        styleCombo.setOnAction(e -> {
            if (styleCombo.getValue() != null) updateThemeStyle(styleCombo.getValue());
        });
        styleBox.getChildren().addAll(lblStyle, styleCombo);

        layout.getChildren().addAll(header, wallpaperBox, styleBox);
        return layout;
    }

    private Node createCategoryContent() {
        VBox layout = new VBox(15);
        layout.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(layout, Priority.ALWAYS);

        Label header = new Label("文件分类管理");
        header.setStyle("-fx-text-fill: -fx-primary; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label sub = new Label("自定义在'类型'视图中显示的文件分类规则。");
        sub.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 12px;");

        VBox listContainer = new VBox(10);
        ScrollPane scroll = new ScrollPane(listContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Runnable refreshList = new Runnable() {
            @Override
            public void run() {
                listContainer.getChildren().clear();
                FILE_CATEGORIES.forEach((name, exts) -> {
                    listContainer.getChildren().add(createCategoryRow(name, exts, () -> this.run()));
                });
            }
        };
        refreshList.run();

        HBox addBox = new HBox(10);
        addBox.setAlignment(Pos.CENTER_LEFT);
        addBox.setPadding(new Insets(10,0,0,0));

        TextField txtName = new TextField();
        txtName.setPromptText("分类名 (如: 模型)");
        txtName.getStyleClass().add("settings-input");

        TextField txtExts = new TextField();
        txtExts.setPromptText("后缀 (如: obj,fbx)");
        txtExts.getStyleClass().add("settings-input");
        HBox.setHgrow(txtExts, Priority.ALWAYS);

        Button btnAdd = new Button("添加");
        btnAdd.getStyleClass().add("btn-primary");
        btnAdd.setOnAction(e -> {
            String n = txtName.getText().trim();
            String ex = txtExts.getText().trim();
            if (!n.isEmpty() && !ex.isEmpty()) {
                List<String> extList = new ArrayList<>(Arrays.asList(ex.split("[,;\\s]+")));
                FILE_CATEGORIES.put(n, extList);
                txtName.clear();
                txtExts.clear();
                refreshList.run();
            }
        });

        addBox.getChildren().addAll(txtName, txtExts, btnAdd);

        layout.getChildren().addAll(header, sub, scroll, addBox);
        return layout;
    }

    private Node createAboutContent() {
        VBox layout = new VBox(20);
        layout.setAlignment(Pos.CENTER);
        VBox.setVgrow(layout, Priority.ALWAYS);

        Label lblName = new Label("Disk Analyzer");
        lblName.setStyle("-fx-text-fill: -fx-primary; -fx-font-size: 32px; -fx-font-weight: bold; -fx-effect: dropshadow(gaussian, -fx-primary, 10, 0.3, 0, 0);");

        Label lblVer = new Label("版本: v.1.1.0");
        lblVer.setStyle("-fx-text-fill: -fx-on-surface-variant; -fx-font-size: 14px; -fx-font-family: 'Consolas'; -fx-letter-spacing: 2px;");

        Region sep = new Region();
        sep.setMaxWidth(200);
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: -fx-outline-variant;");

        VBox infoBox = new VBox(8);
        infoBox.setAlignment(Pos.CENTER);
        Label lblAuthor = new Label("作者: NightRainLone");
        lblAuthor.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label lblEmail = new Label("邮箱: 498187073@qq.com");
        lblEmail.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 14px;");

        infoBox.getChildren().addAll(lblAuthor, lblEmail);

        Label lblThanks = new Label("感谢使用!");
        lblThanks.setStyle("-fx-text-fill: -fx-outline; -fx-font-size: 12px; -fx-padding: 30 0 0 0;");

        layout.getChildren().addAll(lblName, lblVer, sep, infoBox, lblThanks);
        return layout;
    }

    private Node createCategoryRow(String name, List<String> exts, Runnable onUpdate) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8));
        row.setStyle("-fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8;");

        Label lblName = new Label(name);
        lblName.setStyle("-fx-text-fill: -fx-on-surface; -fx-font-weight: bold;");
        lblName.setPrefWidth(80);

        TextField txtExts = new TextField(String.join(", ", exts));
        txtExts.getStyleClass().add("settings-input");
        HBox.setHgrow(txtExts, Priority.ALWAYS);

        txtExts.focusedProperty().addListener((obs, old, isFocused) -> {
            if (!isFocused) {
                List<String> newExts = new ArrayList<>(Arrays.asList(txtExts.getText().split("[,;\\s]+")));
                FILE_CATEGORIES.put(name, newExts);
            }
        });

        Button btnDel = new Button("✕");
        btnDel.setStyle("-fx-background-color: transparent; -fx-text-fill: -fx-text-secondary; -fx-cursor: hand;");
        btnDel.setOnAction(e -> {
            FILE_CATEGORIES.remove(name);
            onUpdate.run();
        });

        row.getChildren().addAll(lblName, txtExts, btnDel);
        return row;
    }

    private void showLoadingView(File dir) {
        DiskScanner.resetStats();

        VBox card = new VBox(25);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(600, 400);
        card.setPadding(new Insets(40));
        card.getStyleClass().add("glass-card");

        Label titleLabel = new Label("正在扫描");
        titleLabel.setStyle("-fx-text-fill: -fx-primary; -fx-font-size: 14px; -fx-font-weight: bold; -fx-letter-spacing: 2px;");

        Label pathLabel = new Label(dir.getAbsolutePath());
        pathLabel.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-family: 'Segoe UI'; -fx-font-size: 13px;");
        pathLabel.setMaxWidth(500);
        pathLabel.setWrapText(false);
        pathLabel.setAlignment(Pos.CENTER);

        ProgressBar pb = new ProgressBar(-1);
        pb.setPrefWidth(500);
        pb.setPrefHeight(6);

        GridPane stats = new GridPane();
        stats.setHgap(40);
        stats.setVgap(30);
        stats.setAlignment(Pos.CENTER);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(50);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(50);
        stats.getColumnConstraints().addAll(col1, col2);

        Label valFiles = createStatBigValue("0");
        Label valSize = createStatBigValue("0 B");
        Label valTime = createStatBigValue("00:00");
        Label valSpeed = createStatBigValue("0 MB/s");

        stats.add(createStatItem("已扫描文件", valFiles), 0, 0);
        stats.add(createStatItem("总大小", valSize), 1, 0);
        stats.add(createStatItem("耗时", valTime), 0, 1);
        stats.add(createStatItem("速度", valSpeed), 1, 1);

        card.getChildren().addAll(titleLabel, pathLabel, pb, stats);

        StackPane mask = new StackPane(card);
        mask.setStyle("-fx-background-color: rgba(0,0,0,0.5);");
        rootContainer.getChildren().setAll(mask);

        long startTime = System.currentTimeMillis();
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long files = DiskScanner.scannedFileCount.get();
                long size = DiskScanner.scannedTotalSize.get();
                String current = DiskScanner.currentScanningPath.get();
                long elapsedMillis = System.currentTimeMillis() - startTime;

                valFiles.setText(String.format("%,d", files));
                valSize.setText(FileNode.formatSize(size));
                if (current != null && !current.isEmpty()) pathLabel.setText("正在扫描: " + current);

                long seconds = elapsedMillis / 1000;
                valTime.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
                if (elapsedMillis > 0) {
                    double speedBytesPerSec = (double) size / elapsedMillis * 1000;
                    valSpeed.setText(FileNode.formatSize((long)speedBytesPerSec) + "/s");
                }
            }
        };
        timer.start();

        Task<FileNode> task = new Task<>() {
            @Override
            protected FileNode call() {
                return new ForkJoinPool().invoke(new DiskScanner(dir));
            }
        };

        task.setOnSucceeded(e -> {
            timer.stop();
            showReportView(task.getValue());
        });
        task.setOnFailed(e -> {
            timer.stop();
            showHomeView();
        });
        new Thread(task).start();
    }

    private Label createStatBigValue(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: -fx-text-primary; -fx-font-family: 'Consolas'; -fx-font-size: 22px; -fx-font-weight: bold;");
        l.setMinWidth(150);
        return l;
    }

    private VBox createStatItem(String title, Label value) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: -fx-text-secondary; -fx-font-size: 10px; -fx-font-weight: bold; -fx-letter-spacing: 1px;");
        VBox box = new VBox(5, t, value);
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    private void showReportView(FileNode rootNode) {
        NativeReportView reportView = new NativeReportView(
                rootNode, this::showHomeView,
                () -> showSettingsView(() -> showReportView(rootNode))
        );
        rootContainer.getChildren().setAll(reportView.getView());
    }

    private static final String GLOBAL_CSS = """
        .root { 
            -fx-font-family: 'Segoe UI', sans-serif;
            -fx-primary: #D0BCFF; 
            -fx-on-primary: #381E72;
            -fx-bg-base: #121212;
            -fx-bg-surface: rgba(30,30,30,0.8);
            -fx-bg-surface-solid: #1e1e1e;
            -fx-text-primary: #E5E7EB;
            -fx-text-secondary: #9CA3AF;
            -fx-selection-bg: rgba(208, 188, 255, 0.1);
            -fx-color-border: rgba(255,255,255,0.1);
            -fx-outline: #888888;
        }
        
        .label { -fx-text-fill: -fx-text-primary; }
        
        .glass-card {
            -fx-background-color: -fx-bg-surface;
            -fx-background-radius: 24;
            -fx-border-color: -fx-color-border;
            -fx-border-radius: 24;
            -fx-border-width: 1;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 20, 0, 0, 8);
        }
        
        .dialog-window {
            -fx-background-color: -fx-bg-surface-solid;
            -fx-background-radius: 20;
            -fx-border-color: -fx-color-border;
            -fx-border-radius: 20;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 30, 0, 0, 10);
        }

        .settings-nav-btn {
            -fx-background-color: transparent;
            -fx-text-fill: -fx-text-secondary;
            -fx-font-size: 14px;
            -fx-alignment: CENTER_LEFT;
            -fx-padding: 10 15;
            -fx-background-radius: 8;
            -fx-cursor: hand;
        }
        .settings-nav-btn:hover {
            -fx-background-color: rgba(255,255,255,0.05);
            -fx-text-fill: -fx-text-primary;
        }
        .settings-nav-btn:selected {
            -fx-background-color: -fx-selection-bg;
            -fx-text-fill: -fx-primary;
            -fx-font-weight: bold;
        }
        
        .settings-input {
            -fx-background-color: rgba(0,0,0,0.3);
            -fx-text-fill: -fx-text-primary;
            -fx-border-color: -fx-color-border;
            -fx-border-radius: 6;
            -fx-background-radius: 6;
        }
        .settings-input:focused {
            -fx-border-color: -fx-primary;
        }

        .btn-primary {
            -fx-background-color: -fx-primary; 
            -fx-text-fill: -fx-on-primary; 
            -fx-font-weight: bold; -fx-font-size: 14px; -fx-background-radius: 24; -fx-padding: 10 30; -fx-cursor: hand;
            -fx-effect: dropshadow(three-pass-box, -fx-primary, 10, 0.2, 0, 2);
        }
        .btn-primary:hover { 
            -fx-background-color: derive(-fx-primary, 20%); 
            -fx-scale-x: 1.02; -fx-scale-y: 1.02;
        }
        
        .btn-secondary {
            -fx-background-color: rgba(255,255,255,0.08); 
            -fx-text-fill: -fx-text-primary; 
            -fx-background-radius: 8; -fx-cursor: hand;
            -fx-border-color: -fx-color-border; -fx-border-radius: 8;
        }
        .btn-secondary:hover { -fx-background-color: rgba(255,255,255,0.15); }
        
        .btn-icon {
            -fx-background-color: rgba(0,0,0,0.2); 
            -fx-text-fill: -fx-text-secondary; 
            -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13px; -fx-padding: 8 15;
            -fx-border-color: -fx-color-border; -fx-border-radius: 20;
        }
        .btn-icon:hover { 
            -fx-background-color: -fx-selection-bg; 
            -fx-text-fill: -fx-primary; 
            -fx-border-color: -fx-primary;
        }

        .context-menu {
            -fx-background-color: -fx-bg-surface-solid;
            -fx-background-radius: 0; 
            -fx-border-color: -fx-color-border;
            -fx-border-radius: 0;
            -fx-padding: 4 0 4 0;
            -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.5), 15, 0, 0, 5);
        }
        .context-menu .menu-item { -fx-background-color: transparent; -fx-padding: 8 15 8 15; }
        .context-menu .menu-item:focused { -fx-background-color: -fx-selection-bg; }
        .context-menu .menu-item > .label { -fx-text-fill: -fx-text-primary; -fx-font-size: 13px; }

        .details-window {
            -fx-background-color: -fx-bg-surface-solid;
            -fx-background-radius: 12;
            -fx-border-color: -fx-color-border;
            -fx-border-radius: 12;
            -fx-border-width: 1;
            -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.8), 40, 0, 0, 15);
        }
        .details-title { -fx-text-fill: -fx-primary; -fx-font-size: 18px; -fx-font-weight: bold; }
        .details-subtitle { -fx-text-fill: -fx-text-secondary; -fx-font-size: 12px; }
        .details-label { -fx-text-fill: -fx-text-secondary; -fx-font-weight: bold; }
        .details-value { -fx-text-fill: -fx-text-primary; }
        .details-separator { -fx-background-color: -fx-color-border; -fx-min-height: 1; -fx-max-height: 1; }

        .progress-bar > .track { -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 4; }
        .progress-bar > .bar { -fx-background-color: -fx-primary; -fx-background-radius: 4; -fx-background-insets: 0; -fx-effect: dropshadow(three-pass-box, -fx-primary, 8, 0, 0, 0); }
        
        .list-cell { -fx-background-color: transparent; -fx-padding: 0; }
        .list-cell:filled:selected, .list-cell:filled:chart-hover { 
            -fx-background-color: -fx-selection-bg !important; 
            -fx-border-color: transparent transparent transparent -fx-primary;
            -fx-border-width: 0 0 0 3;
        }
        .list-cell:filled:hover { -fx-background-color: rgba(255, 255, 255, 0.05); }

        .list-view .virtual-flow .scroll-bar:horizontal { -fx-scale-x: 0; }
        .list-view .virtual-flow .scroll-bar:vertical { -fx-background-color: transparent; }
        .list-view .virtual-flow .scroll-bar .thumb { -fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 4; }
        
        .combo-box { -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 4; }
        .combo-box .list-cell { -fx-text-fill: white; }
        .combo-box-popup .list-view { -fx-background-color: #2b2b2b; }
        .combo-box-popup .list-cell:filled:hover { -fx-background-color: #444; }

        .breadcrumb-btn { -fx-background-color: transparent; -fx-text-fill: -fx-text-secondary; -fx-font-size: 14px; -fx-background-radius: 6; -fx-padding: 4 8; -fx-cursor: hand; }
        .breadcrumb-btn:hover { -fx-background-color: -fx-selection-bg; -fx-text-fill: -fx-primary; }
        .breadcrumb-last { -fx-background-color: transparent; -fx-text-fill: -fx-primary; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 4 8; }
        .breadcrumb-sep { -fx-text-fill: -fx-outline; -fx-font-size: 14px; -fx-padding: 4 0; -fx-alignment: center; }
    """;

    public static void main(String[] args) {
        launch(args);
    }
}