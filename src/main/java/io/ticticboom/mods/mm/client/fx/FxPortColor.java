package io.ticticboom.mods.mm.client.fx;

import com.mojang.blaze3d.platform.NativeImage;
import io.ticticboom.mods.mm.Ref;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.io.InputStream;

public final class FxPortColor {
    private static final Object2IntOpenHashMap<ResourceLocation> AVERAGES = new Object2IntOpenHashMap<>();

    private FxPortColor() {
    }

    public static void clear() {
        AVERAGES.clear();
    }

    public static int tint(FluidStack fluid) {
        return fluid.isEmpty() ? 0xFFFFFF : IClientFluidTypeExtensions.of(fluid.getFluid()).getTintColor(fluid) & 0xFFFFFF;
    }

    public static int of(FluidStack fluid) {
        if (fluid.isEmpty()) {
            return -1;
        }
        var ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        int tint = ext.getTintColor(fluid);
        ResourceLocation still = ext.getStillTexture(fluid);
        int average = still == null ? 0xFFFFFF : AVERAGES.computeIfAbsent(still, FxPortColor::average);
        int r = ((average >> 16) & 0xFF) * ((tint >> 16) & 0xFF) / 255;
        int g = ((average >> 8) & 0xFF) * ((tint >> 8) & 0xFF) / 255;
        int b = (average & 0xFF) * (tint & 0xFF) / 255;
        return r << 16 | g << 8 | b;
    }

    public static int texture(ResourceLocation file) {
        return AVERAGES.computeIfAbsent(file, FxPortColor::averageTexture);
    }

    private static int averageTexture(ResourceLocation file) {
        return averageOf(file, -1);
    }

    private static int average(ResourceLocation sprite) {
        return averageOf(ResourceLocation.fromNamespaceAndPath(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png"), 0xFFFFFF);
    }

    private static int averageOf(ResourceLocation file, int fallback) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(file); NativeImage image = NativeImage.read(in)) {
            long r = 0, g = 0, b = 0, weight = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getPixelRGBA(x, y);
                    int a = pixel >>> 24;
                    r += (long) (pixel & 0xFF) * a;
                    g += (long) ((pixel >> 8) & 0xFF) * a;
                    b += (long) ((pixel >> 16) & 0xFF) * a;
                    weight += a;
                }
            }
            if (weight == 0) {
                return fallback;
            }
            return (int) (r / weight) << 16 | (int) (g / weight) << 8 | (int) (b / weight);
        } catch (Exception e) {
            Ref.LOG.warn("Effect: could not read texture {}: {}", file, e.getMessage());
            return fallback;
        }
    }
}
