package io.ticticboom.mods.mm.compat.ae2.linker;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.storage.MEStorage;
import io.ticticboom.mods.mm.networklink.LinkData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public final class NetworkAccess {

    private NetworkAccess() {
    }

    @Nullable
    public static MEStorage storage(MinecraftServer server, LinkData.NetworkPos network) {
        IGrid grid = grid(server, network);
        return grid == null ? null : grid.getStorageService().getInventory();
    }

    @Nullable
    public static IGrid grid(MinecraftServer server, LinkData.NetworkPos network) {
        ServerLevel level = server.getLevel(network.dimension());
        if (level == null || !level.isLoaded(network.pos())) {
            return null;
        }
        if (!(level.getBlockEntity(network.pos()) instanceof IInWorldGridNodeHost host)) {
            return null;
        }
        IGridNode node = nodeOf(host, network.face());
        if (node == null || !node.isActive()) {
            return null;
        }
        return node.getGrid();
    }

    @Nullable
    public static LinkData.NetworkPos clicked(Level level, BlockPos pos, Direction face, IInWorldGridNodeHost host) {
        return nodeOf(host, face) == null ? null : new LinkData.NetworkPos(level.dimension(), pos, face);
    }

    @Nullable
    public static IGridNode nodeOf(IInWorldGridNodeHost host, @Nullable Direction face) {
        IGridNode node = host.getGridNode(face);
        if (node != null) {
            return node;
        }
        for (Direction dir : Direction.values()) {
            node = host.getGridNode(dir);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
