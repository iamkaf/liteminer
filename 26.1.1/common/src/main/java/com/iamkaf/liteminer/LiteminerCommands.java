package com.iamkaf.liteminer;

import com.iamkaf.amber.api.event.v1.events.common.CommandEvents;
import com.iamkaf.liteminer.networking.LiteminerNetwork;
import com.iamkaf.liteminer.networking.S2CSetShape;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LiteminerCommands {
    private LiteminerCommands() {
    }

    public static void init() {
        CommandEvents.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("liteminer")
                        .then(Commands.literal("shape")
                                .then(Commands.literal("set")
                                        .then(Commands.argument("index", IntegerArgumentType.integer(0, Liteminer.WALKERS.size() - 1))
                                                .executes(context -> setShape(
                                                        context.getSource(),
                                                        IntegerArgumentType.getInteger(context, "index")
                                                )))))));
    }

    private static int setShape(CommandSourceStack source, int index) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        LiteminerPlayerState state = Liteminer.instance.getPlayerState(player);
        Liteminer.instance.onKeymappingStateChange(player, state.getKeymappingState(), index);
        LiteminerNetwork.sendToPlayer(new S2CSetShape(index), player);
        source.sendSuccess(() -> Component.literal("Set Liteminer shape to " + index), false);
        return index;
    }
}
