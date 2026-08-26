package io.ticticboom.mods.mm.util;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.datagen.provider.MMBlockstateProvider;
import io.ticticboom.mods.mm.port.IPortBlock;
import io.ticticboom.mods.mm.setup.RegistryGroupHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

public class PortUtils {

    public static String id(String id, boolean input) {
        var res = id + "_" + (input ? "input" : "output");
        return res;
    }

    public static String name(String name, boolean input) {
        var res = name + " " + (input ? "Input" : "Output");
        return res;
    }

    /**
     * Component form of {@link #name(String, boolean)}. The Input/Output suffix goes through the
     * lang file so translators control both the wording and its position relative to the name.
     */
    public static Component name(Component name, boolean input) {
        return Component.translatable(input ? "port.mm.name_format.input" : "port.mm.name_format.output", name);
    }

    public static void commonGenerateModel(MMBlockstateProvider provider, RegistryGroupHolder groupHolder,
            boolean isInput, ResourceLocation inputOverlay, ResourceLocation outputOverlay) {
        var block = groupHolder.getBlock().get();
        var base = Ref.Textures.BASE_BLOCK;
        var overlay = isInput ? inputOverlay : outputOverlay;
        if (block instanceof IPortBlock portBlock) {
            var model = portBlock.getModel();
            base = Objects.requireNonNullElse(model.baseTexture(), base);
            overlay = Objects.requireNonNullElse(model.overlayTexture(), overlay);
        }
        provider.dynamicBlock(groupHolder.getBlock().getId(), base, overlay);
        provider.simpleBlock(block);
    }
}
