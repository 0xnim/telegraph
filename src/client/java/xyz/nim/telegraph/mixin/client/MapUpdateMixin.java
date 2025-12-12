package xyz.nim.telegraph.mixin.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.component.type.MapIdComponent;
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.nim.telegraph.client.BannerTracker;
import xyz.nim.telegraph.client.MapDecorationTracker;

@Mixin(ClientPlayNetworkHandler.class)
public class MapUpdateMixin {

    @Inject(method = "onMapUpdate", at = @At("TAIL"))
    private void onMapUpdate(MapUpdateS2CPacket packet, CallbackInfo ci) {
        MapIdComponent mapId = packet.mapId();
        MapDecorationTracker.onMapUpdate(mapId);
        BannerTracker.onMapUpdate(mapId);
    }
}
