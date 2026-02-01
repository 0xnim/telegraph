package xyz.nim.telegraph.mixin.client;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nim.telegraph.client.MapDecorationTracker;

@Mixin(ClientPacketListener.class)
public class MapUpdateMixin {

    @Inject(method = "handleMapItemData", at = @At("TAIL"))
    private void onMapUpdate(ClientboundMapItemDataPacket packet, CallbackInfo ci) {
        MapId mapId = packet.mapId();
        MapDecorationTracker.onMapUpdate(mapId);
    }
}
