package io.ticticboom.mods.mm.compat.waila;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.port.IPortBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

public class PortContentsDataProvider implements IServerDataProvider<BlockAccessor>, IBlockComponentProvider {
    public static final ResourceLocation UID = Ref.id("port_contents");
    public static final String KEY = "PortContents";

    public static final PortContentsDataProvider INSTANCE = new PortContentsDataProvider();

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof IPortBlockEntity port) || port.getStorage() == null) {
            return;
        }
        var lines = port.getStorage().describeContents();
        if (lines.isEmpty()) {
            return;
        }
        var registries = accessor.getLevel().registryAccess();
        var list = new ListTag();
        for (var line : lines) {
            list.add(StringTag.valueOf(Component.Serializer.toJson(line, registries)));
        }
        data.put(KEY, list);
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (!data.contains(KEY)) {
            return;
        }
        var registries = accessor.getLevel().registryAccess();
        var list = data.getList(KEY, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            var line = Component.Serializer.fromJson(list.getString(i), registries);
            if (line != null) {
                tooltip.add(line);
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
