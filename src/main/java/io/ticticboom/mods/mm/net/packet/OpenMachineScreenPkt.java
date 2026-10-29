package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerMenu;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineBlockEntity;
import io.ticticboom.mods.mm.controller.single.register.SingleMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

public record OpenMachineScreenPkt(BlockPos pos, boolean controller) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<OpenMachineScreenPkt> TYPE = new CustomPacketPayload.Type<>(Ref.id("open_machine_screen"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMachineScreenPkt> STREAM_CODEC = StreamCodec.of(
            (buf, pkt) -> {
                buf.writeBlockPos(pkt.pos());
                buf.writeBoolean(pkt.controller());
            },
            buf -> new OpenMachineScreenPkt(buf.readBlockPos(), buf.readBoolean()));

    public static void handle(OpenMachineScreenPkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.containerMenu.stillValid(player)) {
                return;
            }
            SingleMachineBlockEntity machine = openMachine(player);
            if (machine == null || !machine.getBlockPos().equals(pkt.pos())) {
                return;
            }
            if (pkt.controller()) {
                machine.openController(player);
            } else {
                machine.openSlots(player);
            }
        });
    }

    @Nullable
    private static SingleMachineBlockEntity openMachine(ServerPlayer player) {
        if (player.containerMenu instanceof SingleMachineMenu menu) {
            return menu.getMachine();
        }
        if (player.containerMenu instanceof MachineControllerMenu menu && menu.getBe().getBlockEntity() instanceof SingleMachineBlockEntity machine) {
            return machine;
        }
        return null;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
