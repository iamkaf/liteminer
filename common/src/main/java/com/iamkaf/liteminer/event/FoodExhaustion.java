package com.iamkaf.liteminer.event;

import com.iamkaf.liteminer.Liteminer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class FoodExhaustion {
    private static final Identifier FARMERS_DELIGHT_NOURISHMENT =
            Identifier.fromNamespaceAndPath("farmersdelight", "nourishment");

    private FoodExhaustion() {
    }

    /** Shared by the server gate and the client HUD, which reads its local copy of the config. */
    public static boolean isTooHungry(Player player) {
        return !player.isCreative()
                && isHungerRequired()
                && player.getFoodData().getFoodLevel() <= 0;
    }

    static void apply(Player player) {
        if (!isEnabled() || hasFarmersDelightNourishment(player)) {
            return;
        }

        player.causeFoodExhaustion(getExhaustion());
    }

    private static boolean isHungerRequired() {
        return !Liteminer.CONFIG.allowVeinMiningAtZeroHunger.get() && isEnabled();
    }

    private static boolean isEnabled() {
        return Liteminer.CONFIG.foodExhaustionEnabled.get() && getExhaustion() > 0;
    }

    private static float getExhaustion() {
        return Liteminer.CONFIG.foodExhaustion.get().floatValue();
    }

    private static boolean hasFarmersDelightNourishment(Player player) {
        return BuiltInRegistries.MOB_EFFECT.get(FARMERS_DELIGHT_NOURISHMENT)
                .map(player::hasEffect)
                .orElse(false);
    }
}
