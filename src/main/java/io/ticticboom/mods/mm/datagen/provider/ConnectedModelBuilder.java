package io.ticticboom.mods.mm.datagen.provider;

import io.ticticboom.mods.mm.Ref;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ConnectedModelBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {

    public static <T extends ModelBuilder<T>> ConnectedModelBuilder<T> begin(T parent, ExistingFileHelper existingFileHelper) {
        return new ConnectedModelBuilder<>(parent, existingFileHelper);
    }

    protected ConnectedModelBuilder(T parent, ExistingFileHelper existingFileHelper) {
        super(Ref.id("connected"), parent, existingFileHelper, false);
    }
}
