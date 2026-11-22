package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.ticticboom.mods.mm.Ref;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.LinkedHashMap;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
public final class FxBuffers {
    private static final LinkedHashMap<RenderType, ByteBufferBuilder> FIXED = new LinkedHashMap<>();
    private static final MultiBufferSource.BufferSource SOURCE = MultiBufferSource.immediateWithBuffers(FIXED, new ByteBufferBuilder(1536));
    private static boolean used;

    static final MultiBufferSource BUFFERS = FxBuffers::buffer;

    private FxBuffers() {
    }

    private static VertexConsumer buffer(RenderType type) {
        if (!FIXED.containsKey(type)) {
            FIXED.put(type, new ByteBufferBuilder(type.bufferSize()));
        }
        used = true;
        return SOURCE.getBuffer(type);
    }

    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES && used) {
            used = false;
            long start = System.nanoTime();
            SOURCE.endBatch();
            FxStats.nanos += System.nanoTime() - start;
        }
    }
}
