package io.ticticboom.mods.mm.port.astral.lumen.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.astral.lumen.AstralLumenPortStorageModel;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class AstralLumenConfigBuilderJS extends PortConfigBuilderJS {

    private int capacity;
    private final List<ResourceLocation> filter = new ArrayList<>();

    public AstralLumenConfigBuilderJS capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public AstralLumenConfigBuilderJS lumen(String... lumen) {
        for (String id : lumen) {
            filter.add(ResourceLocation.parse(id));
        }
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new AstralLumenPortStorageModel(capacity, List.copyOf(filter));
    }
}
