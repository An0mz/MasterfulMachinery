package io.ticticboom.mods.mm.controller.machine.register;

import io.ticticboom.mods.mm.util.DisplayNameUtil;
import net.minecraft.network.chat.MutableComponent;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.IControllerBlock;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.model.ControllerModel;
import io.ticticboom.mods.mm.port.kinetic.register.CreateKineticGenPortBlockEntity;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import io.ticticboom.mods.mm.util.BlockUtils;
import io.ticticboom.mods.mm.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import java.util.EnumMap;
import java.util.Objects;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;

public class MachineControllerBlock extends HorizontalDirectionalBlock implements IControllerPart, IControllerBlock {

    /**
     * HorizontalDirectionalBlock declares codec() abstract in 1.20.5+. MM controller blocks are
     * built from data and registered programmatically rather than decoded from a codec, so this
     * exists only to satisfy the contract and is never used to construct one.
     */
    @Override
    protected com.mojang.serialization.MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return com.mojang.serialization.MapCodec.unit(this);
    }
    private final ControllerModel model;
    private final RegistryGroupHolder groupHolder;

    public MachineControllerBlock(ControllerModel model, RegistryGroupHolder groupHolder) {
        super(BlockUtils.createBlockProperties());
        this.model = model;
        this.groupHolder = groupHolder;
        registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ControllerState.PROPERTY, ControllerState.UNFORMED));
    }

    @Override
    public ControllerModel getModel() {
        return model;
    }

    @Override
    public void generateModel(MMBlockstateProvider provider) {
        var id = groupHolder.getBlock().getId();
        var models = new EnumMap<ControllerState, ModelFile>(ControllerState.class);
        var custom = model.customModel();
        var base = Objects.requireNonNullElse(model.baseTexture(), Ref.Textures.BASE_BLOCK);
        if (custom != null) {
            var mdl = provider.customBlock(id, custom);
            for (ControllerState state : ControllerState.values()) {
                models.put(state, mdl);
            }
        } else if (model.overlayTexture() != null) {
            var mdl = provider.dynamicBlockNorthOverlay(id, base, model.overlayTexture());
            for (ControllerState state : ControllerState.values()) {
                models.put(state, mdl);
            }
        } else {
            for (ControllerState state : ControllerState.values()) {
                var loc = state == ControllerState.UNFORMED ? id : id.withSuffix("_" + state.getSerializedName());
                models.put(state, provider.controllerModel(loc, base, Ref.Textures.CONTROLLER_FRAME,
                        Ref.Textures.controllerScreen(state.getSerializedName())));
            }
        }
        provider.getVariantBuilder(groupHolder.getBlock().get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(models.get(state.getValue(ControllerState.PROPERTY)))
                        .rotationY((int) state.getValue(FACING).toYRot())
                        .build());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ControllerState.PROPERTY);
    }


    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof MachineControllerBlockEntity controller) {
            serverPlayer.openMenu(controller, buf -> MachineControllerMenu.writeOpenData(buf, controller));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return groupHolder.getBe().get().create(blockPos, blockState);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type == groupHolder.getBe().get()) {
            return (l, pos, s, be) -> ((MachineControllerBlockEntity) be).tick();
        }
        return null;
    }

    public boolean usesTintedScreen() {
        return model.customModel() == null && model.overlayTexture() == null;
    }

    @Override
    public void onRemove(BlockState oldState, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (oldState.getBlock() != newState.getBlock()) {
            if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
                var be = WorldUtil.getBlockEntity(pos, serverLevel);
                if (be instanceof MachineControllerBlockEntity mbe) {
                    mbe.invalidateProgress();
                }
            }

            super.onRemove(oldState, level, pos, newState, isMoving);
        }
    }

    /**
     * Matches the block item: plain string names keep using the generated block.mm.<id> entry so
     * resource packs can override them; only a pack-supplied translation key bypasses it.
     */
    @Override
    public MutableComponent getName() {
        var name = DisplayNameUtil.packSuppliedName(model.displayName());
        return name != null ? name.copy() : super.getName();
    }
}
