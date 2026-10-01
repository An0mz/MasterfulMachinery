package io.ticticboom.mods.mm.tool;

import io.ticticboom.mods.mm.builder.TierPrefs;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class ToolData {
    private static final String STRUCTURE_KEY = "Structure";
    private static final String BUILDER_KEY = "BuilderStructure";
    private static final String TURNS_KEY = "ExtraTurns";
    private static final String TIERS_KEY = "Tiers";
    private static final String NETWORK_KEY = "Network";
    private static final String USE_ME_KEY = "UseMe";
    private static final String AUTOCRAFT_KEY = "AutoCraft";
    private static final String INSTANT_BUILD_KEY = "InstantBuild";

    private ToolData() {
    }

    private static CompoundTag tag(ItemStack stack) {
        return ToolComponents.read(stack, ToolComponents.SETTINGS);
    }

    public static @Nullable ResourceLocation structure(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(STRUCTURE_KEY, Tag.TAG_STRING) || tag.getBoolean(BUILDER_KEY)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(STRUCTURE_KEY));
    }

    public static @Nullable ResourceLocation builderStructure(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(STRUCTURE_KEY, Tag.TAG_STRING) || !tag.getBoolean(BUILDER_KEY)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(STRUCTURE_KEY));
    }

    public static void setStructure(ItemStack stack, @Nullable ResourceLocation id) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> {
            if (id == null) {
                tag.remove(STRUCTURE_KEY);
            } else {
                tag.putString(STRUCTURE_KEY, id.toString());
            }
            tag.remove(BUILDER_KEY);
        });
    }

    public static void setBuilderStructure(ItemStack stack, ResourceLocation id) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> {
            tag.putString(STRUCTURE_KEY, id.toString());
            tag.putBoolean(BUILDER_KEY, true);
        });
    }

    public static int extraTurns(ItemStack stack) {
        return Math.floorMod(tag(stack).getInt(TURNS_KEY), 4);
    }

    public static void setExtraTurns(ItemStack stack, int turns) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putInt(TURNS_KEY, Math.floorMod(turns, 4)));
    }

    public static TierPrefs tiers(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(TIERS_KEY, Tag.TAG_COMPOUND)) {
            return new TierPrefs();
        }
        return TierPrefs.load(tag.getCompound(TIERS_KEY));
    }

    public static void setTiers(ItemStack stack, TierPrefs prefs) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.put(TIERS_KEY, prefs.save()));
    }

    @Nullable
    public static LinkData.NetworkPos network(ItemStack stack) {
        CompoundTag tag = tag(stack);
        if (!tag.contains(NETWORK_KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        return LinkData.NetworkPos.load(tag.getCompound(NETWORK_KEY));
    }

    public static void setNetwork(ItemStack stack, @Nullable LinkData.NetworkPos network) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> {
            if (network == null) {
                tag.remove(NETWORK_KEY);
            } else {
                tag.put(NETWORK_KEY, network.save());
            }
        });
    }

    public static boolean useMe(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return !tag.contains(USE_ME_KEY, Tag.TAG_BYTE) || tag.getBoolean(USE_ME_KEY);
    }

    public static void setUseMe(ItemStack stack, boolean value) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putBoolean(USE_ME_KEY, value));
    }

    public static boolean autoCraft(ItemStack stack) {
        CompoundTag tag = tag(stack);
        return !tag.contains(AUTOCRAFT_KEY, Tag.TAG_BYTE) || tag.getBoolean(AUTOCRAFT_KEY);
    }

    public static void setAutoCraft(ItemStack stack, boolean value) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putBoolean(AUTOCRAFT_KEY, value));
    }

    public static boolean instantBuild(ItemStack stack) {
        return tag(stack).getBoolean(INSTANT_BUILD_KEY);
    }

    public static void setInstantBuild(ItemStack stack, boolean value) {
        ToolComponents.update(stack, ToolComponents.SETTINGS, tag -> tag.putBoolean(INSTANT_BUILD_KEY, value));
    }
}
