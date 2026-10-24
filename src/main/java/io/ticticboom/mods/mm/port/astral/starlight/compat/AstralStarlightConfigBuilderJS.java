package io.ticticboom.mods.mm.port.astral.starlight.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.astral.starlight.AstralStarlightPortStorageModel;

public class AstralStarlightConfigBuilderJS extends PortConfigBuilderJS {

    private int capacity;

    public AstralStarlightConfigBuilderJS capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new AstralStarlightPortStorageModel(capacity);
    }
}
