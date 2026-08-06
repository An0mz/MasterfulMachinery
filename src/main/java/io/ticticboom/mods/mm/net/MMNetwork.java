package io.ticticboom.mods.mm.net;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.net.packet.ProcessesSyncPkt;
import io.ticticboom.mods.mm.net.packet.StructureSyncPkt;
import io.ticticboom.mods.mm.net.packet.ToggleRedstoneModePkt;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * NeoForge replaced Forge's SimpleChannel with typed payloads. Each packet now carries its own
 * CustomPacketPayload.Type and StreamCodec, registration happens on an event rather than in the
 * mod constructor, and the direction is declared up front instead of being implied by the
 * sending code. The protocol version is still negotiated, so mismatched clients are rejected.
 */
@EventBusSubscriber(modid = Ref.ID, bus = EventBusSubscriber.Bus.MOD)
public class MMNetwork {

    private static final String PROTOCOL_VERSION = "1";

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(StructureSyncPkt.TYPE, StructureSyncPkt.STREAM_CODEC, StructureSyncPkt::handle);
        registrar.playToClient(ProcessesSyncPkt.TYPE, ProcessesSyncPkt.STREAM_CODEC, ProcessesSyncPkt::handle);
        registrar.playToServer(ToggleRedstoneModePkt.TYPE, ToggleRedstoneModePkt.STREAM_CODEC, ToggleRedstoneModePkt::handle);
    }
}
