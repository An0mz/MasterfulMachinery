package io.ticticboom.mods.mm.compat.kjs.event;

import dev.latvian.mods.kubejs.event.KubeEvent;
import lombok.Getter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BuilderStructureEventJS implements KubeEvent {
    @Getter
    private final List<String> removed = new ArrayList<>();
    @Getter
    private final List<String> removedNamespaces = new ArrayList<>();
    @Getter
    private final Map<String, String> added = new LinkedHashMap<>();

    public void remove(String id) {
        removed.add(id);
    }

    public void removeNamespace(String namespace) {
        removedNamespaces.add(namespace);
    }

    public void add(String id, String file) {
        added.put(id, file);
    }
}
