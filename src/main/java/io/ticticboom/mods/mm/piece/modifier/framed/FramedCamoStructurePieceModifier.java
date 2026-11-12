package io.ticticboom.mods.mm.piece.modifier.framed;

import com.google.gson.JsonObject;
import io.ticticboom.mods.mm.builder.FramedCamo;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.piece.modifier.StructurePieceModifier;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.util.StructurePieceUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FramedCamoStructurePieceModifier extends StructurePieceModifier {

    private final ResourceLocation camoId;
    private final @Nullable ResourceLocation camoTwoId;
    private List<FramedCamo> camos;

    public FramedCamoStructurePieceModifier(ResourceLocation camoId, @Nullable ResourceLocation camoTwoId) {
        this.camoId = camoId;
        this.camoTwoId = camoTwoId;
    }

    @Override
    public String getId() {
        return "framedCamo";
    }

    @Override
    public void validateSetup(StructurePieceSetupMetadata meta, List<Block> requiredBlocks) {
        if (!BuiltInRegistries.BLOCK.containsKey(camoId)) {
            throw new RuntimeException(StructurePieceUtils.errorMessageFor("'camo' field is not a valid block id", meta));
        }
        if (camoTwoId != null && !BuiltInRegistries.BLOCK.containsKey(camoTwoId)) {
            throw new RuntimeException(StructurePieceUtils.errorMessageFor("'camo_two' field is not a valid block id", meta));
        }
    }

    public List<FramedCamo> camos() {
        if (camos == null) {
            var resolved = new ArrayList<FramedCamo>();
            resolved.add(new FramedCamo(BuiltInRegistries.BLOCK.get(camoId), false));
            if (camoTwoId != null) {
                resolved.add(new FramedCamo(BuiltInRegistries.BLOCK.get(camoTwoId), true));
            }
            camos = List.copyOf(resolved);
        }
        return camos;
    }

    @Override
    public boolean formed(Level level, BlockPos pos, StructureModel model, Rotation rotation) {
        return true;
    }

    @Override
    public BlockState modifyBlockState(BlockState state, BlockEntity be, BlockPos pos) {
        return state;
    }

    @Override
    public BlockEntity modifyBlockEntity(BlockState state, BlockEntity be, BlockPos pos) {
        return be;
    }

    @Override
    public JsonObject debugExpected(Level level, BlockPos pos, StructureModel model, Rotation rotation, JsonObject json) {
        json.addProperty("camo", camoId.toString());
        if (camoTwoId != null) {
            json.addProperty("camo_two", camoTwoId.toString());
        }
        return json;
    }

    @Override
    public JsonObject debugFound(Level level, BlockPos pos, StructureModel model, Rotation rotation, JsonObject json) {
        return json;
    }
}
