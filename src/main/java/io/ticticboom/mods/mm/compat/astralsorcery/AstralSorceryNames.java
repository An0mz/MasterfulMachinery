package io.ticticboom.mods.mm.compat.astralsorcery;

import hellfirepvp.astralsorcery.common.constellation.BaseConstellation;
import hellfirepvp.astralsorcery.common.lib.RegistriesAS;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class AstralSorceryNames {

    public static Component constellation(ResourceLocation id) {
        return RegistriesAS.REGISTRY_CONSTELLATIONS.getOptional(id)
                .<Component>map(BaseConstellation::getColoredName)
                .orElseGet(() -> Component.literal(id.toString()));
    }

    public static Component lumen(ResourceLocation id) {
        return RegistriesAS.REGISTRY_LUMEN.getOptional(id)
                .map(Lumen::getName)
                .orElseGet(() -> Component.literal(id.toString()));
    }

    public static BaseConstellation requireConstellation(ResourceLocation id) {
        return RegistriesAS.REGISTRY_CONSTELLATIONS.getOptional(id)
                .orElseThrow(() -> new RuntimeException("Unknown Astral Sorcery constellation '" + id + "'. Known constellations: "
                        + String.join(", ", RegistriesAS.REGISTRY_CONSTELLATIONS.keySet().stream().map(ResourceLocation::toString).sorted().toList())));
    }

    public static Lumen requireLumen(ResourceLocation id) {
        return RegistriesAS.REGISTRY_LUMEN.getOptional(id)
                .orElseThrow(() -> new RuntimeException("Unknown Astral Sorcery lumen '" + id + "'. Known lumen: "
                        + String.join(", ", RegistriesAS.REGISTRY_LUMEN.keySet().stream().map(ResourceLocation::toString).sorted().toList())));
    }
}
