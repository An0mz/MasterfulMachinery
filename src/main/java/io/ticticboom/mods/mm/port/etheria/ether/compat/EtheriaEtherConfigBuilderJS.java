package io.ticticboom.mods.mm.port.etheria.ether.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.etheria.ether.EtheriaEtherPortStorageModel;

public class EtheriaEtherConfigBuilderJS extends PortConfigBuilderJS {

    private long capacity;
    private long vesselRate = 0;

    public EtheriaEtherConfigBuilderJS capacity(long capacity) {
        this.capacity = capacity;
        return this;
    }

    public EtheriaEtherConfigBuilderJS vesselRate(long vesselRate) {
        this.vesselRate = vesselRate;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new EtheriaEtherPortStorageModel(capacity, vesselRate);
    }
}
