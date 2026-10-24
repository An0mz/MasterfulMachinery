package io.ticticboom.mods.mm.port.astral.starlight.feature;

import hellfirepvp.astralsorcery.common.lib.DataAS;
import hellfirepvp.astralsorcery.common.starlight.StarlightNetworkLevelHelper;
import hellfirepvp.astralsorcery.common.starlight.api.TransmissionNode;
import hellfirepvp.astralsorcery.common.starlight.transmission.StarlightTransmissionLevelHelper;
import hellfirepvp.astralsorcery.common.tile.network.ForwardingStarlightReceiverNode;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public class AstralStarlightReceiverFeature {

    private boolean attached = false;

    public void tick(ServerLevel level, BlockPos pos) {
        if (attached) {
            return;
        }
        attached = true;
        var existing = StarlightNetworkLevelHelper.get(level).getNode(pos).orElse(null);
        if (existing instanceof ForwardingStarlightReceiverNode) {
            return;
        }
        var data = DataAS.DOMAIN_AS.getData(level, DataAS.KEY_STARLIGHT_NETWORK_DATA);
        if (existing != null) {
            data.removeTransmissionNode(pos);
        }
        var node = new ForwardingStarlightReceiverNode(pos);
        data.addTransmissionNode(node);
        notifyChange(level, node);
    }

    public static void detach(ServerLevel level, BlockPos pos) {
        StarlightNetworkLevelHelper.get(level).getNode(pos).ifPresent(node -> {
            DataAS.DOMAIN_AS.getData(level, DataAS.KEY_STARLIGHT_NETWORK_DATA).removeTransmissionNode(pos);
            notifyChange(level, node);
        });
    }

    private static void notifyChange(ServerLevel level, TransmissionNode node) {
        StarlightTransmissionLevelHelper.getInstance().getHandler(level)
                .ifPresent(handler -> handler.notifyNodeChange(node));
    }
}
