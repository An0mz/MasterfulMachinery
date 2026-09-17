package io.ticticboom.mods.mm.port.ae2.pattern;

import io.ticticboom.mods.mm.port.IPortStorageModel;

public record Ae2PatternPortStorageModel(
        int patternPriority
) implements IPortStorageModel {
}
