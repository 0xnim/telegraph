package xyz.nim.telegraph.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Consumer;

public class MapDecorationTracker {
    private static final int DEFAULT_SCAN_CADENCE = 5;
    private static final int DEFAULT_HOTBAR_SIZE = 9;
    private static final int MAP_REFRESH_INTERVAL = 1200;
    private static final int MAP_REFRESH_DURATION = 4;
    
    private final Map<Integer, Map<String, DecorationSnapshot>> mapCache = new HashMap<>();
    private final List<Consumer<MapDecorationChangeEvent>> listeners = new ArrayList<>();
    private final TelegraphChannel telegraphChannel = new TelegraphChannel();
    private final NotificationManager notificationManager;
    private final PersistenceManager persistenceManager;
    private final int scanCadence;
    private final int slotsToScan;
    private int tickCounter = 0;
    private int saveCounter = 0;
    private int refreshCounter = 0;
    private int refreshingMapIndex = -1;
    private ItemStack originalOffhandItem = ItemStack.EMPTY;
    private boolean mapRefreshEnabled = false;
    private static final int SAVE_INTERVAL = 1200;
    
    public MapDecorationTracker(int scanCadence, int slotsToScan) {
        this.scanCadence = scanCadence;
        this.slotsToScan = slotsToScan;
        this.notificationManager = new NotificationManager(telegraphChannel);
        this.persistenceManager = new PersistenceManager();
        
        persistenceManager.loadChannelSettings(telegraphChannel);
        persistenceManager.loadMessages(telegraphChannel);
        persistenceManager.loadCivilizations();
        this.mapRefreshEnabled = persistenceManager.loadMapRefreshEnabled();
    }
    
    public MapDecorationTracker() {
        this(DEFAULT_SCAN_CADENCE, DEFAULT_HOTBAR_SIZE);
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
    
    public boolean isMapRefreshEnabled() {
        return mapRefreshEnabled;
    }
    
    public void setMapRefreshEnabled(boolean enabled) {
        this.mapRefreshEnabled = enabled;
        persistenceManager.saveGlobalSettings(enabled);
        if (!enabled) {
            refreshCounter = 0;
            if (refreshingMapIndex >= 0) {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.player != null) {
                    client.player.getInventory().setStack(PlayerInventory.OFF_HAND_SLOT, originalOffhandItem);
                }
                refreshingMapIndex = -1;
                originalOffhandItem = ItemStack.EMPTY;
            }
        }
    }
    
    public void onClientTick(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        
        handleMapRefresh(client, player);
        
        if (++tickCounter % scanCadence != 0) {
            return;
        }
        
        for (int i = 0; i < Math.min(slotsToScan, player.getInventory().size()); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.FILLED_MAP) {
                var mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
                if (mapIdComponent != null) {
                    updateChannelName(stack);
                    processMap(stack);
                }
            }
        }
        
        if (++saveCounter >= SAVE_INTERVAL) {
            saveCounter = 0;
            persistenceManager.saveChannelSettings(telegraphChannel);
            persistenceManager.saveMessages(telegraphChannel);
        }
    }
    
    private void handleMapRefresh(MinecraftClient client, PlayerEntity player) {
        if (!mapRefreshEnabled) {
            return;
        }
        
        if (refreshingMapIndex >= 0) {
            refreshingMapIndex = -1;
            player.getInventory().setStack(PlayerInventory.OFF_HAND_SLOT, originalOffhandItem);
            originalOffhandItem = ItemStack.EMPTY;
            return;
        }
        
        if (++refreshCounter >= MAP_REFRESH_INTERVAL) {
            refreshCounter = 0;
            
            for (int i = 0; i < Math.min(slotsToScan, player.getInventory().size()); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() == Items.FILLED_MAP) {
                    originalOffhandItem = player.getInventory().getStack(PlayerInventory.OFF_HAND_SLOT).copy();
                    player.getInventory().setStack(PlayerInventory.OFF_HAND_SLOT, stack.copy());
                    refreshingMapIndex = i;
                    break;
                }
            }
        }
    }
    
    private void updateChannelName(ItemStack mapStack) {
        var mapIdComponent = mapStack.get(DataComponentTypes.MAP_ID);
        if (mapIdComponent == null) return;
        
        var customNameComponent = mapStack.get(DataComponentTypes.CUSTOM_NAME);
        if (customNameComponent != null) {
            String customName = customNameComponent.getString();
            telegraphChannel.setChannelName(mapIdComponent.id(), customName);
        }
    }
    
    private void processMap(ItemStack mapStack) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        var mapIdComponent = mapStack.get(DataComponentTypes.MAP_ID);
        if (mapIdComponent == null) {
            return;
        }
        int mapId = mapIdComponent.id();
        
        boolean isNewChannel = telegraphChannel.ensureChannelExists(mapId);
        if (isNewChannel) {
            telegraphChannel.addWelcomeMessage(mapId);
        }
        
        MapState mapState = client.world.getMapState(mapIdComponent);
        if (mapState == null) {
            mapCache.putIfAbsent(mapId, new HashMap<>());
            return;
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
