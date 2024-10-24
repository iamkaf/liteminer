package com.iamkaf.liteminer.event;

import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.walker.Walker;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.utils.value.IntValue;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public class OnBlockBreak {
    public static void init() {
        BlockEvent.BREAK.register(OnBlockBreak::onBlockBreak);
    }

    private static EventResult onBlockBreak(Level level, BlockPos absoluteOrigin, BlockState blockState,
            ServerPlayer player, @Nullable IntValue intValue) {
        if (level.isClientSide) {
            return EventResult.pass();
        }

        if (Liteminer.instance.getPlayerState(player).getKeymappingState()) {
            ItemStack tool = player.getMainHandItem();

            // 1 durability left on the tool
            if (tool.isDamageableItem() && (tool.getMaxDamage() - tool.getDamageValue()) == 1) {
                return EventResult.pass();
            }

            Walker walker = new Walker();
            var blocks = walker.walk(level, player, absoluteOrigin)
                    .stream()
                    .sorted(Comparator.comparingInt(p -> p.distManhattan(absoluteOrigin)))
                    .toList();

            for (var block : blocks) {
                if (block.equals(absoluteOrigin)) {
                    continue;
                }
                if (player.isCreative()) {
                    level.setBlockAndUpdate(block, Blocks.AIR.defaultBlockState());
                    continue;
                }
                BlockState state = level.getBlockState(block);
                if (!tool.isEmpty() && tool.isDamageableItem()) {
                    boolean itemIsAboutToBreak = tool.getMaxDamage() - tool.getDamageValue() <= 2;
                    if (itemIsAboutToBreak) {
                        break;
                    } else {
                        if (state.getDestroySpeed(level, block) != 0.0f) {
                            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                        }
                    }
                }
                player.causeFoodExhaustion(0.2f);

                boolean skipDrops = state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state);

                if (!skipDrops) {
                    LootParams.Builder builder = new LootParams.Builder((ServerLevel) level).withParameter(
                                    LootContextParams.ORIGIN,
                                    Vec3.atCenterOf(block)
                            )
                            .withParameter(LootContextParams.TOOL, tool)
                            .withParameter(LootContextParams.BLOCK_STATE, state)
                            .withParameter(LootContextParams.THIS_ENTITY, player);

                    state.spawnAfterBreak((ServerLevel) level, absoluteOrigin, tool, true);

                    for (var stack : state.getDrops(builder)) {
                        var itemEntity = new ItemEntity(level,
                                absoluteOrigin.getX(),
                                absoluteOrigin.getY(),
                                absoluteOrigin.getZ(),
                                stack,
                                level.getRandom().nextFloat() / 10,
                                0.25f,
                                level.getRandom().nextFloat() / 10
                        );
                        level.addFreshEntity(itemEntity);
                    }
                }
                level.setBlockAndUpdate(block, Blocks.AIR.defaultBlockState());
            }
        }

        return EventResult.pass();
    }
}
