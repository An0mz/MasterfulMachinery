package io.ticticboom.mods.mm.config;

import io.ticticboom.mods.mm.Ref;
import net.minecraftforge.common.ForgeConfigSpec;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.apache.commons.lang3.tuple.Pair;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, modid = Ref.ID)
public class MMConfigSetup {
    public static final MMCommonConfig COMMON;
    private static final ForgeConfigSpec commonSpec;

    static {
        final Pair<MMCommonConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(MMCommonConfig::new);
        COMMON = specPair.getKey();
        commonSpec = specPair.getRight();
    }

    public static void setup() {
        @SuppressWarnings("removal")
        var ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, commonSpec);
    }

    @SubscribeEvent
    public static void on(final ModConfigEvent event) {
        if (event.getConfig().getType() == ModConfig.Type.COMMON) {
            MMConfig.bake();
        }
    }
}
