package xyz.nim.telegraph.client.ui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ToastManager {
    private static final int TOAST_DURATION_MS = 3000;
    private static final int FADE_DURATION_MS = 500;
    private static final int TOAST_HEIGHT = 24;
    private static final int TOAST_PADDING = 8;
    private static final int TOAST_MARGIN = 4;
    private static final int MAX_TOASTS = 3;

    private static final int COLOR_SUCCESS = 0xFF2E7D32;
    private static final int COLOR_ERROR = 0xFFC62828;
    private static final int COLOR_INFO = 0xFF1565C0;
    private static final int COLOR_WARNING = 0xFFF57C00;

    public enum ToastType {
        SUCCESS(COLOR_SUCCESS, "✓"),
        ERROR(COLOR_ERROR, "✗"),
        INFO(COLOR_INFO, "i"),
        WARNING(COLOR_WARNING, "!");

        final int color;
        final String icon;

        ToastType(int color, String icon) {
            this.color = color;
            this.icon = icon;
        }
    }

    private static class Toast {
        final String message;
        final ToastType type;
        final long createdAt;

        Toast(String message, ToastType type) {
            this.message = message;
            this.type = type;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isExpired() {
            return System.currentTimeMillis() - createdAt > TOAST_DURATION_MS + FADE_DURATION_MS;
        }

        float getOpacity() {
            long age = System.currentTimeMillis() - createdAt;
            if (age < TOAST_DURATION_MS) {
                return 1.0f;
            }
            float fadeProgress = (age - TOAST_DURATION_MS) / (float) FADE_DURATION_MS;
            return Math.max(0, 1.0f - fadeProgress);
        }
    }

    private final List<Toast> toasts = new ArrayList<>();

    public void success(String message) {
        addToast(message, ToastType.SUCCESS);
    }

    public void error(String message) {
        addToast(message, ToastType.ERROR);
    }

    public void info(String message) {
        addToast(message, ToastType.INFO);
    }

    public void warning(String message) {
        addToast(message, ToastType.WARNING);
    }

    private void addToast(String message, ToastType type) {
        if (toasts.size() >= MAX_TOASTS) {
            toasts.remove(0);
        }
        toasts.add(new Toast(message, type));
    }

    public void render(GuiGraphics context, Font font, int screenWidth, int screenHeight) {
        Iterator<Toast> iterator = toasts.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().isExpired()) {
                iterator.remove();
            }
        }

        int y = screenHeight - TOAST_MARGIN - TOAST_HEIGHT;
        for (int i = toasts.size() - 1; i >= 0; i--) {
            Toast toast = toasts.get(i);
            float opacity = toast.getOpacity();
            if (opacity <= 0) continue;

            int textWidth = font.width(toast.message);
            int toastWidth = textWidth + TOAST_PADDING * 2 + 16;
            int x = screenWidth - toastWidth - TOAST_MARGIN;

            int alpha = (int) (opacity * 230);
            int bgColor = (alpha << 24) | (toast.type.color & 0x00FFFFFF);
            int textAlpha = (int) (opacity * 255);
            int textColor = (textAlpha << 24) | 0x00FFFFFF;

            context.fill(x, y, x + toastWidth, y + TOAST_HEIGHT, bgColor);

            context.drawString(font, toast.type.icon, x + TOAST_PADDING, y + (TOAST_HEIGHT - 8) / 2, textColor, false);
            context.drawString(font, toast.message, x + TOAST_PADDING + 14, y + (TOAST_HEIGHT - 8) / 2, textColor, false);

            y -= TOAST_HEIGHT + TOAST_MARGIN;
        }
    }

    public boolean hasToasts() {
        return !toasts.isEmpty();
    }

    public void clear() {
        toasts.clear();
    }
}
