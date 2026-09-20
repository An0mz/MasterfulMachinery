package io.ticticboom.mods.mm.event;

import io.ticticboom.mods.mm.structure.StructureProblems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class StructureProblemNotifier {

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (StructureProblems.isEmpty() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasPermissions(2)) {
            return;
        }
        player.sendSystemMessage(Component.translatable("message.mm.structure_problems",
                StructureProblems.structureCount()).withStyle(ChatFormatting.RED));
    }
}
