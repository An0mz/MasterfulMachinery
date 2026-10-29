package io.ticticboom.mods.mm.net.packet;

import io.ticticboom.mods.mm.Ref;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerBlockEntity;
import io.ticticboom.mods.mm.controller.machine.register.MachineControllerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class SelectRecipePkt implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectRecipePkt> TYPE =
            new CustomPacketPayload.Type<>(Ref.id("select_recipe"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SelectRecipePkt> STREAM_CODEC =
            StreamCodec.of(SelectRecipePkt::encode, SelectRecipePkt::decode);

    private static final int MAX_ID_LENGTH = 256;

    private final BlockPos pos;
    private final String recipeId;

    public SelectRecipePkt(BlockPos pos, String recipeId) {
        this.pos = pos;
        this.recipeId = recipeId;
    }

    public static void encode(RegistryFriendlyByteBuf buf, SelectRecipePkt pkt) {
        buf.writeBlockPos(pkt.pos);
        buf.writeUtf(pkt.recipeId, MAX_ID_LENGTH);
    }

    public static SelectRecipePkt decode(RegistryFriendlyByteBuf buf) {
        return new SelectRecipePkt(buf.readBlockPos(), buf.readUtf(MAX_ID_LENGTH));
    }

    public static void handle(SelectRecipePkt pkt, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer sender)) {
                return;
            }
            if (!(sender.containerMenu instanceof MachineControllerMenu menu) || !menu.stillValid(sender)) {
                return;
            }
            if (menu.getBe().getBlockEntity() instanceof MachineControllerBlockEntity controller && controller.getBlockPos().equals(pkt.pos)) {
                controller.selectRecipe(pkt.recipeId.isEmpty() ? null : ResourceLocation.tryParse(pkt.recipeId));
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
