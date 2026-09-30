package com.iamkaf.liteminer.event;

import com.iamkaf.amber.api.platform.v1.Platform;
import com.iamkaf.amber.api.event.v1.events.common.BlockEvents;
import com.iamkaf.liteminer.Liteminer;
import com.iamkaf.liteminer.LiteminerPlayerState;
import com.iamkaf.liteminer.api.event.LiteminerEvents;
import com.iamkaf.liteminer.api.shape.LiteminerShape;
import com.iamkaf.liteminer.api.shape.LiteminerShapes;
import com.iamkaf.liteminer.config.DropMode;
import com.iamkaf.liteminer.platform.Services;
import com.iamkaf.liteminer.shapes.VeinmineChecks;
import com.iamkaf.liteminer.tags.TagHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.EnchantmentTags;
//? if >=26.3
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

        if (FoodExhaustion.isTooHungry(player)) {
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

        int shapeIndex = playerState.getShape();
        LiteminerShape shape = LiteminerShapes.byIndex(shapeIndex).orElseThrow();
        int blockLimit = Liteminer.CONFIG.blockBreakLimit.get();
        InteractionResult startResult = LiteminerEvents.BEFORE_VEINMINE.invoker().beforeVeinmine(
                new LiteminerEvents.StartContext(
                        LiteminerEvents.Operation.BREAK,
                        level,
                        player,
                        null,
                        absoluteOrigin,
                        blockState,
                        blockEntity,
                        tool,
                        shape,
                        shapeIndex,
                        blockLimit
                )
        );
        if (startResult != InteractionResult.PASS) {
            return startResult;
        }

        var blocks = shape.walk(level, player, absoluteOrigin)
                .stream()
                .sorted(Comparator.comparingInt(p -> p.distManhattan(absoluteOrigin)))
                .toList();
        List<BlockPos> processed = new ArrayList<>();
        List<BlockPos> skipped = new ArrayList<>();
        int processedIncludingOrigin = 1;

        for (var block : blocks) {
            if (block.equals(absoluteOrigin)) {
                continue;
            }
            if (processedIncludingOrigin >= blockLimit) {
                break;
            }
            if (!VeinmineChecks.shouldMine(player, level, block)) {
                skipped.add(block);
                continue;
            }

            BlockState state = level.getBlockState(block);
            InteractionResult eventResult = LiteminerEvents.ALLOW_BLOCK.invoker()
                    .allowBlock(new LiteminerEvents.BlockContext(
                            LiteminerEvents.Operation.BREAK,
                            level,
                            player,
                            null,
                            absoluteOrigin,
                            blockState,
                            blockEntity,
                            block,
                            state,
                            level.getBlockEntity(block),
                            tool,
                            shape,
                            shapeIndex,
                            blockLimit
                    ));
            if (eventResult != InteractionResult.PASS) {
                skipped.add(block);
                continue;
            }

            if (player.isCreative()) {
                level.setBlockAndUpdate(block, Blocks.AIR.defaultBlockState());
                processed.add(block);
                processedIncludingOrigin++;
                continue;
            }
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
            FoodExhaustion.apply(player);

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

                ServerLevel serverLevel = (ServerLevel) level;
                BlockPos dropPos = Liteminer.CONFIG.dropMode() == DropMode.TOGETHER ? absoluteOrigin : block;

                // Fabric and Forge drop block XP from spawnAfterBreak at the position it is given.
                // NeoForge moved block XP out of spawnAfterBreak, so it is awarded here instead.
                if (Platform.isNeoForge()) {
                    state.spawnAfterBreak(serverLevel, dropPos, tool, false);
                    int xp = Services.PLATFORM.getBlockExperience(
                        serverLevel, block, state,
                        level.getBlockEntity(block), player, tool
                    );
                    if (xp > 0 && serverLevel.getGameRules().get(GameRules.BLOCK_DROPS)) {
                        ExperienceOrb.award(serverLevel, Vec3.atCenterOf(dropPos), xp);
                    }
                } else {
                    state.spawnAfterBreak(serverLevel, dropPos, tool, true);
                }

                for (var stack : state.getDrops(builder)) {
                    Block.popResource(level, dropPos, stack);
                }
            }

            level.setBlockAndUpdate(block, Blocks.AIR.defaultBlockState());
            processed.add(block);
            processedIncludingOrigin++;

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

        LiteminerEvents.AFTER_VEINMINE.invoker().afterVeinmine(new LiteminerEvents.ResultContext(
                LiteminerEvents.Operation.BREAK,
                level,
                player,
                null,
                absoluteOrigin,
                blockState,
                blockEntity,
                tool,
                shape,
                shapeIndex,
                blockLimit,
                blocks,
                processed,
                skipped
        ));

        return InteractionResult.PASS;
    }

    // it's okay, i'll fix it if mojang breaks it
    @SuppressWarnings("deprecation")
    private static boolean shouldMelt(BlockState below) {
        //? if >=26.3
        return below.is(BlockTags.ICE_MELTS_WHEN_DESTROYED_ABOVE) || below.liquid();
        //? if <26.3
        /*return below.blocksMotion() || below.liquid();*/
    }
}
