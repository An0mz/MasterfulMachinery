package io.ticticboom.mods.mm.port.mekanism.chemical.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import net.minecraft.network.chat.MutableComponent;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class MekanismChemicalPortBlock extends Block implements EntityBlock, IPortBlock {

    private final PortModel model;
    private final RegistryGroupHolder groupHolder;
    private final boolean isInput;

    public MekanismChemicalPortBlock(PortModel model, RegistryGroupHolder groupHolder, boolean isInput) {
        super(BlockUtils.createBlockProperties());
        this.model = model;
        this.groupHolder = groupHolder;
        this.isInput = isInput;
    }

    @Override
    public void generateModel(io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider provider) {
        io.ticticboom.mods.mm.util.PortUtils.commonGenerateModel(provider, groupHolder, isInput,
                io.ticticboom.mods.mm.Ref.Textures.INPUT_CHEMICAL_PORT_OVERLAY,
                io.ticticboom.mods.mm.Ref.Textures.OUTPUT_CHEMICAL_PORT_OVERLAY);
    }

    @Override
    public PortModel getModel() {
        return model;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return groupHolder.getBe().get().create(blockPos, blockState);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return BlockUtils.commonUse(state, level, pos, player, hitResult, MekanismChemicalPortBlockEntity.class, null);
    }

    /**
     * Matches the block item: the generated block.mm.<id> entry loses a pack-supplied translation
     * key and hardcodes the English Input/Output suffix, so the model name is used directly.
     */
    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        return (a, b, c, d) -> {
            if (d instanceof MekanismChemicalPortBlockEntity pbe) {
                pbe.tick();
            }
        };
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        if (level instanceof Level actual && actual.getBlockEntity(pos) instanceof MekanismChemicalPortBlockEntity pbe) {
            pbe.neighborsChanged();
        }
    }

    @Override
    public MutableComponent getName() {
        var name = model.displayName();
        return name != null ? name.copy() : super.getName();
    }
}
