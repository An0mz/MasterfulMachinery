package io.ticticboom.mods.mm.client.fx;

import io.ticticboom.mods.mm.Ref;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class FxLog {
    private static final Set<String> SEEN = ConcurrentHashMap.newKeySet();

    private FxLog() {
    }

    public static void warnOnce(String message, Object... args) {
        if (SEEN.size() < 1000 && SEEN.add(message + Arrays.toString(args))) {
            Ref.LOG.warn(message, args);
        }
    }

    static void reset() {
        SEEN.clear();
    }
}
