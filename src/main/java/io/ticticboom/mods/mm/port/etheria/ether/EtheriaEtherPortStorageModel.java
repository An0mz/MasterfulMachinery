package io.ticticboom.mods.mm.port.etheria.ether;

import io.ticticboom.mods.mm.port.IPortStorageModel;

public record EtheriaEtherPortStorageModel(
        long capacity,
        long vesselRate
) implements IPortStorageModel {
}
