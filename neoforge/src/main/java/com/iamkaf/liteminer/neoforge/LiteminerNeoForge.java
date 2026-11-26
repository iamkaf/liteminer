package com.iamkaf.liteminer.neoforge;

import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@Mod(Constants.MOD_ID)
public final class LiteminerNeoForge {
    public LiteminerNeoForge(IEventBus eventBus, ModContainer container) {
        LiteminerMod.init();
        NeoForge.EVENT_BUS.addListener(LiteminerNeoForge::onBreakSpeed);
    }

    // NeoForge mines through Player#getDestroySpeed(BlockState, BlockPos), which posts this event instead of
    // reaching the vanilla Player#getDestroySpeed(BlockState) that Fabric hooks.
    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        event.setNewSpeed(Liteminer.scaleBreakSpeed(event.getEntity(), event.getNewSpeed()));
    }
}
