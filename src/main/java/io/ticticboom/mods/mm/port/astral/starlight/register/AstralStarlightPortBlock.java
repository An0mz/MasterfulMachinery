package io.ticticboom.mods.mm.port.astral.starlight.register;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.model.PortModel;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.port.astral.starlight.feature.AstralStarlightReceiverFeature;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.PortUtils;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AstralStarlightPortBlock extends Block implements IPortBlock, EntityBlock {

    @Getter
    private final PortModel model;
    private final RegistryGroupHolder groupHolder;

    public AstralStarlightPortBlock(PortModel model, RegistryGroupHolder groupHolder) {
        super(BlockUtils.createBlockProperties());
        this.model = model;
        this.groupHolder = groupHolder;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        PortUtils.commonGenerateModel(provider, groupHolder, model.input(),
                Ref.Textures.INPUT_ASTRAL_STARLIGHT_PORT_OVERLAY,
                Ref.Textures.OUTPUT_ASTRAL_STARLIGHT_PORT_OVERLAY);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return BlockUtils.commonUse(state, level, pos, player, hitResult, AstralStarlightPortBlockEntity.class, null);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return groupHolder.getBe().get().create(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> entityType) {
        if (level.isClientSide()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof AstralStarlightPortBlockEntity port) {
                port.tick();
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            AstralStarlightReceiverFeature.detach(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public MutableComponent getName() {
        var name = model.displayName();
        return name != null ? name.copy() : super.getName();
    }
}
