package com.iamkaf.liteminer.event;

import com.iamkaf.amber.api.platform.v1.Platform;
import com.iamkaf.amber.api.event.v1.events.common.BlockEvents;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerPlayerState;
import com.iamkaf.liteminer.platform.Services;
import com.iamkaf.liteminer.shapes.Walker;
import com.iamkaf.liteminer.tags.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;

import static com.iamkaf.liteminer.Liteminer.WALKERS;

public class OnBlockBreak {
    public static void init() {
        BlockEvents.BLOCK_BREAK_BEFORE.register(OnBlockBreak::onBlockBreak);
    }

    private static InteractionResult onBlockBreak(Level level, Player player, BlockPos absoluteOrigin, BlockState blockState,
            @Nullable BlockEntity blockEntity) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }

        LiteminerPlayerState playerState = Liteminer.instance.getPlayerState((ServerPlayer) player);
        if (!playerState.getKeymappingState()) {
            return InteractionResult.PASS;
        }

        ItemStack tool = player.getMainHandItem();

        if (TagHelper.isExcludedTool(tool)) {
            return InteractionResult.PASS;
        }

        // 1 durability left on the tool
        if (tool.isDamageableItem() && (tool.getMaxDamage() - tool.getDamageValue()) == 1) {
            return InteractionResult.PASS;
        }

        Walker walker = WALKERS.get(playerState.getShape());

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
            player.awardStat(Stats.BLOCK_MINED.get(state.getBlock()));
            if (!tool.isEmpty() && tool.isDamageableItem()) {
                boolean itemIsAboutToBreak = tool.getMaxDamage() - tool.getDamageValue() <= 2;
                boolean preventFromBreaking = Liteminer.CONFIG.preventToolBreaking.get();
                if (itemIsAboutToBreak && preventFromBreaking) {
                    break;
                } else {
                    if (state.getDestroySpeed(level, block) != 0.0f) {
                        tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                    }
                }
            }
            boolean exhaustionEnabled = Liteminer.CONFIG.foodExhaustionEnabled.get();
            float exhaustion = Liteminer.CONFIG.foodExhaustion.get().floatValue();
            if (exhaustionEnabled && exhaustion > 0) {
                player.causeFoodExhaustion(exhaustion);
            }

            boolean skipDrops = state.requiresCorrectToolForDrops() && !tool.isCorrectToolForDrops(state);

            if (!skipDrops) {
                LootParams.Builder builder =
                        new LootParams.Builder((ServerLevel) level).withParameter(
                                        LootContextParams.ORIGIN,
                                        Vec3.atCenterOf(block)
                                )
                                .withParameter(LootContextParams.TOOL, tool)
                                .withParameter(LootContextParams.BLOCK_STATE, state)
                                .withParameter(LootContextParams.THIS_ENTITY, player);

                if (state.hasBlockEntity()) {
                    builder = builder.withParameter(
                            LootContextParams.BLOCK_ENTITY,
                            Objects.requireNonNull(level.getBlockEntity(block))
                    );
                }

                // Platform-specific XP handling
                if (Platform.isNeoForge()) {
                    // NeoForge: Call spawnAfterBreak without XP, then spawn XP manually
                    state.spawnAfterBreak((ServerLevel) level, block, tool, false);

                    // Get XP amount via platform helper (calls NeoForge's getExpDrop)
                    int xp = Services.PLATFORM.getBlockExperience(
                        (ServerLevel) level, block, state,
                        level.getBlockEntity(block), player, tool
                    );
                    if (xp > 0) {
                        ExperienceOrb.award((ServerLevel) level, Vec3.atCenterOf(block), xp);
                    }
                } else {
                    // Fabric: Vanilla behavior works correctly
                    state.spawnAfterBreak((ServerLevel) level, block, tool, true);
                }

                for (var stack : state.getDrops(builder)) {
                    var itemEntity = new ItemEntity(
                            level,
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

            // pray that mojang doesn't add more ice, or I'll have to come back here
            // The comment above doesn't help me at all. What the heck, Kaf??? What does this do??? - Kaf, 2025-12-18
            if (state.getBlock() instanceof IceBlock ice) {
                if (!EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_ICE_MELTING)) {
                    var waterEvaporatesEntry = level.dimensionType().attributes().get(EnvironmentAttributes.WATER_EVAPORATES);
                    boolean waterEvaporates = waterEvaporatesEntry != null && (Boolean) waterEvaporatesEntry.argument();
                    if (waterEvaporates) {
                        level.removeBlock(block, false);
                        continue;
                    }

                    BlockState below = level.getBlockState(block.below());
                    if (shouldMelt(below)) {
                        level.setBlockAndUpdate(block, IceBlock.meltsInto());
                    }
                }
            }
        }

        return InteractionResult.PASS;
    }

    // it's okay, i'll fix it if mojang breaks it
    @SuppressWarnings("deprecation")
    private static boolean shouldMelt(BlockState below) {
        return below.blocksMotion() || below.liquid();
    }
}
