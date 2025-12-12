package xyz.nim.telegraph.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;

import java.util.*;
import java.util.function.Consumer;

public class BannerTracker {
    private static BannerTracker instance;

    private final Map<Integer, Map<String, DecorationSnapshot>> mapCache = new HashMap<>();
    private final List<Consumer<BannerChangeEvent>> listeners = new ArrayList<>();
    private final TelegraphChannel telegraphChannel = new TelegraphChannel();
    private final NotificationManager notificationManager;

    public BannerTracker() {
        this.notificationManager = new NotificationManager(telegraphChannel);
        instance = this;
    }

    public static void onMapUpdate(MapIdComponent mapIdComponent) {
        if (instance != null) {
            instance.processMap(mapIdComponent);
        }
    }

    public void registerListener(Consumer<BannerChangeEvent> listener) {
        listeners.add(listener);
    }

    public TelegraphChannel getTelegraphChannel() {
        return telegraphChannel;
    }

    private void processMap(MapIdComponent mapIdComponent) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.world == null) {
            return;
        }

        int mapId = mapIdComponent.id();

        MapState mapState = client.world.getMapState(mapIdComponent);
        if (mapState == null) {
            return;
        }

        boolean isNewChannel = telegraphChannel.ensureChannelExists(mapId);
        if (isNewChannel) {
            telegraphChannel.addWelcomeMessage(mapId);
        }

        Map<String, DecorationSnapshot> newSnapshot = new HashMap<>();

        for (var decoration : mapState.getDecorations()) {
            String assetId = decoration.type().value().assetId().toString();

            if (assetId.contains("banner")) {
                String name = decoration.name().map(text -> text.getString()).orElse(null);
                String key = assetId + "_" + decoration.x() + "_" + decoration.z();
                newSnapshot.put(key, new DecorationSnapshot(
                    assetId,
                    decoration.x(),
                    decoration.z(),
                    decoration.rotation(),
                    name
                ));
            }
        }

        Map<String, DecorationSnapshot> oldSnapshot = mapCache.get(mapId);

        if (oldSnapshot == null) {
            for (var entry : newSnapshot.entrySet()) {
                fireEvent(new BannerChangeEvent(
                    mapId,
                    BannerChangeEvent.ChangeType.ADDED,
                    entry.getKey(),
                    entry.getValue(),
                    null
                ));
            }
        } else {
            Set<String> oldKeys = new HashSet<>(oldSnapshot.keySet());
            Set<String> newKeys = new HashSet<>(newSnapshot.keySet());

            for (String key : newKeys) {
                if (!oldKeys.contains(key)) {
                    fireEvent(new BannerChangeEvent(
                        mapId,
                        BannerChangeEvent.ChangeType.ADDED,
                        key,
                        newSnapshot.get(key),
                        null
                    ));
                } else {
                    DecorationSnapshot oldDeco = oldSnapshot.get(key);
                    DecorationSnapshot newDeco = newSnapshot.get(key);
                    if (!oldDeco.equals(newDeco)) {
                        fireEvent(new BannerChangeEvent(
                            mapId,
                            BannerChangeEvent.ChangeType.CHANGED,
                            key,
                            newDeco,
                            oldDeco
                        ));
                    }
                }
            }

            for (String key : oldKeys) {
                if (!newKeys.contains(key)) {
                    fireEvent(new BannerChangeEvent(
                        mapId,
                        BannerChangeEvent.ChangeType.REMOVED,
                        key,
                        null,
                        oldSnapshot.get(key)
                    ));
                }
            }
        }

        mapCache.put(mapId, newSnapshot);
    }

    private void fireEvent(BannerChangeEvent event) {
        notificationManager.notifyBannerChange(event);

        for (Consumer<BannerChangeEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    public void defaultChatHandler(BannerChangeEvent event) {
        TelegraphMessage telegraphMessage = TelegraphMessage.from(event);
        telegraphChannel.addMessage(telegraphMessage);
    }
}
