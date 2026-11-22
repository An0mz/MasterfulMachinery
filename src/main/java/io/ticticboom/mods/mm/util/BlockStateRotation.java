package io.ticticboom.mods.mm.util;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class BlockStateRotation {

    @SuppressWarnings("unchecked")
    private static final ConcurrentHashMap<BlockState, BlockState>[] CACHE = new ConcurrentHashMap[]{
            new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>(), new ConcurrentHashMap<>()
    };

    private BlockStateRotation() {
    }

    public static BlockState rotate(BlockState state, Rotation rotation) {
        if (rotation == Rotation.NONE) {
            return state;
        }
        ConcurrentHashMap<BlockState, BlockState> cache = CACHE[rotation.ordinal()];
        BlockState cached = cache.get(state);
        if (cached == null) {
            cached = compute(state, rotation);
            cache.put(state, cached);
        }
        return cached;
    }

    private static BlockState compute(BlockState state, Rotation rotation) {
        BlockState rotated = state.rotate(rotation);
        for (Property<?> prop : rotated.getProperties()) {
            if (!state.getValue(prop).equals(rotated.getValue(prop))) {
                continue;
            }
            if (prop instanceof DirectionProperty dir) {
                rotated = rotateFacing(rotated, dir, rotation);
            } else if (prop instanceof EnumProperty<?>) {
                rotated = rotateNamed(rotated, prop, rotation);
            }
        }
        return rotated;
    }

    public static BlockState unrotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> rotate(state, Rotation.COUNTERCLOCKWISE_90);
            case COUNTERCLOCKWISE_90 -> rotate(state, Rotation.CLOCKWISE_90);
            case CLOCKWISE_180 -> rotate(state, Rotation.CLOCKWISE_180);
            default -> state;
        };
    }

    private static BlockState rotateFacing(BlockState state, DirectionProperty prop, Rotation rotation) {
        Direction current = state.getValue(prop);
        if (current.getAxis().isVertical()) {
            return state;
        }
        Direction turned = rotation.rotate(current);
        if (prop.getPossibleValues().contains(turned)) {
            return state.setValue(prop, turned);
        }
        if (prop.getPossibleValues().contains(turned.getOpposite())) {
            return state.setValue(prop, turned.getOpposite());
        }
        return state;
    }

    private static <T extends Comparable<T>> BlockState rotateNamed(BlockState state, Property<T> prop, Rotation rotation) {
        String name = prop.getName(state.getValue(prop));
        String[] tokens = name.split("_");
        boolean changed = false;
        for (int i = 0; i < tokens.length; i++) {
            String turned = rotateToken(tokens[i], rotation);
            if (turned != null && !turned.equals(tokens[i])) {
                tokens[i] = turned;
                changed = true;
            }
        }
        if (!changed) {
            return state;
        }
        Optional<T> value = prop.getValue(String.join("_", tokens));
        return value.isPresent() ? state.setValue(prop, value.get()) : state;
    }

    private static String rotateToken(String token, Rotation rotation) {
        if (token.isEmpty() || token.length() > 2) {
            return null;
        }
        char ns = 0;
        char ew = 0;
        for (int i = 0; i < token.length(); i++) {
            char c = rotateLetter(token.charAt(i), rotation);
            if (c == 'n' || c == 's') {
                if (ns != 0) ns = 'x';
                else ns = c;
            } else if (c == 'e' || c == 'w') {
                if (ew != 0) ew = 'x';
                else ew = c;
            } else {
                return null;
            }
        }
        if (ns == 'x') return "ns";
        if (ew == 'x') return "ew";
        StringBuilder out = new StringBuilder(2);
        if (ns != 0) out.append(ns);
        if (ew != 0) out.append(ew);
        return out.toString();
    }

    private static char rotateLetter(char c, Rotation rotation) {
        Direction dir = switch (c) {
            case 'n' -> Direction.NORTH;
            case 'e' -> Direction.EAST;
            case 's' -> Direction.SOUTH;
            case 'w' -> Direction.WEST;
            default -> null;
        };
        if (dir == null) {
            return 0;
        }
        return switch (rotation.rotate(dir)) {
            case NORTH -> 'n';
            case EAST -> 'e';
            case SOUTH -> 's';
            default -> 'w';
        };
    }
}
