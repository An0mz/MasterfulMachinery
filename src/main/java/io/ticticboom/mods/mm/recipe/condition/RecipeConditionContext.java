package io.ticticboom.mods.mm.recipe.condition;

import io.ticticboom.mods.mm.structure.NearbyMachines;
import io.ticticboom.mods.mm.structure.StructureModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public record RecipeConditionContext(Level level, @Nullable BlockPos pos, @Nullable StructureModel structure, @Nullable NearbyMachines.Lookup nearby) {
    public RecipeConditionContext(Level level, @Nullable BlockPos pos, @Nullable StructureModel structure) {
        this(level, pos, structure, null);
    }
}
