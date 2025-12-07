package diskanalyzer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class FileNode {
    String name;
    String path;
    long size;
    boolean isDir;
    boolean isOther = false;

    // ★★★ 新增：父节点引用，用于删除后向上更新大小 ★★★
    FileNode parent;

    List<FileNode> children = new ArrayList<>();

    public FileNode(String name, String path, boolean isDir) {
        this.name = name;
        this.path = path;
        this.isDir = isDir;
    }

    public static String formatSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        if (digitGroups > 4) digitGroups = 4;
        DecimalFormat df = new DecimalFormat("#.##");
        return df.format(size / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
    }
}