package xyz.nim.telegraph.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.item.map.MapState;

import java.util.*;
import java.util.Objects;
import java.util.function.Consumer;

public class MapDecorationTracker {
    private static final int SAVE_INTERVAL = 1200;

    private static MapDecorationTracker instance;

    private final Map<Integer, Map<String, DecorationSnapshot>> mapCache = new HashMap<>();
    private final List<Consumer<MapDecorationChangeEvent>> listeners = new ArrayList<>();
    private final TelegraphChannel telegraphChannel = new TelegraphChannel();
    private final NotificationManager notificationManager;
    private final PersistenceManager persistenceManager;
    private int tickCounter = 0;
    private int saveCounter = 0;
    private String currentWorldId = null;

    public MapDecorationTracker() {
        this.notificationManager = new NotificationManager(telegraphChannel);
        this.persistenceManager = new PersistenceManager();
        instance = this;
    }

    public static void onMapUpdate(MapIdComponent mapIdComponent) {
        if (instance != null) {
            instance.processMap(mapIdComponent);
        }
    }

    public PersistenceManager getPersistenceManager() {
        return persistenceManager;
    }

    public void registerListener(Consumer<MapDecorationChangeEvent> listener) {
        listeners.add(listener);
    }

    public TelegraphChannel getTelegraphChannel() {
        return telegraphChannel;
    }

    public void onClientTick(MinecraftClient client) {
        String newWorldId = WorldIdentifier.getCurrentWorldId().orElse(null);

        if (!Objects.equals(currentWorldId, newWorldId)) {
            handleWorldChange(newWorldId);
        }

        if (client.player == null || !persistenceManager.hasWorld()) {
            return;
        }

        if (++tickCounter % 20 != 0) {
            return;
        }

        if (++saveCounter >= SAVE_INTERVAL / 20) {
            saveCounter = 0;
            persistenceManager.saveChannelSettings(telegraphChannel);
            persistenceManager.saveMessages(telegraphChannel);
        }
    }

    private void handleWorldChange(String newWorldId) {
        if (currentWorldId != null && persistenceManager.hasWorld()) {
            persistenceManager.saveChannelSettings(telegraphChannel);
            persistenceManager.saveMessages(telegraphChannel);
            persistenceManager.saveCivilizations();
        }

        telegraphChannel.clear();
        mapCache.clear();

        currentWorldId = newWorldId;
        persistenceManager.setCurrentWorldId(newWorldId);

        if (newWorldId != null) {
            persistenceManager.loadChannelSettings(telegraphChannel);
            persistenceManager.loadMessages(telegraphChannel);
            persistenceManager.loadCivilizations();
        }
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
                fireEvent(new MapDecorationChangeEvent(
                    mapId,
                    MapDecorationChangeEvent.ChangeType.ADDED,
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
                    fireEvent(new MapDecorationChangeEvent(
                        mapId,
                        MapDecorationChangeEvent.ChangeType.ADDED,
                        key,
                        newSnapshot.get(key),
                        null
                    ));
                } else {
                    DecorationSnapshot oldDeco = oldSnapshot.get(key);
                    DecorationSnapshot newDeco = newSnapshot.get(key);
                    if (!oldDeco.equals(newDeco)) {
                        fireEvent(new MapDecorationChangeEvent(
                            mapId,
                            MapDecorationChangeEvent.ChangeType.CHANGED,
                            key,
                            newDeco,
                            oldDeco
                        ));
                    }
                }
            }

            for (String key : oldKeys) {
                if (!newKeys.contains(key)) {
                    fireEvent(new MapDecorationChangeEvent(
                        mapId,
                        MapDecorationChangeEvent.ChangeType.REMOVED,
                        key,
                        null,
                        oldSnapshot.get(key)
                    ));
                }
            }
        }

        mapCache.put(mapId, newSnapshot);
    }

    private void fireEvent(MapDecorationChangeEvent event) {
        notificationManager.notifyDecorationChange(event);

        for (Consumer<MapDecorationChangeEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    public void defaultChatHandler(MapDecorationChangeEvent event) {
        TelegraphMessage telegraphMessage = TelegraphMessage.from(event);
        telegraphChannel.addMessage(telegraphMessage);
    }
}
