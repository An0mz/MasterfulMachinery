package io.ticticboom.mods.mm.port.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;

public class EntityTypes {

    public static ResourceLocation idOf(EntityType<?> type) {
        return type == null ? null : BuiltInRegistries.ENTITY_TYPE.getKey(type);
    }

    public static EntityType<?> get(ResourceLocation id) {
        return id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
    }

    public static boolean inTagById(ResourceLocation typeId, ResourceLocation tagId) {
        var type = get(typeId);
        return type != null && inTag(type, tagId);
    }

    public static boolean inTag(EntityType<?> type, ResourceLocation tagId) {
        if (type == null || tagId == null) {
            return false;
        }
        var tag = BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, tagId));
        return tag.isPresent() && tag.get().stream().anyMatch(holder -> holder.value() == type);
    }

    public static EntityType<?> firstInTag(ResourceLocation tagId) {
        if (tagId == null) {
            return null;
        }
        var tag = BuiltInRegistries.ENTITY_TYPE.getTag(TagKey.create(Registries.ENTITY_TYPE, tagId));
        return tag.map(holders -> holders.stream().findFirst().map(holder -> (EntityType<?>) holder.value()).orElse(null)).orElse(null);
    }

    public static Component displayName(ResourceLocation id) {
        var type = get(id);
        if (type != null) {
            return type.getDescription().copy();
        }
        return Component.literal(id == null ? "?" : id.toString());
    }
}
