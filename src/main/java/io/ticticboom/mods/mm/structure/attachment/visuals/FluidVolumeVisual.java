package io.ticticboom.mods.mm.structure.attachment.visuals;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

@Getter
public class FluidVolumeVisual {
    private final VisualRegion region;
    private final @Nullable String port;

    public FluidVolumeVisual(@Nullable BlockPos from, @Nullable BlockPos to, @Nullable String area, @Nullable String port) {
        this(new VisualRegion(from, to, area), port);
    }

    public FluidVolumeVisual(VisualRegion region, @Nullable String port) {
        this.region = region;
        this.port = port;
    }

    public static FluidVolumeVisual parse(JsonObject json) {
        String port = json.has("port") ? json.get("port").getAsString() : null;
        return new FluidVolumeVisual(VisualRegion.parse(json), port);
    }

    public long[] relativeCells(StructureLayout layout, Rotation rotation) {
        return region.relativeCells(layout, rotation);
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", "fluid_volume");
        region.writeJson(json);
        if (port != null) {
            json.addProperty("port", port);
        }
        return json;
    }
}
