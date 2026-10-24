package io.ticticboom.mods.mm.port.astral.lumen;

import io.ticticboom.mods.mm.port.IPortStorageModel;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record AstralLumenPortStorageModel(
        int capacity,
        List<ResourceLocation> filter
) implements IPortStorageModel {
}
