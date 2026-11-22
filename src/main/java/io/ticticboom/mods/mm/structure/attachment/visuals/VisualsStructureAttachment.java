package io.ticticboom.mods.mm.structure.attachment.visuals;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.piece.StructurePieceSetupMetadata;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import io.ticticboom.mods.mm.port.fluid.FluidPortStorage;
import io.ticticboom.mods.mm.recipe.RecipeStorages;
import io.ticticboom.mods.mm.structure.StructureModel;
import io.ticticboom.mods.mm.structure.attachment.StructureAttachment;
import io.ticticboom.mods.mm.structure.layout.PositionedLayoutPiece;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VisualsStructureAttachment extends StructureAttachment {
    @Getter
    private final List<FluidVolumeVisual> fluidVolumes;
    @Getter
    private final List<ParticleVisual> particles;
    @Getter
    private final List<EffectVisual> effects;

    public VisualsStructureAttachment(List<FluidVolumeVisual> fluidVolumes, List<ParticleVisual> particles, List<EffectVisual> effects) {
        this.fluidVolumes = fluidVolumes;
        this.particles = particles;
        this.effects = effects;
    }

    @Override
    public ResourceLocation getId() {
        return Ref.StructureAttachments.VISUALS;
    }

    @Override
    public void validate(StructurePieceSetupMetadata meta) {
        var layout = meta.model().layout();
        for (FluidVolumeVisual volume : fluidVolumes) {
            if (volume.getPort() != null && !VisualRegion.isKey(layout, volume.getPort())) {
                Ref.LOG.warn("Structure {}: fluid_volume port '{}' is not a key in the layout", meta.structureId(), volume.getPort());
            }
            volume.getRegion().validate(meta.structureId(), layout, "fluid_volume", "fluid");
        }
        for (ParticleVisual particle : particles) {
            particle.validate(meta.structureId(), layout);
        }
        for (EffectVisual effect : effects) {
            effect.validate(meta.structureId(), layout);
        }
    }

    public List<FluidVolumeState> snapshot(Level level, BlockPos controllerPos, StructureModel structure, Rotation rotation, RecipeStorages storages) {
        var layout = structure.layout();
        var result = new ArrayList<FluidVolumeState>(fluidVolumes.size());
        for (FluidVolumeVisual volume : fluidVolumes) {
            var tanks = volume.getPort() == null ? allFluidStorages(storages) : keyedFluidStorages(level, controllerPos, layout, rotation, volume.getPort());
            FluidStack shown = FluidStack.EMPTY;
            long amount = 0;
            long capacity = 0;
            for (FluidPortStorage tank : tanks) {
                var handler = tank.getHandler();
                for (int i = 0; i < handler.getTanks(); i++) {
                    capacity += handler.getTankCapacity(i);
                    var stack = handler.getFluidInTank(i);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    if (shown.isEmpty()) {
                        shown = stack.copyWithAmount(1);
                    }
                    if (FluidStack.isSameFluidSameComponents(shown, stack)) {
                        amount += stack.getAmount();
                    }
                }
            }
            float fill = capacity <= 0 ? 0f : (float) Math.min(1.0, (double) amount / capacity);
            result.add(new FluidVolumeState(volume.relativeCells(layout, rotation), shown, fill));
        }
        return result;
    }

    public List<FluidStack> effectFluids(Level level, BlockPos controllerPos, StructureModel structure, Rotation rotation, List<FluidStack> current) {
        boolean any = false;
        for (int e = 0; e < effects.size(); e++) {
            any |= effects.get(e).getPort() != null;
        }
        if (!any) {
            return List.of();
        }
        List<FluidStack> result = current.size() == effects.size() ? current : null;
        for (int e = 0; e < effects.size(); e++) {
            FluidStack shown = firstFluid(level, controllerPos, structure.layout(), rotation, effects.get(e).getPort());
            FluidStack before = result == null ? FluidStack.EMPTY : result.get(e);
            if (result != null && FluidStack.isSameFluidSameComponents(before, shown)) {
                continue;
            }
            if (result == current) {
                result = new ArrayList<>(current);
            } else if (result == null) {
                result = new ArrayList<>(effects.size());
                for (int i = 0; i < effects.size(); i++) {
                    result.add(FluidStack.EMPTY);
                }
            }
            result.set(e, shown.isEmpty() ? FluidStack.EMPTY : shown.copyWithAmount(1));
        }
        return result;
    }

    private static FluidStack firstFluid(Level level, BlockPos controllerPos, StructureLayout layout, Rotation rotation, @Nullable String key) {
        if (key == null) {
            return FluidStack.EMPTY;
        }
        for (PositionedLayoutPiece piece : layout.getRotatedPositionedPieces().get(rotation)) {
            if (!key.equals(piece.piece().valueId())) {
                continue;
            }
            if (level.getBlockEntity(piece.findAbsolutePos(controllerPos)) instanceof IPortBlockEntity port
                    && port.getStorage() instanceof FluidPortStorage storage) {
                var handler = storage.getHandler();
                for (int i = 0; i < handler.getTanks(); i++) {
                    var stack = handler.getFluidInTank(i);
                    if (!stack.isEmpty()) {
                        return stack;
                    }
                }
            }
        }
        return FluidStack.EMPTY;
    }

    public static boolean sameFluids(List<FluidStack> a, List<FluidStack> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!FluidStack.isSameFluidSameComponents(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    private static List<FluidPortStorage> allFluidStorages(RecipeStorages storages) {
        var result = new ArrayList<FluidPortStorage>(storages.getInputStorages(FluidPortStorage.class));
        result.addAll(storages.getOutputStorages(FluidPortStorage.class));
        return result;
    }

    private static List<FluidPortStorage> keyedFluidStorages(Level level, BlockPos controllerPos, StructureLayout layout, Rotation rotation, String key) {
        var result = new ArrayList<FluidPortStorage>();
        for (PositionedLayoutPiece piece : layout.getRotatedPositionedPieces().get(rotation)) {
            if (!key.equals(piece.piece().valueId())) {
                continue;
            }
            if (level.getBlockEntity(piece.findAbsolutePos(controllerPos)) instanceof IPortBlockEntity port
                    && port.getStorage() instanceof FluidPortStorage storage) {
                result.add(storage);
            }
        }
        return result;
    }
}
