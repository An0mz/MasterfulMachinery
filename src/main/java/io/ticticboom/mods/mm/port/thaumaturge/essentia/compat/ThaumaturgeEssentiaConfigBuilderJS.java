package io.ticticboom.mods.mm.port.thaumaturge.essentia.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.thaumaturge.essentia.ThaumaturgeEssentiaPortStorageModel;

public class ThaumaturgeEssentiaConfigBuilderJS extends PortConfigBuilderJS {

    private int capacity;
    private int suction = ThaumaturgeEssentiaPortStorageModel.DEFAULT_SUCTION;
    private int transferRate = ThaumaturgeEssentiaPortStorageModel.DEFAULT_TRANSFER_RATE;

    public ThaumaturgeEssentiaConfigBuilderJS capacity(int capacity) {
        this.capacity = capacity;
        return this;
    }

    public ThaumaturgeEssentiaConfigBuilderJS suction(int suction) {
        this.suction = suction;
        return this;
    }

    public ThaumaturgeEssentiaConfigBuilderJS transferRate(int transferRate) {
        this.transferRate = transferRate;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new ThaumaturgeEssentiaPortStorageModel(capacity, suction, transferRate);
    }
}
