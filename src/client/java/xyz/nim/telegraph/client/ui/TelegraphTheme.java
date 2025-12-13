package xyz.nim.telegraph.client.ui;

public class TelegraphTheme {

    // === COLORS ===
    public static final int PANEL_BG = 0xA0000000;
    public static final int PANEL_BORDER = 0xFFC0C0C0;
    public static final int HEADER_BG = 0xFF222222;

    public static final int TEXT_PRIMARY = 0xFFFFFFFF;
    public static final int TEXT_SECONDARY = 0xFFAAAAAA;
    public static final int TEXT_MUTED = 0xFF888888;

    public static final int SUCCESS = 0xFF55FF55;
    public static final int ERROR = 0xFFFF5555;
    public static final int WARNING = 0xFFFFFF55;
    public static final int INFO = 0xFF55FFFF;

    public static final int SELECTED = 0xFFFFFF00;
    public static final int HOVER = 0x40FFFFFF;

    // === RESPONSIVE BREAKPOINTS ===
    public enum ScreenSize {
        SMALL,
        MEDIUM,
        LARGE,
        XLARGE
    }

    public static ScreenSize getSize(int width, int height) {
        int min = Math.min(width, height);
        if (min < 300) return ScreenSize.SMALL;
        if (min < 450) return ScreenSize.MEDIUM;
        if (min < 600) return ScreenSize.LARGE;
        return ScreenSize.XLARGE;
    }

    // === RESPONSIVE SPACING ===
    public static int margin(ScreenSize size) {
        return switch (size) {
            case SMALL -> 6;
            case MEDIUM -> 10;
            case LARGE -> 15;
            case XLARGE -> 20;
        };
    }

    public static int padding(ScreenSize size) {
        return switch (size) {
            case SMALL -> 4;
            case MEDIUM -> 6;
            case LARGE -> 8;
            case XLARGE -> 10;
        };
    }

    public static int spacing(ScreenSize size) {
        return switch (size) {
            case SMALL -> 4;
            case MEDIUM -> 6;
            case LARGE -> 8;
            case XLARGE -> 10;
        };
    }

    // === RESPONSIVE SIZES ===
    public static int buttonHeight(ScreenSize size) {
        return switch (size) {
            case SMALL -> 16;
            case MEDIUM -> 18;
            case LARGE -> 20;
            case XLARGE -> 22;
        };
    }

    public static int headerHeight(ScreenSize size) {
        return switch (size) {
            case SMALL -> 20;
            case MEDIUM -> 24;
            case LARGE -> 28;
            case XLARGE -> 32;
        };
    }

    public static int controlHeight(ScreenSize size) {
        return switch (size) {
            case SMALL -> 16;
            case MEDIUM -> 18;
            case LARGE -> 20;
            case XLARGE -> 20;
        };
    }

    public static int buttonWidth(ScreenSize size) {
        return switch (size) {
            case SMALL -> 60;
            case MEDIUM -> 80;
            case LARGE -> 100;
            case XLARGE -> 120;
        };
    }

    public static int smallButtonWidth(ScreenSize size) {
        return switch (size) {
            case SMALL -> 40;
            case MEDIUM -> 50;
            case LARGE -> 60;
            case XLARGE -> 70;
        };
    }
}
