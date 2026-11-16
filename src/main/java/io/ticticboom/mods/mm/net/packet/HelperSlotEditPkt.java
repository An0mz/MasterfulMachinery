package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.structure.HelperSlotOverrides;
import io.ticticboom.mods.mm.tool.MultiblockToolMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record HelperSlotEditPkt(HelperSlotOverrides.Action action, ResourceLocation structure, char key, int value) implements CustomPacketPayload {
    public static final Type<HelperSlotEditPkt> TYPE = new Type<>(Ref.id("helper_slot_edit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HelperSlotEditPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), HelperSlotEditPkt::decode);

    public static void encode(HelperSlotEditPkt packet, RegistryFriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeResourceLocation(packet.structure);
        buf.writeChar(packet.key);
        buf.writeInt(packet.value);
    }

    public static HelperSlotEditPkt decode(RegistryFriendlyByteBuf buf) {
        return new HelperSlotEditPkt(buf.readEnum(HelperSlotOverrides.Action.class), buf.readResourceLocation(), buf.readChar(), buf.readInt());
    }

    public static void handle(HelperSlotEditPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.player() instanceof ServerPlayer sp ? sp : null;
            if (sender == null || !sender.hasPermissions(2) || !(sender.containerMenu instanceof MultiblockToolMenu)) return;
            HelperSlotOverrides overrides = HelperSlotOverrides.get(sender.getServer());
            if (overrides.apply(packet.action, packet.structure, packet.key, packet.value)) {
                PacketDistributor.sendToAllPlayers(new HelperSlotsSyncPkt(overrides.snapshot()));
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
