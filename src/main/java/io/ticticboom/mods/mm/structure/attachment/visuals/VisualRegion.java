package io.ticticboom.mods.mm.structure.attachment.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureKeyChar;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

@Getter
public class VisualRegion {
    private final @Nullable BlockPos from;
    private final @Nullable BlockPos to;
    private final @Nullable String area;
    @Getter(lombok.AccessLevel.NONE)
    private final Map<Rotation, long[]> cellCache = new EnumMap<>(Rotation.class);

    public VisualRegion(@Nullable BlockPos from, @Nullable BlockPos to, @Nullable String area) {
        this.from = from;
        this.to = to;
        this.area = area;
    }

    public static VisualRegion parse(JsonObject json) {
        if (json.has("area")) {
            return new VisualRegion(null, null, json.get("area").getAsString());
        }
        return new VisualRegion(parsePos(json, "from"), parsePos(json, "to"), null);
    }

    private static BlockPos parsePos(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray() || json.getAsJsonArray(key).size() != 3) {
            throw new IllegalArgumentException("Structure visual needs 'area', or 'from' and 'to' as [x, y, z]");
        }
        JsonArray array = json.getAsJsonArray(key);
        return new BlockPos(array.get(0).getAsInt(), array.get(1).getAsInt(), array.get(2).getAsInt());
    }

    public BlockPos min() {
        return new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
    }

    public BlockPos max() {
        return new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()), Math.max(from.getZ(), to.getZ()));
    }

    public long[] relativeCells(StructureLayout layout, Rotation rotation) {
        long[] cells = cellCache.get(rotation);
        if (cells == null) {
            cells = area != null ? areaCells(layout, rotation) : boxCells(layout, rotation);
            cellCache.put(rotation, cells);
        }
        return cells;
    }

    private long[] areaCells(StructureLayout layout, Rotation rotation) {
        return layout.getRotatedPositionedPieces().get(rotation).stream()
                .filter(piece -> area.equals(piece.piece().valueId()))
                .map(PositionedLayoutPiece::pos)
                .mapToLong(BlockPos::asLong)
                .toArray();
    }

    private long[] boxCells(StructureLayout layout, Rotation rotation) {
        var origin = layout.getCharGrid().getControllerPos();
        var min = min();
        var max = max();
        var cells = new long[(max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1)];
        int i = 0;
        for (var pos : BlockPos.betweenClosed(min, max)) {
            cells[i++] = pos.subtract(origin).rotate(rotation).asLong();
        }
        return cells;
    }

    public void validate(ResourceLocation structureId, StructureLayout layout, String type, String hidden) {
        if (area != null) {
            if (!isKey(layout, area)) {
                Ref.LOG.warn("Structure {}: {} area '{}' is not a key in the layout", structureId, type, area);
            }
            return;
        }
        var min = min();
        var max = max();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (layout.getCharGrid().valueFromBottom(x, y, z) == ' ') {
                        Ref.LOG.warn("Structure {}: {} from {} to {} covers spaces in the layout, so blocks placed there will hide the {}. Use a key that requires minecraft:air.",
                                structureId, type, from.toShortString(), to.toShortString(), hidden);
                        return;
                    }
                }
            }
        }
    }

    static boolean isKey(StructureLayout layout, String key) {
        return key.length() == 1 && layout.getPieces().containsKey(new StructureKeyChar(key.charAt(0)));
    }

    public void writeJson(JsonObject json) {
        if (area != null) {
            json.addProperty("area", area);
        } else {
            json.add("from", posJson(from));
            json.add("to", posJson(to));
        }
    }

    private static JsonArray posJson(BlockPos pos) {
        var array = new JsonArray();
        array.add(pos.getX());
        array.add(pos.getY());
        array.add(pos.getZ());
        return array;
    }
}
