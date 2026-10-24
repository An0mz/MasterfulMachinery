package io.ticticboom.mods.mm.port.astral.lumen.feature;

import hellfirepvp.astralsorcery.common.lib.RegistriesAS;
import hellfirepvp.astralsorcery.common.lumen.ILumenHandler;
import hellfirepvp.astralsorcery.common.lumen.Lumen;
import hellfirepvp.astralsorcery.common.lumen.LumenStack;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenNetworkHelper;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenNode;
import hellfirepvp.astralsorcery.common.lumen.transfer.LumenRequestHelper;
import hellfirepvp.astralsorcery.common.util.LumenUtil;
import io.ticticboom.mods.mm.port.astral.lumen.AstralLumenPortStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.HashSet;
import java.util.Set;

public class AstralLumenNetworkFeature {

    private static final int TRANSFER_INTERVAL = 20;
    private static final int MAX_REQUEST = 500;

    private final AstralLumenPortStorage storage;
    private final boolean input;
    private boolean attached = false;

    public AstralLumenNetworkFeature(AstralLumenPortStorage storage, boolean input) {
        this.storage = storage;
        this.input = input;
    }

    public void tick(ServerLevel level, BlockPos pos) {
        if (!input && !attached) {
            attached = true;
            if (LumenNetworkHelper.getNode(level, pos).isEmpty()) {
                LumenNetworkHelper.createNode(level, pos, pos.getCenter(), LumenNode.ConnectionType.SOURCE);
            }
        }
        if (level.getGameTime() % TRANSFER_INTERVAL != 0) {
            return;
        }
        if (input) {
            pull(level, pos);
        } else {
            advertise(level, pos);
        }
    }

    private void pull(ServerLevel level, BlockPos pos) {
        for (var id : storage.wantedLumen(level.getGameTime())) {
            int room = storage.getCapacity() - storage.getTotal();
            if (room <= 0) {
                return;
            }
            Lumen lumen = RegistriesAS.REGISTRY_LUMEN.getOptional(id).orElse(null);
            if (lumen == null) {
                continue;
            }
            LumenStack request = LumenStack.of(lumen, Math.min(room, MAX_REQUEST));
            LumenRequestHelper.requestRelayed(level, pos, request).ifPresent(chain -> {
                LumenStack moved = LumenUtil.tryChainTransfer(storage.getHandler(), level, chain, request, ILumenHandler.Action.EXECUTE);
                if (!moved.isEmpty()) {
                    chain.playTransferEffect(level, moved.getLumen());
                }
            });
        }
    }

    private void advertise(ServerLevel level, BlockPos pos) {
        LumenNetworkHelper.getNode(level, pos).ifPresent(node -> {
            Set<Lumen> provided = new HashSet<>();
            storage.getStoredByLumen().keySet().forEach(id ->
                    RegistriesAS.REGISTRY_LUMEN.getOptional(id).ifPresent(provided::add));
            if (!provided.equals(node.getProvidedLumenTypes())) {
                LumenNetworkHelper.setNodeProvidedLumenTypes(level, node, provided);
            }
        });
    }

    public static void detach(ServerLevel level, BlockPos pos) {
        LumenNetworkHelper.getNode(level, pos).ifPresent(node -> LumenNetworkHelper.removeNode(level, node));
    }
}
