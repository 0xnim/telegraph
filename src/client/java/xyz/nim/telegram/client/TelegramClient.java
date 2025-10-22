package xyz.nim.telegram.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class TelegramClient implements ClientModInitializer {
    
    private static final BannerTracker BANNER_TRACKER = new BannerTracker();
    private static final MapDecorationTracker MAP_DECORATION_TRACKER = new MapDecorationTracker();
    private static KeyBinding openMenuKey;

    @Override
    public void onInitializeClient() {
        BANNER_TRACKER.registerListener(BANNER_TRACKER::defaultChatHandler);
        ClientTickEvents.END_CLIENT_TICK.register(BANNER_TRACKER::onClientTick);
        
        MAP_DECORATION_TRACKER.registerListener(MAP_DECORATION_TRACKER::defaultChatHandler);
        ClientTickEvents.END_CLIENT_TICK.register(MAP_DECORATION_TRACKER::onClientTick);
        
        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.telegram.open_menu",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.telegram"
        ));
        
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.wasPressed()) {
                client.setScreen(new MapDecorationsScreen(MAP_DECORATION_TRACKER.getTelegramChannel()));
            }
        });
    }
    
    public static BannerTracker getBannerTracker() {
        return BANNER_TRACKER;
    }
    
    public static MapDecorationTracker getMapDecorationTracker() {
        return MAP_DECORATION_TRACKER;
    }
}
