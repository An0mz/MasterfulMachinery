package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.builder.structure.BuildableStructureSync;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.structure.StructureManager;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class DatapackSyncHandler {

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        try {
            StructureManager.validateAllPieces();
        } catch (RuntimeException t) {
            Ref.LOG.error("Error validating structure pieces on datapack sync", t);
        }
        try {
            event.getRelevantPlayers().forEach(BuildableStructureSync::send);
        } catch (RuntimeException t) {
            Ref.LOG.error("Error syncing builder structures on datapack sync", t);
        }
    }
}

