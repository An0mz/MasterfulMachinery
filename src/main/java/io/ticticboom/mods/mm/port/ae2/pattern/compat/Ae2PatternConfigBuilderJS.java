package io.ticticboom.mods.mm.port.ae2.pattern.compat;

import io.ticticboom.mods.mm.compat.kjs.builder.PortConfigBuilderJS;
import io.ticticboom.mods.mm.port.IPortStorageModel;
import io.ticticboom.mods.mm.port.ae2.pattern.Ae2PatternPortStorageModel;

public class Ae2PatternConfigBuilderJS extends PortConfigBuilderJS {

    private int patternPriority = 0;

    public Ae2PatternConfigBuilderJS patternPriority(int patternPriority) {
        this.patternPriority = patternPriority;
        return this;
    }

    @Override
    public IPortStorageModel build() {
        return new Ae2PatternPortStorageModel(patternPriority);
    }
}
