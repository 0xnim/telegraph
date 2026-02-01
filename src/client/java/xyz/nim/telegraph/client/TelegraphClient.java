package xyz.nim.telegraph.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class TelegraphClient implements ClientModInitializer {

    private static final MapDecorationTracker MAP_DECORATION_TRACKER = new MapDecorationTracker();
    private static KeyMapping openMenuKey;

    @Override
    public void onInitializeClient() {
        MAP_DECORATION_TRACKER.registerListener(MAP_DECORATION_TRACKER::defaultChatHandler);
        ClientTickEvents.END_CLIENT_TICK.register(MAP_DECORATION_TRACKER::onClientTick);

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.telegraph.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_M,
            "category.telegraph"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenuKey.consumeClick()) {
                client.setScreen(new MapDecorationsScreen(MAP_DECORATION_TRACKER.getTelegraphChannel()));
            }
        });
    }

    public static MapDecorationTracker getMapDecorationTracker() {
        return MAP_DECORATION_TRACKER;
    }

    public static PersistenceManager getPersistenceManager() {
        return MAP_DECORATION_TRACKER.getPersistenceManager();
    }
}
