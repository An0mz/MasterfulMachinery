package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.structure.HelperSlotOverrides;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public record HelperSlotsSyncPkt(Map<ResourceLocation, HelperSlotOverrides.Rule> rules) implements CustomPacketPayload {
    public static final Type<HelperSlotsSyncPkt> TYPE = new Type<>(Ref.id("helper_slots_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HelperSlotsSyncPkt> STREAM_CODEC = StreamCodec.of((buf, pkt) -> encode(pkt, buf), HelperSlotsSyncPkt::decode);

    public static void encode(HelperSlotsSyncPkt packet, RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(packet.rules.size());
        packet.rules.forEach((id, rule) -> {
            buf.writeResourceLocation(id);
            buf.writeUtf(HelperSlotOverrides.keysString(rule.keys()), 256);
            buf.writeVarInt(rule.maxHelpers() + 1);
        });
    }

    public static HelperSlotsSyncPkt decode(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        if (count < 0 || count > HelperSlotOverrides.MAX_RULES) throw new IllegalArgumentException("Invalid helper rule count");
        Map<ResourceLocation, HelperSlotOverrides.Rule> rules = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buf.readResourceLocation();
            Set<Character> keys = new LinkedHashSet<>();
            for (char c : buf.readUtf(256).toCharArray()) keys.add(c);
            rules.put(id, new HelperSlotOverrides.Rule(keys, buf.readVarInt() - 1));
        }
        return new HelperSlotsSyncPkt(Map.copyOf(rules));
    }

    public static void handle(HelperSlotsSyncPkt packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> HelperSlotOverrides.receive(packet.rules));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
