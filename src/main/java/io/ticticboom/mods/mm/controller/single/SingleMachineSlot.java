package io.ticticboom.mods.mm.controller.single;

import io.ticticboom.mods.mm.port.IPortStorageFactory;
import net.minecraft.resources.ResourceLocation;

public record SingleMachineSlot(String id, ResourceLocation type, boolean input, IPortStorageFactory factory) {
}
