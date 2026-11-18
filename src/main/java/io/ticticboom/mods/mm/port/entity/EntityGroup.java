package io.ticticboom.mods.mm.port.entity;

import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.Tags;

import java.util.Locale;

public enum EntityGroup {
    PASSIVE,
    HOSTILE,
    BOSS;

    public static EntityGroup parse(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "passive" -> PASSIVE;
            case "hostile" -> HOSTILE;
            case "boss" -> BOSS;
            default -> throw new RuntimeException(String.format(
                    "Unknown entity group [%s], expected 'passive', 'hostile' or 'boss'", value));
        };
    }

    public boolean matches(EntityType<?> type) {
        if (type == null) {
            return false;
        }
        return switch (this) {
            case BOSS -> type.is(Tags.EntityTypes.BOSSES);
            case HOSTILE -> switch (type.getCategory()) {
                case MONSTER -> !type.is(Tags.EntityTypes.BOSSES);
                default -> false;
            };
            case PASSIVE -> switch (type.getCategory()) {
                case CREATURE, AMBIENT, WATER_CREATURE, WATER_AMBIENT, UNDERGROUND_WATER_CREATURE, AXOLOTLS -> true;
                default -> false;
            };
        };
    }

    public EntityType<?> representative() {
        return switch (this) {
            case PASSIVE -> EntityType.COW;
            case HOSTILE -> EntityType.ZOMBIE;
            case BOSS -> EntityType.WITHER;
        };
    }

    public String serialize() {
        return name().toLowerCase(Locale.ROOT);
    }
}
