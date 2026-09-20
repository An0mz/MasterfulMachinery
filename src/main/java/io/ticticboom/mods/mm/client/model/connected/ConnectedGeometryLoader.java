package io.ticticboom.mods.mm.client.model.connected;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;

public class ConnectedGeometryLoader implements IGeometryLoader<ConnectedGeometry> {

    public static final ConnectedGeometryLoader INSTANCE = new ConnectedGeometryLoader();

    @Override
    public ConnectedGeometry read(JsonObject json, JsonDeserializationContext context) {
        return new ConnectedGeometry();
    }
}
