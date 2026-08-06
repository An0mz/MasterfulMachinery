package io.ticticboom.mods.mm.datagen.provider;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.extra.IExtraBlockPart;
import io.ticticboom.mods.mm.controller.IControllerPart;
import io.ticticboom.mods.mm.port.IPortPart;
import io.ticticboom.mods.mm.setup.MMRegisters;
import net.minecraft.data.DataGenerator;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredHolder;

public class MMLangProvider extends LanguageProvider {
    public MMLangProvider(DataGenerator generator, String locale) {
        super(generator.getPackOutput(), Ref.ID, locale);
    }

    @Override
    protected void addTranslations() {
        for (DeferredHolder<Block, Block> entry : MMRegisters.BLOCKS.getEntries()) {
            if (entry.get() instanceof IControllerPart controllerPart) {
                this.add(entry.get(), controllerPart.getModel().name());
            }
            if (entry.get() instanceof IPortPart part) {
                this.add(entry.get(), part.getModel().name());
            }
            if (entry.get() instanceof IExtraBlockPart ebp) {
                this.add(entry.get(), ebp.getModel().name());
            }
        }

        this.add(MMRegisters.BLUEPRINT.get(), "Blueprint");
    }
}
