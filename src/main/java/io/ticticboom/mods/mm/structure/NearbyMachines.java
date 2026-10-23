package io.ticticboom.mods.mm.structure;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

@EventBusSubscriber(modid = Ref.ID)
public final class NearbyMachines {

    private static final int REFRESH_TICKS = 20;
    private static final Map<ResourceKey<Level>, Set<MachineControllerBlockEntity>> FORMED = new HashMap<>();

    private NearbyMachines() {
    }

    public static void formed(Level level, MachineControllerBlockEntity controller) {
        if (level == null || level.isClientSide()) {
            return;
        }
        FORMED.computeIfAbsent(level.dimension(), k -> Collections.newSetFromMap(new IdentityHashMap<>())).add(controller);
    }

    public static void unformed(Level level, MachineControllerBlockEntity controller) {
        if (level == null || level.isClientSide()) {
            return;
        }
        var controllers = FORMED.get(level.dimension());
        if (controllers != null) {
            controllers.remove(controller);
        }
    }

    public static int count(Level level, BlockPos center, ResourceLocation structureId, int radius) {
        var controllers = FORMED.get(level.dimension());
        if (controllers == null) {
            return 0;
        }
        int found = 0;
        for (var controller : controllers) {
            if (controller.isRemoved() || !controller.isFormed() || controller.getStructure() == null) {
                continue;
            }
            var pos = controller.getBlockPos();
            if (pos.equals(center) || !controller.getStructure().id().equals(structureId)) {
                continue;
            }
            if (Math.abs(pos.getX() - center.getX()) <= radius
                    && Math.abs(pos.getY() - center.getY()) <= radius
                    && Math.abs(pos.getZ() - center.getZ()) <= radius) {
                found++;
            }
        }
        return found;
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        FORMED.clear();
    }

    public static final class Lookup {
        private record Key(ResourceLocation structureId, int radius) {
        }

        private final Level level;
        private final BlockPos center;
        private final Map<Key, Integer> counts = new HashMap<>();
        private long refreshedAt = Long.MIN_VALUE / 2;

        public Lookup(Level level, BlockPos center) {
            this.level = level;
            this.center = center;
        }

        public int count(ResourceLocation structureId, int radius) {
            long now = level.getGameTime();
            if (now - refreshedAt >= REFRESH_TICKS || now < refreshedAt) {
                counts.clear();
                refreshedAt = now;
            }
            return counts.computeIfAbsent(new Key(structureId, radius), key -> NearbyMachines.count(level, center, key.structureId(), key.radius()));
        }
    }
}
