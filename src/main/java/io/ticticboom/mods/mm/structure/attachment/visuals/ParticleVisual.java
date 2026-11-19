package io.ticticboom.mods.mm.structure.attachment.visuals;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.ControllerState;
import io.ticticboom.mods.mm.structure.layout.StructureLayout;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

@Getter
public class ParticleVisual {
    public enum When {
        WORKING, IDLE, FORMED;

        boolean matches(ControllerState state) {
            return switch (this) {
                case WORKING -> state == ControllerState.WORKING;
                case IDLE -> state == ControllerState.IDLE;
                case FORMED -> state != ControllerState.UNFORMED;
            };
        }
    }

    private final VisualRegion region;
    private final JsonElement particle;
    private final When when;
    private final float rate;
    private final double velocityX;
    private final double velocityY;
    private final double velocityZ;
    private final double[] offset;
    private final double[] spread;
    @Getter(lombok.AccessLevel.NONE)
    private @Nullable ParticleOptions resolved;
    @Getter(lombok.AccessLevel.NONE)
    private boolean failed;

    public ParticleVisual(VisualRegion region, JsonElement particle, When when, float rate, double velocityX, double velocityY, double velocityZ, double[] offset, double[] spread) {
        this.region = region;
        this.particle = particle;
        this.when = when;
        this.rate = Math.max(0f, Math.min(rate, 100f));
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;
        this.offset = offset;
        this.spread = spread;
    }

    public static ParticleVisual parse(JsonObject json) {
        if (!json.has("particle")) {
            throw new IllegalArgumentException("particles visual needs 'particle'");
        }
        When when = json.has("when") ? parseWhen(json.get("when").getAsString()) : When.WORKING;
        float rate = json.has("rate") ? json.get("rate").getAsFloat() : 1f;
        double[] velocity = vector(json, "velocity", 0);
        return new ParticleVisual(VisualRegion.parse(json), json.get("particle"), when, rate, velocity[0], velocity[1], velocity[2],
                vector(json, "offset", 0), vector(json, "spread", 1));
    }

    private static double[] vector(JsonObject json, String key, double fallback) {
        double[] values = {fallback, fallback, fallback};
        if (json.has(key)) {
            JsonElement el = json.get(key);
            if (!el.isJsonArray() || el.getAsJsonArray().size() != 3) {
                throw new IllegalArgumentException("particles '" + key + "' must be [x, y, z]");
            }
            JsonArray a = el.getAsJsonArray();
            for (int i = 0; i < 3; i++) {
                values[i] = a.get(i).getAsDouble();
            }
        }
        return values;
    }

    public static When parseWhen(String value) {
        try {
            return When.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("particles 'when' must be working, idle or formed, got: " + value);
        }
    }

    @Nullable
    public ResourceLocation particleTypeId() {
        String type = particle.isJsonPrimitive() ? particle.getAsString()
                : particle.isJsonObject() && particle.getAsJsonObject().has("type") ? particle.getAsJsonObject().get("type").getAsString() : null;
        return type == null ? null : ResourceLocation.tryParse(type);
    }

    public void validate(ResourceLocation structureId, StructureLayout layout) {
        region.validate(structureId, layout, "particles", "particles");
        var type = particleTypeId();
        if (type == null || !BuiltInRegistries.PARTICLE_TYPE.containsKey(type)) {
            Ref.LOG.warn("Structure {}: particles visual has an unknown particle {}", structureId, particle);
        }
    }

    public void spawn(Level level, BlockPos controllerPos, StructureLayout layout, Rotation rotation, ControllerState state) {
        if (failed || rate <= 0 || !when.matches(state)) {
            return;
        }
        var options = resolve(level);
        if (options == null) {
            return;
        }
        long[] cells = region.relativeCells(layout, rotation);
        if (cells.length == 0) {
            return;
        }
        RandomSource random = level.getRandom();
        int count = (int) rate;
        if (random.nextFloat() < rate - count) {
            count++;
        }
        double vx = rotateX(velocityX, velocityZ, rotation, 0);
        double vz = rotateZ(velocityX, velocityZ, rotation, 0);
        for (int i = 0; i < count; i++) {
            long cell = cells[random.nextInt(cells.length)];
            double fx = offset[0] + random.nextDouble() * spread[0];
            double fy = offset[1] + random.nextDouble() * spread[1];
            double fz = offset[2] + random.nextDouble() * spread[2];
            double x = controllerPos.getX() + BlockPos.getX(cell) + rotateX(fx, fz, rotation, 1);
            double y = controllerPos.getY() + BlockPos.getY(cell) + fy;
            double z = controllerPos.getZ() + BlockPos.getZ(cell) + rotateZ(fx, fz, rotation, 1);
            level.addParticle(options, x, y, z, vx, velocityY, vz);
        }
    }

    private static double rotateX(double x, double z, Rotation rotation, double size) {
        return switch (rotation) {
            case NONE -> x;
            case CLOCKWISE_90 -> size - z;
            case CLOCKWISE_180 -> size - x;
            case COUNTERCLOCKWISE_90 -> z;
        };
    }

    private static double rotateZ(double x, double z, Rotation rotation, double size) {
        return switch (rotation) {
            case NONE -> z;
            case CLOCKWISE_90 -> x;
            case CLOCKWISE_180 -> size - z;
            case COUNTERCLOCKWISE_90 -> size - x;
        };
    }

    @Nullable
    private ParticleOptions resolve(Level level) {
        if (resolved != null) {
            return resolved;
        }
        JsonElement input = particle;
        if (particle.isJsonPrimitive()) {
            var obj = new JsonObject();
            obj.add("type", particle);
            input = obj;
        }
        var result = ParticleTypes.CODEC.parse(RegistryOps.create(JsonOps.INSTANCE, level.registryAccess()), input);
        resolved = result.result().orElse(null);
        if (resolved == null) {
            failed = true;
            Ref.LOG.warn("Not showing particles {}: {}", particle, result.error().map(e -> e.message()).orElse("unknown particle"));
        }
        return resolved;
    }

    public JsonObject toJson() {
        var json = new JsonObject();
        json.addProperty("type", "particles");
        region.writeJson(json);
        json.add("particle", particle);
        if (when != When.WORKING) {
            json.addProperty("when", when.name().toLowerCase(Locale.ROOT));
        }
        if (rate != 1f) {
            json.addProperty("rate", rate);
        }
        if (velocityX != 0 || velocityY != 0 || velocityZ != 0) {
            json.add("velocity", vectorJson(velocityX, velocityY, velocityZ));
        }
        if (offset[0] != 0 || offset[1] != 0 || offset[2] != 0) {
            json.add("offset", vectorJson(offset[0], offset[1], offset[2]));
        }
        if (spread[0] != 1 || spread[1] != 1 || spread[2] != 1) {
            json.add("spread", vectorJson(spread[0], spread[1], spread[2]));
        }
        return json;
    }

    private static JsonArray vectorJson(double x, double y, double z) {
        var array = new JsonArray();
        array.add(x);
        array.add(y);
        array.add(z);
        return array;
    }
}
