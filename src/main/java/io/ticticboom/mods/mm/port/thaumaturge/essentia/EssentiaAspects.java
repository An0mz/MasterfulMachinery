package io.ticticboom.mods.mm.port.thaumaturge.essentia;

import com.leclowndu93150.thaumaturge.api.aspect.AspectComponents;
import com.leclowndu93150.thaumaturge.api.aspect.Aspects;
import com.leclowndu93150.thaumaturge.api.aspect.IAspect;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Nullable;

public final class EssentiaAspects {

    private static final int FALLBACK_COLOR = 0xFFAA88CC;

    private EssentiaAspects() {
    }

    public static ResourceKey<IAspect> key(ResourceLocation id) {
        return ResourceKey.create(IAspect.REGISTRY_KEY, id);
    }

    @Nullable
    public static ResourceKey<IAspect> key(@Nullable Holder<IAspect> holder) {
        return holder == null ? null : holder.unwrapKey().orElse(null);
    }

    @Nullable
    public static Holder<IAspect> holder(@Nullable HolderLookup.Provider registries, @Nullable ResourceKey<IAspect> key) {
        return registries == null || key == null ? null : Aspects.resolve(registries, key);
    }

    @Nullable
    public static Holder<IAspect> holder(@Nullable Level level, @Nullable ResourceKey<IAspect> key) {
        return level == null ? holder(key) : holder(level.registryAccess(), key);
    }

    @Nullable
    public static Holder<IAspect> holder(@Nullable ResourceKey<IAspect> key) {
        return holder(currentRegistries(), key);
    }

    public static Component name(@Nullable Level level, ResourceKey<IAspect> key) {
        var holder = holder(level, key);
        return holder == null ? fallbackName(key) : AspectComponents.name(holder);
    }

    public static Component name(ResourceKey<IAspect> key) {
        return name(null, key);
    }

    public static int color(@Nullable Level level, @Nullable ResourceKey<IAspect> key) {
        var holder = holder(level, key);
        return holder == null ? FALLBACK_COLOR : 0xFF000000 | holder.value().color();
    }

    private static Component fallbackName(ResourceKey<IAspect> key) {
        var id = key.location();
        return Component.translatable("aspect." + id.getNamespace() + "." + id.getPath());
    }

    @Nullable
    private static HolderLookup.Provider currentRegistries() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            var client = EssentiaClientRegistries.current();
            if (client != null) {
                return client;
            }
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.registryAccess();
    }
}
