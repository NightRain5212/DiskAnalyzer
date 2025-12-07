package diskanalyzer;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.util.*;

public class ThemeEngine {

    // Material Design 3 风格的语义化配色表
    public static class Palette {
        // --- 核心色 ---
        public Color primary;
        public Color onPrimary;
        public Color primaryContainer;
        public Color onPrimaryContainer;

        public Color secondary;
        public Color onSecondary;
        public Color secondaryContainer;
        public Color onSecondaryContainer;

        public Color tertiary; // Accent / 强调色
        public Color onTertiary;
        public Color tertiaryContainer;
        public Color onTertiaryContainer;

        // --- 背景与表面 ---
        public Color background;
        public Color onBackground;
        public Color surface;
        public Color onSurface;
        public Color surfaceVariant;
        public Color onSurfaceVariant;
        public Color outline;
        public Color outlineVariant;

        // --- ★★★ 修复：显式定义文本颜色字段 ★★★ ---
        public Color textPrimary;   // 用于主要标题、正文
        public Color textSecondary; // 用于次要信息、标签

        // --- 图表专用 ---
        public List<String> chartColors = new ArrayList<>();
    }

    /**
     * 1. 提取壁纸主色 (Seed Color)
     */
    public static Color extractDominantColor(Image image) {
        if (image == null) return Color.web("#D9E878");

        PixelReader reader = image.getPixelReader();
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();

        int step = Math.max(1, width * height / 2000);

        Color bestColor = Color.web("#D9E878");
        double maxScore = -1.0;

        for (int i = 0; i < width * height; i += step) {
            int x = i % width;
            int y = i / width;
            Color c = reader.getColor(x, y);

            double sat = c.getSaturation();
            double bri = c.getBrightness();

            if (bri < 0.1 || bri > 0.95 || sat < 0.1) continue;

            double score = sat * 3.0 + (1.0 - Math.abs(bri - 0.6));
            if (score > maxScore) {
                maxScore = score;
                bestColor = c;
            }
        }
        return bestColor;
    }

    /**
     * 2. 生成完整配色方案
     */
    public static Palette generatePalette(Color seed, ThemeStyle style, boolean isDark) {
        Palette p = new Palette();

        double hue = seed.getHue();
        double sat = seed.getSaturation();

        switch (style) {
            case EXPRESSIVE: sat = Math.min(1.0, sat * 1.5); break;
            case NEUTRAL:    sat *= 0.2; break;
            case MONOCHROME: sat = 0; break;
            case FRUIT_SALAD: sat = 0.9; hue = (hue + 10) % 360; break;
            default: break;
        }

        double hPri = hue;
        double sPri = sat;

        double hSec = (hue + 15) % 360;
        double sSec = Math.max(0, sat - 0.2);

        double hTer = (hue + 60) % 360;
        double sTer = Math.min(1.0, sat + 0.1);

        double hNeu = hue;
        double sNeu = Math.min(sat, 0.05);

        if (isDark) {
            p.primary           = Color.hsb(hPri, sPri * 0.8, 0.90);
            p.onPrimary         = Color.hsb(hPri, sPri, 0.10);
            p.primaryContainer  = Color.hsb(hPri, sPri, 0.30);
            p.onPrimaryContainer= Color.hsb(hPri, sPri * 0.5, 0.95);

            p.secondary         = Color.hsb(hSec, sSec * 0.8, 0.85);
            p.onSecondary       = Color.hsb(hSec, sSec, 0.10);
            p.secondaryContainer= Color.hsb(hSec, sSec, 0.30);
            p.onSecondaryContainer= Color.hsb(hSec, sSec * 0.5, 0.95);

            p.tertiary          = Color.hsb(hTer, sTer * 0.8, 0.90);
            p.onTertiary        = Color.hsb(hTer, sTer, 0.10);
            p.tertiaryContainer = Color.hsb(hTer, sTer, 0.30);
            p.onTertiaryContainer= Color.hsb(hTer, sTer * 0.5, 0.95);

            p.background        = Color.hsb(hNeu, sNeu, 0.06);
            p.onBackground      = Color.hsb(hNeu, sNeu, 0.90);

            p.surface           = Color.hsb(hNeu, sNeu, 0.12);
            p.onSurface         = Color.hsb(hNeu, sNeu, 0.90);
            p.surfaceVariant    = Color.hsb(hNeu, sNeu, 0.25);
            p.onSurfaceVariant  = Color.hsb(hNeu, sNeu, 0.80);

            p.outline           = Color.hsb(hNeu, sNeu, 0.60);
            p.outlineVariant    = Color.hsb(hNeu, sNeu, 0.30);
        } else {
            // Light Mode
            p.primary           = Color.hsb(hPri, sPri, 0.40);
            p.onPrimary         = Color.WHITE;
            p.primaryContainer  = Color.hsb(hPri, sPri, 0.90);
            p.onPrimaryContainer= Color.hsb(hPri, sPri, 0.10);

            p.secondary         = Color.hsb(hSec, sSec, 0.40);
            p.onSecondary       = Color.WHITE;
            p.secondaryContainer= Color.hsb(hSec, sSec, 0.90);
            p.onSecondaryContainer= Color.hsb(hSec, sSec, 0.10);

            p.tertiary          = Color.hsb(hTer, sTer, 0.40);
            p.onTertiary        = Color.WHITE;
            p.tertiaryContainer = Color.hsb(hTer, sTer, 0.90);
            p.onTertiaryContainer= Color.hsb(hTer, sTer, 0.10);

            p.background        = Color.hsb(hNeu, sNeu, 0.98);
            p.onBackground      = Color.hsb(hNeu, sNeu, 0.10);
            p.surface           = Color.hsb(hNeu, sNeu, 0.96);
            p.onSurface         = Color.hsb(hNeu, sNeu, 0.10);
            p.surfaceVariant    = Color.hsb(hNeu, sNeu, 0.90);
            p.onSurfaceVariant  = Color.hsb(hNeu, sNeu, 0.30);
            p.outline           = Color.hsb(hNeu, sNeu, 0.50);
            p.outlineVariant    = Color.hsb(hNeu, sNeu, 0.80);
        }

        // ★★★ 映射 Text 颜色 (修复 MainApp 报错的关键) ★★★
        p.textPrimary = p.onSurface;         // 主要文字 = 表面上的文字色
        p.textSecondary = p.onSurfaceVariant;// 次要文字 = 表面变体上的文字色

        // 扇形图颜色
        p.chartColors = generateChartColors(hue, sat, style, isDark);

        return p;
    }

    private static List<String> generateChartColors(double h, double s, ThemeStyle style, boolean isDark) {
        List<String> colors = new ArrayList<>();
        double brightness = isDark ? 0.9 : 0.6;

        for (int i = 0; i < 8; i++) {
            double hueOffset;
            if (style == ThemeStyle.FRUIT_SALAD || style == ThemeStyle.EXPRESSIVE) {
                hueOffset = i * 45;
            } else if (style == ThemeStyle.MONOCHROME) {
                hueOffset = 0;
            } else {
                hueOffset = i * 20;
            }

            double newSat = style == ThemeStyle.MONOCHROME ? 0 : s;
            double newBri = style == ThemeStyle.MONOCHROME ?
                    (isDark ? 1.0 - i * 0.1 : 0.3 + i * 0.1) :
                    brightness;

            Color c = Color.hsb((h + hueOffset) % 360, Math.max(0.4, newSat), newBri);
            colors.add(toHex(c));
        }
        return colors;
    }

    public static String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }
}