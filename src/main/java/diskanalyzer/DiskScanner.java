package diskanalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class DiskScanner extends RecursiveTask<FileNode> {
    private final Path dirPath; // 改用 Path，这是 NIO 的核心类

    // 全局统计指标
    public static final AtomicLong scannedFileCount = new AtomicLong(0);
    public static final AtomicLong scannedTotalSize = new AtomicLong(0);
    public static final AtomicReference<String> currentScanningPath = new AtomicReference<>("");

    public static void resetStats() {
        scannedFileCount.set(0);
        scannedTotalSize.set(0);
        currentScanningPath.set("Initializing...");
    }

    // 公共构造函数接收 File (兼容 MainApp)
    public DiskScanner(File dir) {
        this.dirPath = dir.toPath();
    }

    // 私有构造函数接收 Path (用于内部递归，减少转换开销)
    private DiskScanner(Path dirPath) {
        this.dirPath = dirPath;
    }

    @Override
    protected FileNode compute() {
        // 更新 UI 状态 (仅获取文件名，避免 toString 全路径带来的字符串开销)
        Path fileName = dirPath.getFileName();
        currentScanningPath.set(fileName == null ? dirPath.toString() : fileName.toString());

        // 创建当前节点
        FileNode node = new FileNode(
                fileName == null ? dirPath.toString() : fileName.toString(),
                dirPath.toString(),
                true
        );

        List<DiskScanner> subTasks = new ArrayList<>();

        // ★★★ 核心优化：使用 NIO DirectoryStream 流式读取 ★★★
        // try-with-resources 自动关闭流，防止文件句柄泄漏
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dirPath)) {
            for (Path entry : stream) {
                try {
                    // 读取文件属性 (比 toFile().length() 更快且更准确)
                    BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);

                    if (attrs.isDirectory()) {
                        // 如果是目录：创建子任务并 Fork
                        DiskScanner task = new DiskScanner(entry);
                        task.fork();
                        subTasks.add(task);
                    } else {
                        // 如果是文件：直接构建节点
                        // 注意：entry.toAbsolutePath().toString() 可能会有微小性能开销，但在 UI 展示中是必须的
                        FileNode fileNode = new FileNode(
                                entry.getFileName().toString(),
                                entry.toAbsolutePath().toString(),
                                false
                        );
                        fileNode.size = attrs.size();
                        fileNode.parent = node; // 维护父子关系，用于删除功能

                        node.children.add(fileNode);
                        node.size += fileNode.size;

                        // 更新全局统计
                        scannedFileCount.incrementAndGet();
                        scannedTotalSize.addAndGet(fileNode.size);
                    }
                } catch (IOException e) {
                    // 忽略单个文件的读取错误（如符号链接失效或权限不足）
                    // continue;
                }
            }
        } catch (IOException | SecurityException e) {
            // 忽略整个目录的读取错误（如 System Volume Information 拒绝访问）
            // System.err.println("Access Denied: " + dirPath);
        }

        // 等待所有子目录扫描完成并汇总大小
        for (DiskScanner task : subTasks) {
            FileNode childDir = task.join();
            // 只有当子目录扫描成功（非 null）才添加
            if (childDir != null) {
                childDir.parent = node;
                node.children.add(childDir);
                node.size += childDir.size;
            }
        }

        // --- 以下是原有的排序与截断逻辑 (保持不变) ---

        // 排序：从大到小
        node.children.sort((a, b) -> Long.compare(b.size, a.size));

        // 截断逻辑：保留前 50 个，其余归并为 Other
        if (node.children.size() > 50) {
            long otherSize = 0;
            List<FileNode> keep = new ArrayList<>();
            for (int i = 0; i < 50; i++) keep.add(node.children.get(i));
            for (int i = 50; i < node.children.size(); i++) otherSize += node.children.get(i).size;

            if (otherSize > 0) {
                // 创建 Other 节点
                FileNode other = new FileNode("[Other Files]", node.path, false);
                other.size = otherSize;
                other.isOther = true;
                // other.parent = node; // Other 节点通常不参与物理删除，parent 可设可不设
                keep.add(other);
            }
            node.children = keep;
        }

        return node;
    }
}