package xyz.nim.telegram.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.map.MapState;
import net.minecraft.text.Text;

import java.util.*;
import java.util.function.Consumer;

public class BannerTracker {
    private static final int DEFAULT_SCAN_CADENCE = 5;
    private static final int DEFAULT_HOTBAR_SIZE = 9;
    
    private final Map<Integer, Map<String, DecorationSnapshot>> mapCache = new HashMap<>();
    private final List<Consumer<BannerChangeEvent>> listeners = new ArrayList<>();
    private final TelegramChannel telegramChannel = new TelegramChannel();
    private final NotificationManager notificationManager;
    private final int scanCadence;
    private final int slotsToScan;
    private int tickCounter = 0;
    
    public BannerTracker(int scanCadence, int slotsToScan) {
        this.scanCadence = scanCadence;
        this.slotsToScan = slotsToScan;
        this.notificationManager = new NotificationManager(telegramChannel);
    }
    
    public BannerTracker() {
        this(DEFAULT_SCAN_CADENCE, DEFAULT_HOTBAR_SIZE);
    }
    
    public void registerListener(Consumer<BannerChangeEvent> listener) {
        listeners.add(listener);
    }
    
    public TelegramChannel getTelegramChannel() {
        return telegramChannel;
    }
    
    public void onClientTick(MinecraftClient client) {
        if (++tickCounter % scanCadence != 0) {
            return;
        }
        
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        
        Set<Integer> currentMaps = new HashSet<>();
        for (int i = 0; i < Math.min(slotsToScan, player.getInventory().size()); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.FILLED_MAP) {
                var mapIdComponent = stack.get(DataComponentTypes.MAP_ID);
                if (mapIdComponent != null) {
                    currentMaps.add(mapIdComponent.id());
                    updateChannelName(stack);
                    processMap(stack);
                }
            }
        }
        
        mapCache.keySet().retainAll(currentMaps);
    }
    
    private void updateChannelName(ItemStack mapStack) {
        var mapIdComponent = mapStack.get(DataComponentTypes.MAP_ID);
        if (mapIdComponent == null) return;
        
        var customNameComponent = mapStack.get(DataComponentTypes.CUSTOM_NAME);
        if (customNameComponent != null) {
            String customName = customNameComponent.getString();
            telegramChannel.setChannelName(mapIdComponent.id(), customName);
        }
    }
    
    private void processMap(ItemStack mapStack) {
        MinecraftClient client = MinecraftClient.getInstance();
        
        var mapIdComponent = mapStack.get(DataComponentTypes.MAP_ID);
        if (mapIdComponent == null) {
            return;
        }
        int mapId = mapIdComponent.id();
        
        boolean isNewChannel = telegramChannel.ensureChannelExists(mapId);
        if (isNewChannel) {
            telegramChannel.addWelcomeMessage(mapId);
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
        TelegramMessage telegramMessage = TelegramMessage.from(event);
        telegramChannel.addMessage(telegramMessage);
    }
}
