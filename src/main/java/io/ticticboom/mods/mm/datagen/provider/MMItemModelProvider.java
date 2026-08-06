package io.ticticboom.mods.mm.datagen.provider;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.extra.IExtraBlockPart;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MMItemModelProvider extends ItemModelProvider {
    private final DataGenerator generator;

    public MMItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {
        super(generator.getPackOutput(), Ref.ID, existingFileHelper);
        this.generator = generator;
    }

    @Override
    protected void registerModels() {
        for (DeferredHolder<Item, ? extends Item> entry : MMRegisters.ITEMS.getEntries()) {
            if (entry.get() instanceof IControllerPart controllerPart) {
                String id = controllerPart.getModel().id();
                this.getBuilder(Ref.id(id).toString()).parent(new ModelFile.UncheckedModelFile(Ref.id("block/" + id)));

            }
            if (entry.get() instanceof IPortPart portPart) {
                String id = portPart.getModel().id();
                this.getBuilder(Ref.id(id).toString()).parent(new ModelFile.UncheckedModelFile(Ref.id("block/" + id)));
            }
            if (entry.get() instanceof IExtraBlockPart ebp) {
                String id = ebp.getModel().id();
                this.getBuilder(Ref.id(id).toString()).parent(new ModelFile.UncheckedModelFile(Ref.id("block/" + id)));
            }
        }
    }
}