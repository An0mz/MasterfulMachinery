package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.platform.InputConstants;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.config.MMClientConfig;
import io.ticticboom.mods.mm.config.MMConfigSetup;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class FxKeys {
    public static final KeyMapping QUALITY = new KeyMapping("key.mm.effect_quality", KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "key.categories.mm");

    private FxKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(QUALITY);
    }

    @EventBusSubscriber(modid = Ref.ID, value = Dist.CLIENT)
    public static final class Presses {
        private Presses() {
        }

        @SubscribeEvent
        public static void onTick(ClientTickEvent.Post event) {
            while (QUALITY.consumeClick()) {
                var value = MMConfigSetup.CLIENT.effectQuality;
                MMClientConfig.EffectQuality[] all = MMClientConfig.EffectQuality.values();
                MMClientConfig.EffectQuality next = all[(value.get().ordinal() + 1) % all.length];
                value.set(next);
                value.save();
                FxSettings.refresh();
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    player.displayClientMessage(Component.translatable("message.mm.effect_quality",
                            Component.translatable("config.mm.quality." + next.name().toLowerCase(java.util.Locale.ROOT))), true);
                }
            }
        }
    }
}
