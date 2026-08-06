package io.ticticboom.mods.mm.compat.kjs;

import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventGroupRegistry;
import dev.latvian.mods.kubejs.event.EventHandler;
import io.ticticboom.mods.mm.compat.kjs.event.*;

public interface MMKubeEvents {
    EventGroup GROUP = EventGroup.of("MMEvents");

    EventHandler CONTROLLERS = GROUP.startup("registerControllers", () -> ControllerEventJS.class);
    EventHandler PORTS = GROUP.startup("registerPorts", () -> PortEventJS.class);
    EventHandler EXTRA = GROUP.startup("registerExtraBlocks", () -> ExtraBlockEventJS.class);
    EventHandler STRUCTURES = GROUP.server("createStructures", () -> StructureEventJS.class);
    EventHandler RECIPES = GROUP.server("createProcesses", () -> RecipeEventJS.class);

    // KubeJS 2101 registers groups through a registry handed to the plugin rather than
    // letting the group register itself.
    static void register(EventGroupRegistry registry) {
        registry.register(GROUP);
    }
}
