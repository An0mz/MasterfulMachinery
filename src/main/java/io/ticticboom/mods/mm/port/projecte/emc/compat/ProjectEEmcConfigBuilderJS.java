package io.ticticboom.mods.mm.port.projecte.emc.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.projecte.emc.ProjectEEmcPortStorageModel;

public class ProjectEEmcConfigBuilderJS extends PortConfigBuilderJS {

    private long capacity;

    public ProjectEEmcConfigBuilderJS capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new ProjectEEmcPortStorageModel(capacity);
    }
}
