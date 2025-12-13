package xyz.nim.telegraph.client.ui;

import org.lwjgl.glfw.GLFW;

public class KeyboardConstants {
    public static final int KEY_ENTER = GLFW.GLFW_KEY_ENTER;
    public static final int KEY_KEYPAD_ENTER = GLFW.GLFW_KEY_KP_ENTER;
    public static final int KEY_ESCAPE = GLFW.GLFW_KEY_ESCAPE;
    public static final int KEY_TAB = GLFW.GLFW_KEY_TAB;
    public static final int KEY_BACKSPACE = GLFW.GLFW_KEY_BACKSPACE;
    public static final int KEY_DELETE = GLFW.GLFW_KEY_DELETE;

    public static final int KEY_UP = GLFW.GLFW_KEY_UP;
    public static final int KEY_DOWN = GLFW.GLFW_KEY_DOWN;
    public static final int KEY_LEFT = GLFW.GLFW_KEY_LEFT;
    public static final int KEY_RIGHT = GLFW.GLFW_KEY_RIGHT;

    public static final int KEY_1 = GLFW.GLFW_KEY_1;
    public static final int KEY_2 = GLFW.GLFW_KEY_2;
    public static final int KEY_3 = GLFW.GLFW_KEY_3;

    public static final int KEY_F = GLFW.GLFW_KEY_F;

    public static final int MOD_CONTROL = GLFW.GLFW_MOD_CONTROL;
    public static final int MOD_SHIFT = GLFW.GLFW_MOD_SHIFT;
    public static final int MOD_ALT = GLFW.GLFW_MOD_ALT;

    public static boolean isEnter(int keyCode) {
        return keyCode == KEY_ENTER || keyCode == KEY_KEYPAD_ENTER;
    }

    public static boolean hasControl(int modifiers) {
        return (modifiers & MOD_CONTROL) != 0;
    }

    public static boolean hasShift(int modifiers) {
        return (modifiers & MOD_SHIFT) != 0;
    }

    public static boolean hasAlt(int modifiers) {
        return (modifiers & MOD_ALT) != 0;
    }
}
