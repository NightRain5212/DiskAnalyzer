module diskanalyzer.diskanalyzer {
    // 引入需要的 JavaFX 模块
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics; // DirectoryChooser 实际上依赖 graphics 模块
    requires java.desktop;

    // 开放你的包给 JavaFX 调用（必须！）
    // 这里的 'com.example' 必须改成你 MainApp.java 所在的实际包名
    // 假设你的 MainApp 在 package com.example; 下
    opens diskanalyzer to javafx.fxml;
    exports diskanalyzer;
}