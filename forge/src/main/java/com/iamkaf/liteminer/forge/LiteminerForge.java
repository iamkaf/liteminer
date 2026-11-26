package com.iamkaf.liteminer.forge;

import com.iamkaf.konfig.forge.api.v1.KonfigForgeClientScreens;
import com.iamkaf.liteminer.Constants;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerClient;
import com.iamkaf.liteminer.LiteminerMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Constants.MOD_ID)
public class LiteminerForge {

    public LiteminerForge() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            KonfigForgeClientScreens.register(Constants.MOD_ID);
            LiteminerClient.init();
        }
        LiteminerMod.init();
        //? if >=1.21.6 {
        PlayerEvent.BreakSpeed.BUS.addListener(LiteminerForge::onBreakSpeed);
        //?} else {
        /*net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(LiteminerForge::onBreakSpeed);
        *///?}
    }

    // Forge mines through its own position-aware break speed method, which posts this event instead of
    // reaching the vanilla Player#getDestroySpeed(BlockState) that Fabric hooks.
    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        //? if >=1.19 {
        var player = event.getEntity();
        //?} else {
        /*var player = event.getPlayer();
        *///?}
        event.setNewSpeed(Liteminer.scaleBreakSpeed(player, event.getNewSpeed()));
    }
}
