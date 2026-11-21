package io.ticticboom.mods.mm.client.fx;

import org.jetbrains.annotations.Nullable;

public interface FxLayer {
    @Nullable
    State create(FxShared shared);

    interface State {
        default void scan(FxContext ctx) {
        }

        default void reset() {
        }

        default double reach() {
            return 0;
        }

        @Nullable
        default String problem(double intensity) {
            return null;
        }

        default void update(FxContext ctx) {
        }

        void render(FxContext ctx);
    }
}
