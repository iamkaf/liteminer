import {
  Capability,
  Readiness,
  describe,
  expect,
  test,
} from "@teakit/test";
import type { BlockId, BlockPos, ItemId, SpyHandle, TeaKitTestContext, Vec3 } from "@teakit/test";

describe.configure({
  timeout: "3m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreen,
    Capability.ClientScreens,
    Capability.ClientScreenshot,
    Capability.PlayerInteractions,
    Capability.PlayerInventory,
    Capability.PlayerReset,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
    Capability.SpyInstrumentation,
    Capability.WorldBlock,
    Capability.WorldClear,
    Capability.WorldEntities,
    Capability.WorldFill,
    Capability.WorldLoot,
    Capability.WorldSetBlock,
  ],
});

describe("Liteminer vein mining", () => {
  test("mines a connected ore vein", async (ctx) => {
    const area = box({ x: 0, y: 69, z: 0 }, { x: 4, y: 73, z: 4 });
    try {
      await prepareCreativeTest(ctx, { x: 2.5, y: 70, z: 0.5 }, area, 8);
      await ctx.world.fill({ x: 0, y: 69, z: 0 }, { x: 4, y: 69, z: 4 }, "minecraft:stone");
      await setBlocks(ctx, [
        block(1, 70, 2),
        block(2, 70, 2),
        block(3, 70, 2),
      ]);
      await assertBlocks(ctx, [
        block(1, 70, 2),
        block(2, 70, 2),
        block(3, 70, 2),
      ]);

      await holdVeinmineAndMine(ctx, { x: 2, y: 70, z: 2 }, { x: 2.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 1, y: 70, z: 2 },
        { x: 2, y: 70, z: 2 },
        { x: 3, y: 70, z: 2 },
      ]);
      await ctx.client.screenshot("liteminer-veinmine-basic");
    } finally {
      await cleanup(ctx, area, { x: 0, y: 70, z: 0 }, 8);
    }
  });

  test("keeps vein mining at zero hunger by default", { tags: ["hunger"] }, async (ctx) => {
    const area = hungerArea();
    try {
      await prepareZeroHungerTest(ctx, area);
      await holdVeinmineAndMine(ctx, HUNGER_ORIGIN, HUNGER_LOOK);
      await waitForAir(ctx, [HUNGER_ORIGIN, HUNGER_SECONDARY]);
    } finally {
      await cleanup(ctx, area, HUNGER_ORIGIN, 12);
    }
  });

  test("stops vein mining at zero hunger when configured", {
    target: { minecraft: ">=1.19" },
    tags: ["hunger"],
  }, async (ctx) => {
    const area = hungerArea();
    let zeroHungerDisallowed = false;
    let foodExhaustionDisabled = false;
    const hud = await ctx.spy.method("hunger.hud", "com.iamkaf.liteminer.rendering.HUD#statusLine");
    try {
      await toggleConfigEntry(ctx, "Allow Vein Mining at Zero Hunger");
      zeroHungerDisallowed = true;
      await prepareZeroHungerTest(ctx, area);
      await ctx.client.lookAt(HUNGER_LOOK);
      await ctx.client.keyState(96, true);
      await ctx.runtime.wait(1_200);
      await ctx.client.screenshot("liteminer-too-hungry-hud");
      assertMatches(JSON.stringify((await hud.$calls()).map((call) => call.returned)), /too_hungry/, "HUD too hungry warning");
      assertMatches(JSON.stringify((await hud.$calls()).map((call) => call.returned)), /"color"/, "HUD warning color");
      await ctx.player.mine(HUNGER_ORIGIN, { timeoutMs: 5_000 });
      await ctx.runtime.wait(500);
      await ctx.client.keyState(96, false);
      await waitForAir(ctx, [HUNGER_ORIGIN]);
      await assertBlock(ctx, HUNGER_SECONDARY, "minecraft:coal_ore");

      // Without food exhaustion, hunger never limits vein mining.
      await toggleConfigEntry(ctx, "Food Exhaustion");
      foodExhaustionDisabled = true;
      await ctx.world.setBlock(HUNGER_ORIGIN, "minecraft:coal_ore");
      await holdVeinmineAndMine(ctx, HUNGER_ORIGIN, HUNGER_LOOK);
      await waitForAir(ctx, [HUNGER_ORIGIN, HUNGER_SECONDARY]);
    } finally {
      await hud.$detach();
      await ctx.client.keyState(96, false);
      if (foodExhaustionDisabled) await toggleConfigEntry(ctx, "Food Exhaustion");
      if (zeroHungerDisallowed) await toggleConfigEntry(ctx, "Allow Vein Mining at Zero Hunger");
      await cleanup(ctx, area, HUNGER_ORIGIN, 12);
    }
  });

  test("renders highlight lines while selecting a vein", { tags: ["highlights"] }, async (ctx) => {
    await checkHighlightRendering(ctx);
  });

  test("keeps highlights visible when switching improved transparency", {
    target: { minecraft: ">=26.3" },
    tags: ["highlights"],
    capabilities: [Capability.ClientRenderProbes],
  }, async (ctx) => {
    await checkHighlightRendering(ctx, true);
  });

  test("collects secondary drops at the player-broken block", { tags: ["drops"] }, async (ctx) => {
    const dropPosition = await mineOreLineAndFindFarDrop(ctx);
    expect(dropPosition).toBeNear(ORE_LINE_ORIGIN, { distance: 3 });
  });

  test("drops items and experience at each block when configured", {
    target: { minecraft: ">=1.19" },
    tags: ["drops", "xp"],
  }, async (ctx) => {
    let eachBlock = false;
    try {
      await cycleConfigDropdown(ctx, "How should blocks drop");
      eachBlock = true;
      const dropPosition = await mineOreLineAndFindFarDrop(ctx);
      expect(dropPosition).toBeNear(ORE_LINE_FAR_END, { distance: 1.5 });
      const orbXs = await mineSculkAndCollectExperience(ctx);
      expect(orbXs.some((x) => x > 5.5)).toBe(true);
    } finally {
      if (eachBlock) await cycleConfigDropdown(ctx, "How should blocks drop");
    }
  });

  test("allows no-tool blocks when the correct-tool check is enabled", {
    target: { minecraft: ">=1.19 <26.3" },
  }, async (ctx) => {
    const area = box({ x: 108, y: 69, z: 0 }, { x: 114, y: 72, z: 6 });
    let correctToolToggled = false;
    try {
      await toggleRequireCorrectTool(ctx);
      correctToolToggled = true;
      await setShape(ctx, 0);
      await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
      await ctx.player.teleport({ x: 111, y: 70, z: 0 });
      await ctx.world.clear(area.min, area.max);
      await ctx.world.fill({ x: 108, y: 69, z: 0 }, { x: 114, y: 69, z: 6 }, "minecraft:dirt");

      await ctx.player.give("minecraft:golden_pickaxe");
      await ctx.player.inventory().selectHotbar(0);
      await setBlocks(ctx, [
        block(111, 70, 2, "minecraft:diamond_ore"),
        block(111, 70, 3, "minecraft:diamond_ore"),
      ]);
      await holdVeinmineAndMine(ctx, { x: 111, y: 70, z: 2 }, { x: 111.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [{ x: 111, y: 70, z: 2 }]);
      await assertBlock(ctx, { x: 111, y: 70, z: 3 }, "minecraft:diamond_ore");

      await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
      await ctx.world.clear({ x: 108, y: 70, z: 0 }, area.max);
      for (let z = 2; z <= 4; z += 1) {
        await ctx.world.setBlock({ x: 111, y: 70, z }, await shortGrassId(ctx));
      }
      await holdVeinmineAndMine(ctx, { x: 111, y: 70, z: 2 }, { x: 111.5, y: 70.25, z: 2.5 });
      await waitForAir(ctx, [
        { x: 111, y: 70, z: 2 },
        { x: 111, y: 70, z: 3 },
        { x: 111, y: 70, z: 4 },
      ]);
    } finally {
      await ctx.client.keyState(96, false);
      await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
      await ctx.world.clear(area.min, area.max);
      if (correctToolToggled) await toggleRequireCorrectTool(ctx);
    }
  });

  test("applies each configured mining shape", async (ctx) => {
    const area = box({ x: -4, y: 66, z: 0 }, { x: 44, y: 75, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 0.5, y: 70, z: 0.5 }, area, 24);
      await ctx.world.fill({ x: -4, y: 69, z: 0 }, { x: 44, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 0);
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(0, 70, 2), block(-1, 70, 2), block(1, 70, 2),
        block(0, 71, 2), block(0, 70, 3), block(3, 70, 2),
      ]);
      await mine(ctx, { x: 0, y: 70, z: 2 }, { x: 0.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 0, y: 70, z: 2 }, { x: -1, y: 70, z: 2 }, { x: 1, y: 70, z: 2 },
        { x: 0, y: 71, z: 2 }, { x: 0, y: 70, z: 3 },
      ]);
      await assertBlock(ctx, { x: 3, y: 70, z: 2 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-shapeless");

      await setShape(ctx, 1);
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(10, 70, 2), block(10, 70, 3), block(10, 70, 4), block(11, 70, 3),
      ]);
      await ctx.player.teleport({ x: 10.5, y: 70, z: 0.5 });
      await mineWithClientAttack(ctx, { x: 10, y: 70, z: 2 }, { x: 10.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 10, y: 70, z: 2 }, { x: 10, y: 70, z: 3 }, { x: 10, y: 70, z: 4 },
      ]);
      await assertBlock(ctx, { x: 11, y: 70, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-small-tunnel");

      await setShape(ctx, 2);
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(20, 70, 2), block(20, 71, 2), block(20, 72, 2),
        block(20, 71, 3), block(20, 72, 3), block(20, 73, 3),
        block(20, 72, 4), block(20, 73, 4), block(20, 74, 4), block(21, 72, 3),
      ]);
      await ctx.player.teleport({ x: 20.5, y: 70, z: 0.5 });
      await mineWithClientAttack(ctx, { x: 20, y: 71, z: 2 }, { x: 20.5, y: 71.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 20, y: 70, z: 2 }, { x: 20, y: 71, z: 2 }, { x: 20, y: 72, z: 2 },
        { x: 20, y: 71, z: 3 }, { x: 20, y: 72, z: 3 }, { x: 20, y: 73, z: 3 },
        { x: 20, y: 72, z: 4 }, { x: 20, y: 73, z: 4 }, { x: 20, y: 74, z: 4 },
      ]);
      await assertBlock(ctx, { x: 21, y: 72, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-staircase-up");

      await setShape(ctx, 3);
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(30, 71, 2), block(30, 70, 2), block(30, 69, 2),
        block(30, 70, 3), block(30, 69, 3), block(30, 68, 3),
        block(30, 69, 4), block(30, 68, 4), block(30, 67, 4), block(31, 69, 3),
      ]);
      await ctx.player.teleport({ x: 30.5, y: 70, z: 0.5 });
      await mineWithClientAttack(ctx, { x: 30, y: 70, z: 2 }, { x: 30.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 30, y: 70, z: 2 }, { x: 30, y: 69, z: 2 }, { x: 30, y: 68, z: 2 },
        { x: 30, y: 69, z: 3 }, { x: 30, y: 68, z: 3 }, { x: 30, y: 67, z: 3 },
        { x: 30, y: 68, z: 4 }, { x: 30, y: 67, z: 4 }, { x: 30, y: 66, z: 4 },
      ]);
      await assertBlock(ctx, { x: 31, y: 69, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-staircase-down");

      await setShape(ctx, 4);
      await ctx.runtime.wait(500);
      await ctx.world.fill({ x: 39, y: 69, z: 2 }, { x: 41, y: 71, z: 2 }, "minecraft:coal_ore");
      await ctx.world.setBlock({ x: 40, y: 70, z: 3 }, "minecraft:coal_ore");
      await ctx.player.teleport({ x: 40.5, y: 70, z: 0.5 });
      await mine(ctx, { x: 40, y: 70, z: 2 }, { x: 40.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, cuboidPositions({ x: 39, y: 69, z: 2 }, { x: 41, y: 71, z: 2 }));
      await assertBlock(ctx, { x: 40, y: 70, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-3x3");
      await setShape(ctx, 0);
    } finally {
      await cleanup(ctx, area, { x: 20, y: 70, z: 2 }, 24);
    }
  });

  test("uses the hit face for staircase direction", async (ctx) => {
    const area = box({ x: 46, y: 66, z: 0 }, { x: 86, y: 75, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 50, y: 70, z: 0 }, area, 32);
      await ctx.world.fill({ x: 46, y: 69, z: 0 }, { x: 86, y: 69, z: 6 }, "minecraft:stone");
      await ctx.client.keyState(96, true);

      await setShape(ctx, 2);
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(60, 70, 2), block(60, 71, 2), block(60, 72, 2),
        block(60, 71, 3), block(60, 72, 3), block(60, 73, 3), block(61, 72, 2),
      ]);
      await ctx.player.teleport({ x: 57, y: 70, z: 1 });
      await mineWithClientAttack(ctx, { x: 60, y: 71, z: 2 }, { x: 60.5, y: 71.5, z: 2.01 });
      await waitForAir(ctx, [
        { x: 60, y: 70, z: 2 }, { x: 60, y: 71, z: 2 }, { x: 60, y: 72, z: 2 },
        { x: 60, y: 71, z: 3 }, { x: 60, y: 72, z: 3 }, { x: 60, y: 73, z: 3 },
      ]);
      await assertBlock(ctx, { x: 61, y: 72, z: 2 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-staircase-up-hit-face-over-yaw");

      await setShape(ctx, 3);
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(80, 70, 2), block(80, 69, 2), block(80, 71, 2),
        block(80, 70, 3), block(80, 69, 3), block(80, 68, 3), block(81, 69, 2),
      ]);
      await ctx.player.teleport({ x: 77, y: 70, z: 1 });
      await mineWithClientAttack(ctx, { x: 80, y: 70, z: 2 }, { x: 80.5, y: 70.5, z: 2.01 });
      await waitForAir(ctx, [
        { x: 80, y: 70, z: 2 }, { x: 80, y: 69, z: 2 }, { x: 80, y: 68, z: 2 },
        { x: 80, y: 69, z: 3 }, { x: 80, y: 68, z: 3 }, { x: 80, y: 67, z: 3 },
      ]);
      await assertBlock(ctx, { x: 81, y: 69, z: 2 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-staircase-down-hit-face-over-yaw");
    } finally {
      await cleanup(ctx, area, { x: 66, y: 70, z: 2 }, 32);
    }
  });

  test("keeps an invalid lower staircase block", async (ctx) => {
    const area = box({ x: 66, y: 66, z: 0 }, { x: 74, y: 75, z: 4 });
    try {
      await prepareCreativeTest(ctx, { x: 70, y: 70, z: 0 }, area, 24);
      await ctx.world.fill({ x: 66, y: 69, z: 0 }, { x: 74, y: 69, z: 4 }, "minecraft:stone");
      await setShape(ctx, 2);
      await ctx.runtime.wait(500);
      await ctx.client.keyState(96, true);
      await ctx.runtime.wait(1_200);
      await setBlocks(ctx, [
        block(69, 70, 2, "minecraft:stone"),
        block(71, 70, 2, "minecraft:stone"),
        block(70, 70, 1, "minecraft:stone"),
        block(70, 70, 3, "minecraft:stone"),
      ]);
      await ctx.world.setBlock({ x: 70, y: 70, z: 2 }, "minecraft:water");
      await setBlocks(ctx, [block(70, 71, 2), block(70, 72, 2)]);
      await ctx.player.teleport({ x: 70, y: 71, z: 1 });
      await mine(ctx, { x: 70, y: 71, z: 2 }, { x: 70.5, y: 71.5, z: 2.01 });
      await waitForAir(ctx, [{ x: 70, y: 71, z: 2 }, { x: 70, y: 72, z: 2 }]);
      await assertBlock(ctx, { x: 70, y: 70, z: 2 }, "minecraft:water");
      await ctx.client.screenshot("liteminer-staircase-up-invalid-lower-block");
    } finally {
      await cleanup(ctx, area, { x: 70, y: 70, z: 0 }, 24);
    }
  });

  test("does not expand a tunnel from a torch into obsidian", { tags: ["shape-safety"] }, async (ctx) => {
    const area = box({ x: 118, y: 69, z: 0 }, { x: 122, y: 72, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 120, y: 70, z: 0 }, area, 16);
      await ctx.world.fill({ x: 118, y: 69, z: 0 }, { x: 122, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 1);
      await ctx.runtime.wait(500);
      await ctx.world.setBlock({ x: 120, y: 70, z: 2 }, "minecraft:torch");
      await ctx.world.setBlock({ x: 120, y: 70, z: 3 }, "minecraft:obsidian");
      await ctx.world.setBlock({ x: 120, y: 70, z: 4 }, "minecraft:obsidian");
      await mine(ctx, { x: 120, y: 70, z: 2 }, { x: 120.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [{ x: 120, y: 70, z: 2 }]);
      await assertBlock(ctx, { x: 120, y: 70, z: 3 }, "minecraft:obsidian");
      await assertBlock(ctx, { x: 120, y: 70, z: 4 }, "minecraft:obsidian");
      await ctx.client.screenshot("liteminer-torch-tunnel-leaves-obsidian");
      await setShape(ctx, 0);
    } finally {
      await cleanup(ctx, area, { x: 120, y: 70, z: 0 }, 16);
    }
  });

  test("does not expand a staircase from a torch into surrounding blocks", { tags: ["shape-safety"] }, async (ctx) => {
    const area = box({ x: 138, y: 69, z: 0 }, { x: 142, y: 74, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 140, y: 70, z: 0 }, area, 16);
      await ctx.world.fill({ x: 138, y: 69, z: 0 }, { x: 142, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 2);
      await ctx.runtime.wait(500);
      await ctx.world.setBlock({ x: 140, y: 70, z: 2 }, "minecraft:torch");
      await setBlocks(ctx, [
        block(140, 71, 2, "minecraft:obsidian"),
        block(140, 70, 3, "minecraft:obsidian"),
        block(140, 71, 3, "minecraft:obsidian"),
        block(140, 72, 3, "minecraft:obsidian"),
      ]);
      // player.mine targets the torch block. A screen-center click can miss the
      // torch hitbox, break the floor, and staircase into these sentinels.
      await mine(ctx, { x: 140, y: 70, z: 2 }, { x: 140.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [{ x: 140, y: 70, z: 2 }]);
      await assertBlock(ctx, { x: 140, y: 69, z: 2 }, "minecraft:stone");
      await assertBlock(ctx, { x: 140, y: 71, z: 2 }, "minecraft:obsidian");
      await assertBlock(ctx, { x: 140, y: 70, z: 3 }, "minecraft:obsidian");
      await assertBlock(ctx, { x: 140, y: 71, z: 3 }, "minecraft:obsidian");
      await assertBlock(ctx, { x: 140, y: 72, z: 3 }, "minecraft:obsidian");
      await ctx.client.screenshot("liteminer-torch-staircase-leaves-structure");
      await setShape(ctx, 0);
    } finally {
      await cleanup(ctx, area, { x: 140, y: 70, z: 0 }, 16);
    }
  });

  test("does not expand a 3x3 from a torch into obsidian", { tags: ["shape-safety"] }, async (ctx) => {
    const area = box({ x: 158, y: 69, z: 0 }, { x: 162, y: 72, z: 4 });
    try {
      await prepareCreativeTest(ctx, { x: 160, y: 70, z: 0 }, area, 16);
      await ctx.world.fill({ x: 158, y: 69, z: 0 }, { x: 162, y: 69, z: 4 }, "minecraft:stone");
      await setShape(ctx, 4);
      await ctx.runtime.wait(500);
      await ctx.world.setBlock({ x: 159, y: 70, z: 2 }, "minecraft:obsidian");
      await ctx.world.setBlock({ x: 161, y: 70, z: 2 }, "minecraft:obsidian");
      await ctx.world.setBlock({ x: 160, y: 70, z: 2 }, "minecraft:torch");
      await mine(ctx, { x: 160, y: 70, z: 2 }, { x: 160.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [{ x: 160, y: 70, z: 2 }]);
      await assertBlock(ctx, { x: 159, y: 70, z: 2 }, "minecraft:obsidian");
      await assertBlock(ctx, { x: 161, y: 70, z: 2 }, "minecraft:obsidian");
      await ctx.client.screenshot("liteminer-torch-3x3-leaves-obsidian");
      await setShape(ctx, 0);
    } finally {
      await cleanup(ctx, area, { x: 160, y: 70, z: 0 }, 16);
    }
  });

  test("tunnels through mixed stone and dirt", { tags: ["shape-safety"] }, async (ctx) => {
    const area = box({ x: 168, y: 69, z: 0 }, { x: 172, y: 72, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 170, y: 70, z: 0 }, area, 16);
      await ctx.world.fill({ x: 168, y: 69, z: 0 }, { x: 172, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 1);
      await ctx.runtime.wait(500);
      await ctx.world.setBlock({ x: 170, y: 70, z: 2 }, "minecraft:stone");
      await ctx.world.setBlock({ x: 170, y: 70, z: 3 }, "minecraft:dirt");
      await ctx.world.setBlock({ x: 170, y: 70, z: 4 }, "minecraft:stone");
      await mineWithClientAttack(ctx, { x: 170, y: 70, z: 2 }, { x: 170.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 170, y: 70, z: 2 },
        { x: 170, y: 70, z: 3 },
        { x: 170, y: 70, z: 4 },
      ]);
      await setShape(ctx, 0);
    } finally {
      await cleanup(ctx, area, { x: 170, y: 70, z: 0 }, 16);
    }
  });

  test("shapeless still veins connected torches", { tags: ["shape-safety"] }, async (ctx) => {
    const area = box({ x: 178, y: 69, z: 0 }, { x: 182, y: 72, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 180, y: 70, z: 0 }, area, 16);
      await ctx.world.fill({ x: 178, y: 69, z: 0 }, { x: 182, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 0);
      await ctx.runtime.wait(500);
      await ctx.world.setBlock({ x: 180, y: 70, z: 2 }, "minecraft:torch");
      await ctx.world.setBlock({ x: 180, y: 70, z: 3 }, "minecraft:torch");
      await ctx.world.setBlock({ x: 180, y: 70, z: 4 }, "minecraft:torch");
      await mine(ctx, { x: 180, y: 70, z: 2 }, { x: 180.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 180, y: 70, z: 2 },
        { x: 180, y: 70, z: 3 },
        { x: 180, y: 70, z: 4 },
      ]);
    } finally {
      await cleanup(ctx, area, { x: 180, y: 70, z: 0 }, 16);
    }
  });

  test("limits a staircase selection to three layers", async (ctx) => {
    const area = box({ x: 86, y: 66, z: 0 }, { x: 96, y: 76, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 90, y: 70, z: 0 }, area, 24);
      await ctx.world.fill({ x: 86, y: 69, z: 0 }, { x: 96, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 2);
      await ctx.runtime.wait(500);
      await ctx.client.keyState(96, true);
      await ctx.runtime.wait(1_200);
      await setBlocks(ctx, [
        block(90, 70, 2), block(90, 71, 2), block(90, 72, 2),
        block(90, 71, 3), block(90, 72, 3), block(90, 73, 3),
      ]);
      await mine(ctx, { x: 90, y: 71, z: 2 }, { x: 90.5, y: 71.5, z: 2.01 });
      await waitForAir(ctx, [
        { x: 90, y: 70, z: 2 }, { x: 90, y: 71, z: 2 }, { x: 90, y: 72, z: 2 },
        { x: 90, y: 71, z: 3 }, { x: 90, y: 72, z: 3 }, { x: 90, y: 73, z: 3 },
      ]);
      await ctx.client.screenshot("liteminer-staircase-limit-3");
    } finally {
      await cleanup(ctx, area, { x: 90, y: 70, z: 0 }, 24);
    }
  });

  test("respects Silk Touch for secondary ore experience", async (ctx) => {
    const area = box({ x: 0, y: 69, z: 0 }, { x: 8, y: 73, z: 4 });
    try {
      await prepareSurvivalXpTest(ctx, area);
      await ctx.player.give("minecraft:netherite_pickaxe");
      await ctx.player.inventory().selectHotbar(0);
      await ctx.commands.assert("/enchant @s minecraft:silk_touch 1");
      await setExperience(ctx, 6);
      await setBlocks(ctx, [block(1, 70, 2, "minecraft:diamond_ore"), block(2, 70, 2, "minecraft:diamond_ore"), block(3, 70, 2, "minecraft:diamond_ore")]);
      await holdVeinmineAndMine(ctx, { x: 2, y: 70, z: 2 }, { x: 2.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [{ x: 1, y: 70, z: 2 }, { x: 2, y: 70, z: 2 }, { x: 3, y: 70, z: 2 }], 5_000);
      await ctx.entities.query({ origin: { x: 2, y: 70, z: 2 }, radius: 12, type: "minecraft:experience_orb" }).waitForCount(0, { timeoutMs: 1_000 });
      await ctx.player.teleport({ x: 2, y: 70, z: 2 });
      await ctx.runtime.wait(500);
      await ctx.commands.assert("/execute unless entity @s[level=1..]");

      await prepareSurvivalXpTest(ctx, area);
      await ctx.player.give("minecraft:netherite_pickaxe");
      await ctx.player.inventory().selectHotbar(0);
      await setExperience(ctx, 6);
      await setBlocks(ctx, [block(1, 70, 2, "minecraft:diamond_ore"), block(2, 70, 2, "minecraft:diamond_ore"), block(3, 70, 2, "minecraft:diamond_ore")]);
      await holdVeinmineAndMine(ctx, { x: 2, y: 70, z: 2 }, { x: 2.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [{ x: 1, y: 70, z: 2 }, { x: 2, y: 70, z: 2 }, { x: 3, y: 70, z: 2 }], 5_000);
      await ctx.player.teleport({ x: 2, y: 70, z: 2 });
      await ctx.runtime.wait(500);
      await ctx.commands.assert("/execute if entity @s[level=1..]");
    } finally {
      await ctx.client.keyState(96, false);
      await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
      await setExperience(ctx, 0);
      await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:item");
      await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:experience_orb");
      await ctx.world.clear(area.min, area.max);
    }
  });

  test("drops every block's experience together at the player-broken block", { tags: ["xp"] }, async (ctx) => {
    const orbXs = await mineSculkAndCollectExperience(ctx);
    expect(orbXs.length).toBeGreaterThan(0);
    expect(orbXs.every((x) => x < 4.5)).toBe(true);
  });

  test("drops nothing from secondary blocks when block drops are off", { tags: ["drops", "xp"] }, async (ctx) => {
    const area = box({ x: 0, y: 69, z: 0 }, { x: 8, y: 73, z: 4 });
    const ores = [1, 2, 3].map((x) => block(x, 70, 2, "minecraft:diamond_ore"));
    let dropsOff = false;
    try {
      await prepareSurvivalXpTest(ctx, area);
      await ctx.player.give("minecraft:netherite_pickaxe");
      await ctx.player.inventory().selectHotbar(0);
      await ctx.commands.assert(`/gamerule ${await blockDropsRule(ctx)} false`);
      dropsOff = true;
      await setBlocks(ctx, ores);
      await holdVeinmineAndMine(ctx, { x: 2, y: 70, z: 2 }, { x: 2.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, ores.map((ore) => ore.pos), 5_000);
      await ctx.runtime.wait(500);
      await ctx.entities.query({ origin: { x: 2, y: 70, z: 2 }, radius: 12, type: "minecraft:item" }).waitForCount(0, { timeoutMs: 1_000 });
      await ctx.entities.query({ origin: { x: 2, y: 70, z: 2 }, radius: 12, type: "minecraft:experience_orb" }).waitForCount(0, { timeoutMs: 1_000 });
      await ctx.commands.assert("/execute unless entity @s[level=1..]");
    } finally {
      if (dropsOff) await ctx.commands.assert(`/gamerule ${await blockDropsRule(ctx)} true`);
      await ctx.client.keyState(96, false);
      await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
      await setExperience(ctx, 0);
      await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:item");
      await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:experience_orb");
      await ctx.world.clear(area.min, area.max);
    }
  });

  test("drops the block that breaks the tool", {
    target: { minecraft: ">=1.19" },
    tags: ["drops"],
  }, async (ctx) => {
    const area = box({ x: 188, y: 69, z: 0 }, { x: 192, y: 73, z: 6 });
    const origin = { x: 190, y: 71, z: 2 };
    const ores = [189, 191].map((x) => block(x, 71, 2));
    let toolBreakingAllowed = false;
    try {
      await toggleConfigEntry(ctx, "Prevent Tool Breaking");
      toolBreakingAllowed = true;
      await ctx.client.keyState(96, false);
      await setShape(ctx, 4);
      await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
      await removeEntities(ctx, origin, 12, "minecraft:item");
      await ctx.world.clear(area.min, area.max);
      await ctx.world.fill({ x: 188, y: 69, z: 0 }, { x: 192, y: 69, z: 6 }, "minecraft:stone");
      await ctx.player.teleport({ x: 190.5, y: 70, z: 0.5 });
      // Two durability left: the first ore takes one, and the second breaks the pickaxe.
      await ctx.commands.assert(`/item replace entity @s hotbar.0 with ${await nearlyBrokenPickaxe(ctx)}`);
      await ctx.player.inventory().selectHotbar(0);
      // A stone origin keeps the count to the two ores. Whether the origin drops depends on the loader.
      await setBlocks(ctx, [block(origin.x, origin.y, origin.z, "minecraft:stone"), ...ores]);
      await holdVeinmineAndMine(ctx, origin, { x: 190.5, y: 71.5, z: 2.5 });
      await waitForAir(ctx, [origin, ...ores.map((ore) => ore.pos)], 5_000);
      await ctx.player.inventory().waitForItemAbsent("minecraft:diamond_pickaxe", { timeout: "1s" });

      await ctx.player.teleport({ x: 190.5, y: 70, z: 2.5 });
      await expect(() => countInInventory(ctx, "minecraft:coal")).toEventuallyEqual(2, { timeout: 5_000, interval: 100 });
    } finally {
      if (toolBreakingAllowed) await toggleConfigEntry(ctx, "Prevent Tool Breaking");
      await setShape(ctx, 0);
      await cleanup(ctx, area, origin, 12);
    }
  });

  test("leaves blocks outside the world border", { tags: ["protection"] }, async (ctx) => {
    const area = box({ x: 198, y: 69, z: 0 }, { x: 202, y: 72, z: 6 });
    const inside = [2, 3].map((z) => block(200, 70, z));
    const outside = [4, 5].map((z) => block(200, 70, z));
    let borderMoved = false;
    try {
      await ctx.client.closeMenus();
      await ctx.client.keyState(96, false);
      await setShape(ctx, 0);
      await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
      await ctx.world.clear(area.min, area.max);
      await ctx.world.fill({ x: 198, y: 69, z: 0 }, { x: 202, y: 69, z: 6 }, "minecraft:stone");
      await ctx.player.teleport({ x: 200.5, y: 70, z: 0.5 });
      await ctx.player.give("minecraft:netherite_pickaxe");
      await ctx.player.inventory().selectHotbar(0);
      await setBlocks(ctx, [...inside, ...outside]);
      // The border spans z -2 to 4, so the vein crosses it between z 3 and z 4.
      await ctx.commands.batch(["/worldborder center 200.5 1.0", "/worldborder set 6"], { requireSuccess: true });
      borderMoved = true;

      await holdVeinmineAndMine(ctx, inside[0].pos, { x: 200.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, inside.map((ore) => ore.pos));
      await ctx.runtime.wait(500);
      await assertBlocks(ctx, outside);
    } finally {
      if (borderMoved) await ctx.commands.batch(["/worldborder set 59999968", "/worldborder center 0 0"], { requireSuccess: true });
      await cleanup(ctx, area, inside[0].pos, 12);
    }
  });
});

describe("Liteminer vein interactions", () => {
  test("strips a log vein", { tags: ["interact"] }, async (ctx) => {
    const area = box({ x: 218, y: 69, z: 0 }, { x: 222, y: 74, z: 4 });
    const logs = [70, 71, 72].map((y) => block(220, y, 2, "minecraft:oak_log"));
    try {
      await prepareStripTest(ctx, area, { x: 220.5, y: 70, z: 0.5 }, 0);
      await setBlocks(ctx, logs);
      await holdVeinmineAndUse(ctx, logs[0].pos, { x: 220.5, y: 70.5, z: 2.5 });
      await assertBlocks(ctx, logs.map((log) => ({ ...log, id: "minecraft:stripped_oak_log" })));
      expect(await heldDamage(ctx)).toBe(3);
    } finally {
      await setShape(ctx, 0);
      await cleanup(ctx, area, logs[0].pos, 8);
    }
  });

  test("leaves the shape alone when the clicked block doesn't change", { tags: ["interact"] }, async (ctx) => {
    const area = box({ x: 228, y: 69, z: 0 }, { x: 232, y: 74, z: 4 });
    const origin = block(230, 71, 2, "minecraft:stone");
    const logs = [229, 231].map((x) => block(x, 71, 2, "minecraft:oak_log"));
    try {
      // The 3x3 around stone holds two logs. An axe does nothing to stone, so vanilla never uses it there.
      await prepareStripTest(ctx, area, { x: 230.5, y: 70, z: 0.5 }, 4);
      await setBlocks(ctx, [origin, ...logs]);
      await holdVeinmineAndUse(ctx, origin.pos, { x: 230.5, y: 71.5, z: 2.5 });
      await assertBlocks(ctx, [origin, ...logs]);
      expect(await heldDamage(ctx)).toBe(0);
    } finally {
      await setShape(ctx, 0);
      await cleanup(ctx, area, origin.pos, 8);
    }
  });
});

describe("Liteminer controls", () => {
  test("toggles vein mining with one key press in toggle mode", { tags: ["keybinds"] }, async (ctx) => {
    const area = box({ x: 238, y: 69, z: 0 }, { x: 242, y: 72, z: 6 });
    const acks = await ctx.spy.method("controls.acks", "com.iamkaf.liteminer.networking.ClientMiningState#acknowledge");
    let toggleMode = false;
    try {
      await cycleConfigDropdown(ctx, "Key Mode");
      toggleMode = true;
      await prepareCreativeTest(ctx, { x: 240, y: 70, z: 0 }, area, 12);
      await ctx.world.fill({ x: 238, y: 69, z: 0 }, { x: 242, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 0);
      await ctx.runtime.wait(500);
      const line = [2, 3, 4].map((z) => block(240, 70, z));
      await setBlocks(ctx, line);
      await ctx.client.lookAt({ x: 240.5, y: 70.5, z: 2.5 });

      // One press turns vein mining on and it stays on after the key comes up.
      await tapVeinmineKey(ctx);
      await ctx.runtime.wait(1_200);
      await expectMiningState(acks, true);
      await ctx.player.mine(line[0].pos, { timeoutMs: 5_000 });
      await waitForAir(ctx, line.map((ore) => ore.pos));

      // A second press turns it off again, so only the broken block goes.
      await setBlocks(ctx, line);
      await tapVeinmineKey(ctx);
      await ctx.runtime.wait(1_200);
      await expectMiningState(acks, false);
      await ctx.player.mine(line[0].pos, { timeoutMs: 5_000 });
      await waitForAir(ctx, [line[0].pos]);
      await ctx.runtime.wait(500);
      await assertBlocks(ctx, line.slice(1));
    } finally {
      await ctx.client.keyState(KEY_VEINMINE, false);
      await acks.$detach();
      if (toggleMode) await cycleConfigDropdown(ctx, "Key Mode");
      await cleanup(ctx, area, { x: 240, y: 70, z: 0 }, 12);
    }
  });

  test("stops vein mining when the hold key is released", { tags: ["keybinds"] }, async (ctx) => {
    const acks = await ctx.spy.method("controls.release", "com.iamkaf.liteminer.networking.ClientMiningState#acknowledge");
    try {
      await ctx.client.closeMenus();
      await ctx.client.keyState(KEY_VEINMINE, true);
      await ctx.runtime.wait(1_200);
      await expectMiningState(acks, true);
      await ctx.client.keyState(KEY_VEINMINE, false);
      await ctx.runtime.wait(1_200);
      await expectMiningState(acks, false);
    } finally {
      await ctx.client.keyState(KEY_VEINMINE, false);
      await acks.$detach();
    }
  });

  test("cycles the shape with the scroll wheel while the key is held", { tags: ["keybinds"] }, async (ctx) => {
    const area = box({ x: 248, y: 69, z: 0 }, { x: 252, y: 72, z: 6 });
    const acks = await ctx.spy.method("controls.shapes", "com.iamkaf.liteminer.networking.ClientMiningState#acknowledge");
    let hud = await ctx.spy.method("controls.hud", "com.iamkaf.liteminer.api.shape.LiteminerShape#displayName");
    const scroll = (amount: number) => ctx.client.scroll({ x: 213.5, y: 120, verticalAmount: amount });
    try {
      await prepareCreativeTest(ctx, { x: 250, y: 70, z: 0 }, area, 12);
      await ctx.world.fill({ x: 248, y: 69, z: 0 }, { x: 252, y: 69, z: 6 }, "minecraft:stone");
      await setShape(ctx, 0);
      await ctx.runtime.wait(500);
      await ctx.client.keyState(KEY_VEINMINE, true);
      await ctx.runtime.wait(1_200);
      expect((await lastAppliedState(acks)).shape).toBe("liteminer:shapeless");

      // Scrolling down walks forward through the registered shapes and wraps. Scrolling up walks back.
      const forward = ["small_tunnel", "staircase_up", "staircase_down", "three_by_three", "shapeless"];
      for (const shape of forward) {
        await scroll(-1);
        await ctx.runtime.wait(400);
        expect((await lastAppliedState(acks)).shape).toBe(`liteminer:${shape}`);
      }
      await scroll(1);
      await ctx.runtime.wait(400);
      expect((await lastAppliedState(acks)).shape).toBe("liteminer:three_by_three");
      await scroll(1);
      await ctx.runtime.wait(400);
      expect((await lastAppliedState(acks)).shape).toBe("liteminer:staircase_down");

      // The server applies the cycled shape: only a tunnel digs through mixed blocks.
      await scroll(1);
      await ctx.runtime.wait(400);
      await scroll(1);
      await ctx.runtime.wait(400);
      expect((await lastAppliedState(acks)).shape).toBe("liteminer:small_tunnel");
      // Reading the whole history of a busy spy is heavy, so watch from a fresh spy.
      await hud.$detach();
      hud = await ctx.spy.method("controls.hud.fresh", "com.iamkaf.liteminer.api.shape.LiteminerShape#displayName");
      await ctx.runtime.wait(300);
      await ctx.client.screenshot("liteminer-scroll-cycled-shape");
      assertMatches(JSON.stringify((await hud.$calls({ limit: 3 })).map((call) => call.returned)), /small_tunnel/, "HUD shape name");
      await setBlocks(ctx, [block(250, 70, 2, "minecraft:stone"), block(250, 70, 3, "minecraft:dirt"), block(250, 70, 4, "minecraft:stone")]);
      await ctx.client.lookAt({ x: 250.5, y: 70.5, z: 2.5 });
      await attackBlock(ctx, { x: 250, y: 70, z: 2 });
      await waitForAir(ctx, [{ x: 250, y: 70, z: 2 }, { x: 250, y: 70, z: 3 }, { x: 250, y: 70, z: 4 }]);
    } finally {
      await ctx.client.keyState(KEY_VEINMINE, false);
      await acks.$detach();
      await hud.$detach();
      await setShape(ctx, 0);
      await cleanup(ctx, area, { x: 250, y: 70, z: 0 }, 12);
    }
  });
});

describe("Liteminer block rules", () => {
  test("melts ice below a vein unless the tool has Silk Touch", { tags: ["ice"] }, async (ctx) => {
    const area = box({ x: 258, y: 69, z: -1 }, { x: 262, y: 73, z: 8 });
    const ice = [2, 3, 4].map((z) => block(260, 70, z, "minecraft:ice"));
    const waterAt = (z: number) => ({ x: 260, y: 70, z });
    try {
      await prepareSurvivalLine(ctx, area, { x: 260, y: 70, z: 0 });
      await setBlocks(ctx, ice);
      await holdVeinmineAndAttack(ctx, ice[0].pos, { x: 260.5, y: 70.5, z: 2.5 }, "minecraft:water");
      // Like vanilla ice, each secondary block melts into water over a solid block.
      for (const z of [3, 4]) {
        await expect(async () => (await ctx.world.block(waterAt(z))).id).toEventuallyEqual("minecraft:water", {
          timeout: 3_000,
          interval: 50,
        });
      }

      await prepareSurvivalLine(ctx, area, { x: 260, y: 70, z: 0 });
      await ctx.commands.assert("/enchant @s minecraft:silk_touch 1");
      await setBlocks(ctx, ice);
      await holdVeinmineAndAttack(ctx, ice[0].pos, { x: 260.5, y: 70.5, z: 2.5 }, "minecraft:air");
      await waitForAir(ctx, ice.map((entry) => entry.pos));
      await ctx.runtime.wait(500);
      for (const entry of ice) expect((await ctx.world.block(entry.pos)).id).toBe("minecraft:air");
    } finally {
      await cleanup(ctx, area, { x: 260, y: 70, z: 0 }, 12);
    }
  });

  test("keeps tagged blocks out of a vein", { tags: ["tags"] }, async (ctx) => {
    const area = box({ x: 268, y: 69, z: 0 }, { x: 272, y: 72, z: 8 });
    const bedrock = [2, 3, 4].map((z) => block(270, 70, z, "minecraft:bedrock"));
    const coal = [2, 3, 4].map((z) => block(272, 70, z));
    try {
      await prepareCreativeTest(ctx, { x: 270, y: 70, z: 0 }, area, 12);
      await ctx.world.fill({ x: 268, y: 69, z: 0 }, { x: 272, y: 69, z: 8 }, "minecraft:stone");
      await setShape(ctx, 0);
      await ctx.runtime.wait(500);
      // Creative players break bedrock, so only the excluded-blocks tag keeps the rest of the line.
      await setBlocks(ctx, bedrock);
      await holdVeinmineAndMine(ctx, bedrock[0].pos, { x: 270.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [bedrock[0].pos]);
      await ctx.runtime.wait(500);
      await assertBlocks(ctx, bedrock.slice(1));

      // The same line of a mineable block goes together.
      await ctx.player.teleport({ x: 272.5, y: 70, z: 0.5 });
      await setBlocks(ctx, coal);
      await holdVeinmineAndMine(ctx, coal[0].pos, { x: 272.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, coal.map((ore) => ore.pos));
    } finally {
      await cleanup(ctx, area, { x: 270, y: 70, z: 0 }, 12);
    }
  });
});

describe("Liteminer config options", () => {
  test("caps a vein at the block break limit", { tags: ["config"] }, async (ctx) => {
    const area = box({ x: 278, y: 69, z: 0 }, { x: 282, y: 72, z: 10 });
    const line = [2, 3, 4, 5, 6, 7].map((z) => block(280, 70, z));
    try {
      // The limit counts the block that started the vein, so a limit of 3 breaks it and two neighbors.
      await setConfigInt(ctx, "Block Break Limit", 3, 0);
      await prepareCreativeTest(ctx, { x: 280, y: 70, z: 0 }, area, 12);
      await ctx.world.fill({ x: 278, y: 69, z: 0 }, { x: 282, y: 69, z: 10 }, "minecraft:stone");
      await setShape(ctx, 0);
      await ctx.runtime.wait(500);
      await setBlocks(ctx, line);
      await holdVeinmineAndMine(ctx, line[0].pos, { x: 280.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, line.slice(0, 3).map((ore) => ore.pos));
      await ctx.runtime.wait(500);
      await assertBlocks(ctx, line.slice(3));
    } finally {
      await setConfigInt(ctx, "Block Break Limit", 64, 63 / 2047);
      await cleanup(ctx, area, { x: 280, y: 70, z: 0 }, 12);
    }
  });

  test("slows mining by the size of the selection when configured", { tags: ["config"] }, async (ctx) => {
    const area = box({ x: 288, y: 69, z: 0 }, { x: 292, y: 72, z: 8 });
    const line = [2, 3, 4, 5].map((z) => block(290, 70, z));
    const scaled = await ctx.spy.method("config.harvest", "com.iamkaf.liteminer.Liteminer#getScaledBreakSpeedModifier");
    let enabled = false;
    try {
      await prepareSurvivalLine(ctx, area, { x: 290, y: 70, z: 0 });
      await setBlocks(ctx, line);
      await holdVeinmineAndMine(ctx, line[0].pos, { x: 290.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, line.map((ore) => ore.pos));
      expect((await scaled.$calls()).length).toBe(0);

      await toggleConfigEntry(ctx, "Increased Harvesting Time");
      enabled = true;
      // The slider's top end makes the break speed one over the number of selected blocks.
      await setConfigSlider(ctx, "Harvesting Time Modifier", 1);
      await setBlocks(ctx, line);
      await holdVeinmineAndMine(ctx, line[0].pos, { x: 290.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, line.map((ore) => ore.pos));
      const modifiers = (await scaled.$calls()).filter((call) => call.args?.[0] === 4).map((call) => Number(call.returned));
      expect(modifiers.length).toBeGreaterThan(0);
      for (const modifier of modifiers) expect(Math.abs(modifier - 0.25)).toBeLessThan(0.001);
    } finally {
      await scaled.$detach();
      if (enabled) {
        await toggleConfigEntry(ctx, "Increased Harvesting Time");
        await setConfigSlider(ctx, "Harvesting Time Modifier", 1 / 9);
      }
      await cleanup(ctx, area, { x: 290, y: 70, z: 0 }, 12);
    }
  });

  test("charges the configured food exhaustion for each secondary block", { tags: ["config", "hunger"] }, async (ctx) => {
    const area = box({ x: 298, y: 69, z: 0 }, { x: 302, y: 72, z: 8 });
    const line = [2, 3, 4, 5].map((z) => block(300, 70, z));
    const exhaustion = await ctx.spy.method("config.exhaustion", "com.iamkaf.liteminer.event.FoodExhaustion#getExhaustion");
    const applied = await ctx.spy.method("config.exhaustion.applied", "com.iamkaf.liteminer.event.FoodExhaustion#apply");
    // Amounts and applications since the last mining run.
    let seen = 0;
    let appliedSeen = 0;
    const mineLine = async () => {
      await setBlocks(ctx, line);
      await holdVeinmineAndMine(ctx, line[0].pos, { x: 300.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, line.map((ore) => ore.pos));
      const calls = await exhaustion.$calls();
      const amounts = calls.slice(seen).map((call) => Number(call.returned));
      seen = calls.length;
      const charges = (await applied.$calls()).length - appliedSeen;
      appliedSeen += charges;
      return { amounts, charges };
    };
    let amountRaised = false;
    let disabled = false;
    try {
      await prepareSurvivalLine(ctx, area, { x: 300, y: 70, z: 0 });
      // Each of the three blocks after the first is charged, at the default of 0.2.
      const byDefault = await mineLine();
      expect(byDefault.charges).toBe(3);
      expect(byDefault.amounts.length).toBeGreaterThan(0);
      for (const amount of byDefault.amounts) expect(Math.abs(amount - 0.2)).toBeLessThan(0.01);

      await setConfigSlider(ctx, "Food Exhaustion Amount", 1);
      amountRaised = true;
      const raised = await mineLine();
      expect(raised.charges).toBe(3);
      for (const amount of raised.amounts) expect(Math.abs(amount - 1)).toBeLessThan(0.01);

      await toggleConfigEntry(ctx, "Food Exhaustion");
      disabled = true;
      const off = await mineLine();
      expect(off.amounts.length).toBe(0);
    } finally {
      await exhaustion.$detach();
      await applied.$detach();
      if (disabled) await toggleConfigEntry(ctx, "Food Exhaustion");
      if (amountRaised) await setConfigSlider(ctx, "Food Exhaustion Amount", 0.2);
      await cleanup(ctx, area, { x: 300, y: 70, z: 0 }, 12);
    }
  });

  test("keeps growing crops out of a grown crop vein unless configured", { tags: ["config"] }, async (ctx) => {
    const area = box({ x: 308, y: 69, z: 0 }, { x: 312, y: 72, z: 8 });
    const crops = [{ z: 2, age: 7 }, { z: 3, age: 7 }, { z: 4, age: 3 }, { z: 5, age: 7 }];
    const cropAt = (z: number) => ({ x: 310, y: 70, z });
    let distinguishOff = false;
    const plant = async () => {
      await ctx.world.fill({ x: 310, y: 69, z: 2 }, { x: 310, y: 69, z: 5 }, "minecraft:farmland");
      // A crop that is already in the requested state makes /setblock report failure.
      for (const crop of crops) await ctx.commands.run(`/setblock 310 70 ${crop.z} minecraft:wheat[age=${crop.age}]`);
    };
    try {
      await prepareCreativeTest(ctx, { x: 310, y: 70, z: 0 }, area, 12);
      await ctx.world.fill({ x: 308, y: 69, z: 0 }, { x: 312, y: 69, z: 8 }, "minecraft:stone");
      await setShape(ctx, 0);
      await ctx.runtime.wait(500);
      await plant();
      await holdVeinmineAndMine(ctx, cropAt(2), { x: 310.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [cropAt(2), cropAt(3)]);
      await ctx.runtime.wait(500);
      expect((await ctx.world.block(cropAt(4))).id).toBe("minecraft:wheat");
      expect((await ctx.world.block(cropAt(5))).id).toBe("minecraft:wheat");

      await toggleConfigEntry(ctx, "Distinguish Grown Crops");
      distinguishOff = true;
      await plant();
      await holdVeinmineAndMine(ctx, cropAt(2), { x: 310.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, crops.map((crop) => cropAt(crop.z)));
    } finally {
      if (distinguishOff) await toggleConfigEntry(ctx, "Distinguish Grown Crops");
      await cleanup(ctx, area, { x: 310, y: 70, z: 0 }, 12);
    }
  });
});

describe("Liteminer client rendering", () => {
  test("draws highlight lines and HUD text for a selection", { tags: ["highlights", "hud"] }, async (ctx) => {
    const area = box({ x: 8, y: 69, z: 0 }, { x: 16, y: 72, z: 6 });
    const render = await watchRendering(ctx);
    try {
      await prepareSelection(ctx, area);
      await ctx.client.screenshot("liteminer-selection-render");

      const drawn = await render.lines.$calls();
      expect(drawn.length).toBeGreaterThan(0);
      const last = drawn[drawn.length - 1];
      // Four blocks in an L share their outer edges, so the merged outline has at least a box's twelve.
      const lines = last?.args?.[2];
      expect(Array.isArray(lines) && lines.length >= 12).toBe(true);
      // The see-through lines are translucent, the solid ones opaque, in the configured default colors.
      expect(Number(last?.args?.[3]) | 0).toBe(0x4b169c9c | 0);
      expect(Number(last?.args?.[4]) | 0).toBe(0xfff9fffe | 0);

      const hud = JSON.stringify((await render.hud.$calls()).map((call) => call.returned));
      assertMatches(hud, /selected_blocks/, "HUD block count");
      assertMatches(hud, /"args":\[4\]/, "HUD block count of 4");
      assertMatches(JSON.stringify((await render.shape.$calls({ limit: 3 })).map((call) => call.returned)), /shape\.liteminer\.shapeless/, "HUD shape name");
    } finally {
      await render.detach();
      await cleanup(ctx, area, { x: 12, y: 70, z: 0 }, 12);
    }
  });

  test("hides highlights and the HUD when they are switched off", { tags: ["highlights", "hud"] }, async (ctx) => {
    const area = box({ x: 8, y: 69, z: 0 }, { x: 16, y: 72, z: 6 });
    const render = await watchRendering(ctx);
    let highlightsOff = false;
    let hudOff = false;
    try {
      await toggleConfigEntry(ctx, "Show Block Highlights");
      highlightsOff = true;
      const linesBefore = (await render.lines.$calls()).length;
      const hudFirst = (await render.hud.$calls()).length;
      await prepareSelection(ctx, area);
      await ctx.client.screenshot("liteminer-highlights-off");
      expect((await render.lines.$calls()).length).toBe(linesBefore);
      expect((await render.hud.$calls()).length).toBeGreaterThan(hudFirst);

      await ctx.client.keyState(KEY_VEINMINE, false);
      await toggleConfigEntry(ctx, "Show Block Highlights");
      highlightsOff = false;
      await toggleConfigEntry(ctx, "Show HUD");
      hudOff = true;
      const hudBefore = (await render.hud.$calls()).length;
      await prepareSelection(ctx, area);
      await ctx.client.screenshot("liteminer-hud-off");
      expect((await render.hud.$calls()).length).toBe(hudBefore);
      expect((await render.lines.$calls()).length).toBeGreaterThan(linesBefore);
    } finally {
      await ctx.client.keyState(KEY_VEINMINE, false);
      await render.detach();
      if (highlightsOff) await toggleConfigEntry(ctx, "Show Block Highlights");
      if (hudOff) await toggleConfigEntry(ctx, "Show HUD");
      await cleanup(ctx, area, { x: 12, y: 70, z: 0 }, 12);
    }
  });

  test("scales the HUD text", { tags: ["hud"] }, async (ctx) => {
    const area = box({ x: 8, y: 69, z: 0 }, { x: 16, y: 72, z: 6 });
    // The HUD reads its scale from the config every frame it draws, so the values it read show what it drew with.
    const reads = await ctx.spy.method("hud.scale.reads", "com.iamkaf.konfig.impl.v1.config.model.ConfigValueImpl#get");
    let scaled = false;
    let seen = 0;
    const scalesSince = async () => {
      const calls = await reads.$calls();
      const values = calls.slice(seen).map((call) => call.returned);
      seen = calls.length;
      return values;
    };
    try {
      await setConfigSlider(ctx, "HUD Scale", 1);
      scaled = true;
      await scalesSince();
      await prepareSelection(ctx, area);
      await ctx.client.screenshot("liteminer-hud-scale-large");
      const large = await scalesSince();
      expect(large.includes(2)).toBe(true);
      expect(large.includes(0.5)).toBe(false);

      await ctx.client.keyState(KEY_VEINMINE, false);
      await setConfigSlider(ctx, "HUD Scale", 0);
      await scalesSince();
      await prepareSelection(ctx, area);
      await ctx.client.screenshot("liteminer-hud-scale-small");
      const small = await scalesSince();
      expect(small.includes(0.5)).toBe(true);
      expect(small.includes(2)).toBe(false);
    } finally {
      await ctx.client.keyState(KEY_VEINMINE, false);
      await reads.$detach();
      if (scaled) await setConfigSlider(ctx, "HUD Scale", 1 / 3);
      await cleanup(ctx, area, { x: 12, y: 70, z: 0 }, 12);
    }
  });

  test("draws highlight lines in the configured colors", { tags: ["highlights"] }, async (ctx) => {
    const area = box({ x: 8, y: 69, z: 0 }, { x: 16, y: 72, z: 6 });
    const render = await watchRendering(ctx);
    let changed = false;
    try {
      // Sliding red to its end turns the see-through default 169C9C into FF9C9C, and to zero turns F9FFFE into 00FFFE.
      await setColorChannel(ctx, "Highlight See-through Color", "Red", 1);
      await setColorChannel(ctx, "Highlight Foreground Color", "Red", 0);
      changed = true;
      await prepareSelection(ctx, area);
      await ctx.client.screenshot("liteminer-highlight-colors");
      const drawn = await render.lines.$calls();
      const last = drawn[drawn.length - 1];
      expect(drawn.length).toBeGreaterThan(0);
      expect(Number(last?.args?.[3]) | 0).toBe(0x4bff9c9c | 0);
      expect(Number(last?.args?.[4]) | 0).toBe(0xff00fffe | 0);
    } finally {
      await ctx.client.keyState(KEY_VEINMINE, false);
      await render.detach();
      if (changed) {
        await setColorChannel(ctx, "Highlight See-through Color", "Red", 0x16 / 255, 0x16);
        await setColorChannel(ctx, "Highlight Foreground Color", "Red", 0xf9 / 255, 0xf9);
      }
      await cleanup(ctx, area, { x: 12, y: 70, z: 0 }, 12);
    }
  });
});

async function prepareStripTest(ctx: TeaKitTestContext, area: Area, playerPos: Vec3, shape: number) {
  await ctx.client.closeMenus();
  await ctx.client.keyState(96, false);
  await setShape(ctx, shape);
  await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
  await ctx.world.clear(area.min, area.max);
  await ctx.world.fill(area.min, { ...area.max, y: area.min.y }, "minecraft:stone");
  await ctx.player.teleport(playerPos);
  await ctx.commands.assert("/item replace entity @s hotbar.0 with minecraft:netherite_axe");
  await ctx.player.inventory().selectHotbar(0);
}

async function holdVeinmineAndUse(ctx: TeaKitTestContext, target: BlockPos, lookTarget: Vec3) {
  await ctx.client.lookAt(lookTarget);
  await ctx.client.keyState(96, true);
  await ctx.runtime.wait(1_200);
  await ctx.player.useBlock(target, { face: "north" });
  await ctx.runtime.wait(500);
  await ctx.client.keyState(96, false);
}

async function heldDamage(ctx: TeaKitTestContext): Promise<number> {
  const { items } = await ctx.player.inventory();
  // The runtime reports the item as itemId, not the declared id.
  const axe = items.find((stack) => (stack["itemId"] ?? stack.id) === "minecraft:netherite_axe");
  expect(axe).toBeDefined();
  return Number(axe?.["damage"] ?? 0);
}

async function checkHighlightRendering(ctx: TeaKitTestContext, switchTransparency = false) {
  const area = box({ x: 8, y: 69, z: 0 }, { x: 16, y: 72, z: 6 });
  let transparencyToggled = false;
  try {
    await prepareCreativeTest(ctx, { x: 12, y: 70, z: 0 }, area, 12);
    await ctx.world.fill({ x: 8, y: 69, z: 0 }, { x: 16, y: 69, z: 6 }, "minecraft:stone");
    await setBlocks(ctx, [
      block(12, 70, 3),
      block(12, 70, 4),
      block(12, 70, 5),
      block(12, 71, 5),
    ]);
    await setShape(ctx, 0);
    await ctx.runtime.wait(500);
    await ctx.client.lookAt({ x: 12.5, y: 70.5, z: 3.5 });
    await ctx.client.keyState(96, true);
    await ctx.runtime.wait(1_600);
    await ctx.client.screenshot("liteminer-highlight-lines");

    if (switchTransparency) {
      // Two toggles exercise both modes and restore the original setting.
      for (let mode = 0; mode < 2; mode++) {
        await toggleImprovedTransparency(ctx);
        transparencyToggled = !transparencyToggled;
        await ctx.runtime.wait(1_600);
        expect((await ctx.client.waitForFrames(5)).ok).toBe(true);
        await ctx.client.screenshot(`liteminer-highlight-transparency-${mode}`);
      }
    }
  } finally {
    await ctx.client.keyState(96, false);
    if (transparencyToggled) await toggleImprovedTransparency(ctx);
    await cleanup(ctx, area, { x: 12, y: 70, z: 0 }, 12);
  }
}

async function toggleImprovedTransparency(ctx: TeaKitTestContext) {
  await ctx.client.keyState(292, true);
  try {
    await ctx.client.key(88);
  } finally {
    await ctx.client.keyState(292, false);
  }
}

async function prepareCreativeTest(ctx: TeaKitTestContext, playerPos: Vec3, area: Area, radius: number) {
  await ctx.client.closeMenus();
  await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
  await removeEntities(ctx, playerPos, radius, "minecraft:item");
  await ctx.world.clear(area.min, area.max);
  await ctx.world.setBlock({ x: Math.floor(playerPos.x), y: playerPos.y - 1, z: Math.floor(playerPos.z) }, "minecraft:stone");
  await ctx.player.teleport(playerPos);
  await ctx.player.give("minecraft:netherite_pickaxe");
  await ctx.player.inventory().selectHotbar(0);
}

/** The running Minecraft version as numbers, for the few commands and blocks that differ between versions. */
async function minecraftVersion(ctx: TeaKitTestContext): Promise<number[]> {
  const version = (await ctx.runtime.summary()).minecraftVersion ?? "";
  return version.split(".").map((part) => Number.parseInt(part, 10) || 0);
}

function atLeast(version: number[], ...minimum: number[]): boolean {
  for (let i = 0; i < Math.max(version.length, minimum.length); i += 1) {
    const difference = (version[i] ?? 0) - (minimum[i] ?? 0);
    if (difference !== 0) return difference > 0;
  }
  return true;
}

/** The block drops game rule was renamed in 1.21.11. */
async function blockDropsRule(ctx: TeaKitTestContext): Promise<string> {
  return atLeast(await minecraftVersion(ctx), 1, 21, 11) ? "block_drops" : "doTileDrops";
}

/**
 * Stops experience orbs and items from moving so a test can read where they landed. `/tick freeze` arrived in
 * 1.20.3. Before that, spectators attract no orbs, which is the part that matters here.
 */
async function freezeWorld(ctx: TeaKitTestContext) {
  if (atLeast(await minecraftVersion(ctx), 1, 20, 3)) await ctx.commands.assert("/tick freeze");
  else await ctx.commands.assert("/gamemode spectator @s");
}

async function unfreezeWorld(ctx: TeaKitTestContext) {
  if (atLeast(await minecraftVersion(ctx), 1, 20, 3)) await ctx.commands.assert("/tick unfreeze");
  else await ctx.commands.assert("/gamemode survival @s");
}

/** A diamond pickaxe with two durability left. Item data moved from NBT to components in 1.20.5. */
async function nearlyBrokenPickaxe(ctx: TeaKitTestContext): Promise<string> {
  return atLeast(await minecraftVersion(ctx), 1, 20, 5)
    ? "minecraft:diamond_pickaxe[damage=1559]"
    : "minecraft:diamond_pickaxe{Damage:1559}";
}

/** Short grass was called grass before 1.20.3. */
async function shortGrassId(ctx: TeaKitTestContext): Promise<BlockId> {
  return atLeast(await minecraftVersion(ctx), 1, 20, 3) ? "minecraft:short_grass" : "minecraft:grass";
}

/** Sculk always drops 1 XP but only exists since 1.19. Before that, diamond ore stands in. */
async function experienceBlockId(ctx: TeaKitTestContext): Promise<BlockId> {
  return atLeast(await minecraftVersion(ctx), 1, 19) ? "minecraft:sculk" : "minecraft:diamond_ore";
}

const SHAPE_COUNT = 5;
let selectedShape = 0;

/** Client commands reach the loader's handling only since 1.19; TeaKit sends older ones to the server as plain chat. */
async function supportsClientCommands(ctx: TeaKitTestContext): Promise<boolean> {
  return atLeast(await minecraftVersion(ctx), 1, 19);
}

/** Selects a shape. Where client commands are out of reach, cycles to it with the scroll wheel instead. */
async function setShape(ctx: TeaKitTestContext, index: number) {
  if (await supportsClientCommands(ctx)) {
    await ctx.client.command(`/liteminer shape set ${index}`);
    selectedShape = index;
    return;
  }
  const steps = (index - selectedShape + SHAPE_COUNT) % SHAPE_COUNT;
  if (steps === 0) return;
  await ctx.client.keyState(96, true);
  try {
    await ctx.runtime.wait(500);
    for (let step = 0; step < steps; step += 1) {
      await ctx.client.scroll({ x: 213.5, y: 120, verticalAmount: -1 });
      await ctx.runtime.wait(200);
    }
  } finally {
    await ctx.client.keyState(96, false);
  }
  selectedShape = index;
  await ctx.runtime.wait(300);
}

const HUNGER_ORIGIN = { x: 122, y: 70, z: 2 };
const HUNGER_SECONDARY = { x: 122, y: 70, z: 3 };
const HUNGER_LOOK = { x: 122.5, y: 70.5, z: 2.5 };

function hungerArea() {
  return box({ x: 118, y: 69, z: 0 }, { x: 126, y: 72, z: 5 });
}

async function prepareZeroHungerTest(ctx: TeaKitTestContext, area: Area) {
  await ctx.client.closeMenus();
  await ctx.client.keyState(96, false);
  await setShape(ctx, 0);
  await ctx.player.reset({
    gameMode: "survival",
    inventory: "clear",
    effects: "clear",
    food: 0,
    saturation: 0,
  });
  await ctx.world.clear(area.min, area.max);
  await ctx.world.fill({ x: 118, y: 69, z: 0 }, { x: 126, y: 69, z: 5 }, "minecraft:stone");
  await ctx.player.teleport({ x: 122.5, y: 70, z: 0.5 });
  await ctx.player.give("minecraft:netherite_pickaxe");
  await ctx.player.inventory().selectHotbar(0);
  await setBlocks(ctx, [
    block(HUNGER_ORIGIN.x, HUNGER_ORIGIN.y, HUNGER_ORIGIN.z),
    block(HUNGER_SECONDARY.x, HUNGER_SECONDARY.y, HUNGER_SECONDARY.z),
  ]);
}

async function prepareSurvivalXpTest(ctx: TeaKitTestContext, area: Area) {
  await ctx.client.closeMenus();
  await ctx.client.keyState(96, false);
  await ctx.runtime.wait(250);
  await setShape(ctx, 0);
  await ctx.runtime.wait(500);
  // Rebuild the floor before a survival player stands on it, or the player can fall out of the world.
  await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
  await ctx.world.clear(area.min, area.max);
  await ctx.world.fill({ x: 0, y: 69, z: 0 }, { x: 8, y: 69, z: 4 }, "minecraft:stone");
  await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
  await ctx.player.teleport({ x: 0, y: 70, z: 0 });
  await setExperience(ctx, 0);
  await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:item");
  await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:experience_orb");
}

const ORE_LINE_ORIGIN = { x: 100, y: 70, z: 3 };
const ORE_LINE_FAR_END = { x: 100.5, y: 70, z: 9.5 };

/** Vein mines a coal ore line that ends in deepslate coal ore, and returns where the deepslate drop landed. */
async function mineOreLineAndFindFarDrop(ctx: TeaKitTestContext): Promise<Vec3> {
  const area = box({ x: 98, y: 69, z: -2 }, { x: 102, y: 72, z: 11 });
  const origin = ORE_LINE_ORIGIN;
  let ticksFrozen = false;
  try {
    await ctx.client.closeMenus();
    await ctx.client.keyState(96, false);
    await setShape(ctx, 0);
    await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
    await ctx.player.teleport({ x: 100, y: 70, z: -1 });
    await removeEntities(ctx, origin, 16, "minecraft:item");
    await ctx.world.clear(area.min, area.max);
    await ctx.world.fill({ x: 98, y: 69, z: -2 }, { x: 102, y: 69, z: 11 }, "minecraft:stone");
    await ctx.player.give("minecraft:netherite_pickaxe");
    await ctx.player.inventory().selectHotbar(0);
    await ctx.commands.assert("/enchant @s minecraft:silk_touch 1");
    // A full inventory keeps the drops on the ground where they landed.
    await ctx.commands.batch([
      ...Array.from({ length: 8 }, (_, slot) => `/item replace entity @s hotbar.${slot + 1} with minecraft:stone 64`),
      ...Array.from({ length: 27 }, (_, slot) => `/item replace entity @s inventory.${slot} with minecraft:stone 64`),
    ], { requireSuccess: true });
    await ctx.world.setBlock(origin, "minecraft:coal_ore");
    for (let z = 4; z < 9; z += 1) {
      await ctx.world.setBlock({ x: 100, y: 70, z }, "minecraft:coal_ore");
    }
    await ctx.world.setBlock({ x: 100, y: 70, z: 9 }, "minecraft:deepslate_coal_ore");

    await ctx.client.lookAt({ x: 100.5, y: 70.5, z: 3.5 });
    await ctx.client.keyState(96, true);
    await ctx.runtime.wait(1_200);
    await ctx.player.mine(origin, { timeoutMs: 5_000 });
    await ctx.client.keyState(96, false);
    await ctx.runtime.wait(50);
    await freezeWorld(ctx);
    ticksFrozen = true;
    const drops = await ctx.entities
      .query({ origin, radius: 16, type: "minecraft:item", item: "minecraft:deepslate_coal_ore" })
      .waitForCountAtLeast(1, { timeout: 3_000 });
    const dropPosition = (await drops[0]?.inspect())?.position;
    if (!dropPosition) throw new Error("The deepslate drop had no reported position");
    await unfreezeWorld(ctx);
    ticksFrozen = false;
    await waitForAir(ctx, [origin, ...Array.from({ length: 6 }, (_, i) => ({ x: 100, y: 70, z: 4 + i }))]);
    return dropPosition;
  } finally {
    if (ticksFrozen) await unfreezeWorld(ctx);
    await ctx.client.keyState(96, false);
    await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
    await removeEntities(ctx, origin, 16, "minecraft:item");
    await removeEntities(ctx, origin, 16, "minecraft:experience_orb");
    await ctx.world.clear(area.min, area.max);
  }
}

/**
 * Vein mines 14 sculk blocks from x=1 to x=7, returns the x of each experience orb right after mining,
 * then collects the orbs and checks that every block's experience arrived.
 */
async function mineSculkAndCollectExperience(ctx: TeaKitTestContext): Promise<number[]> {
  const area = box({ x: 0, y: 69, z: 0 }, { x: 8, y: 73, z: 4 });
  const origin = { x: 2, y: 70, z: 2 };
  const sculk = [2, 3].flatMap((z) => Array.from({ length: 7 }, (_, x) => ({ x: x + 1, y: 70, z })));
  let ticksFrozen = false;
  try {
    await prepareSurvivalXpTest(ctx, area);
    await ctx.player.give("minecraft:netherite_pickaxe");
    await ctx.player.inventory().selectHotbar(0);
    const experienceBlock = await experienceBlockId(ctx);
    await setBlocks(ctx, sculk.map((pos) => block(pos.x, pos.y, pos.z, experienceBlock)));
    await ctx.client.lookAt({ x: 2.5, y: 70.5, z: 2.5 });
    await ctx.client.keyState(96, true);
    await ctx.runtime.wait(1_200);
    await ctx.player.mine(origin, { timeoutMs: 5_000 });
    await freezeWorld(ctx);
    ticksFrozen = true;
    await ctx.client.keyState(96, false);
    await waitForAir(ctx, sculk, 5_000);
    const orbs = await ctx.entities.query({ origin, radius: 12, type: "minecraft:experience_orb" }).list();
    // Frozen ticks still let the player absorb an orb between listing and inspecting it.
    const orbXs = (await Promise.all(orbs.map((orb) => orb.inspect().then((snapshot) => snapshot.position?.x, () => undefined))))
      .filter((x): x is number => x !== undefined);
    await unfreezeWorld(ctx);
    ticksFrozen = false;

    // Sculk always drops 1 XP, so 14 blocks reach level 1 while the origin alone cannot.
    await ctx.player.teleport({ x: 4, y: 70, z: 2 });
    // Pickup speed varies by line, so wait for the level instead of a fixed time.
    await expect(async () => (await ctx.commands.run("/execute if entity @s[level=1..]")).result).toEventuallyEqual(1, {
      timeout: 10_000,
      interval: 200,
    });
    return orbXs;
  } finally {
    if (ticksFrozen) await unfreezeWorld(ctx);
    await ctx.client.keyState(96, false);
    await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
    await setExperience(ctx, 0);
    await removeEntities(ctx, origin, 12, "minecraft:experience_orb");
    await ctx.world.clear(area.min, area.max);
  }
}

async function cleanup(ctx: TeaKitTestContext, area: Area, origin: BlockPos, radius: number) {
  await ctx.client.keyState(96, false);
  await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
  await removeEntities(ctx, origin, radius, "minecraft:item");
  await ctx.world.clear(area.min, area.max);
}

async function removeEntities(ctx: TeaKitTestContext, origin: BlockPos, radius: number, type: "minecraft:item" | "minecraft:experience_orb") {
  await ctx.entities.query({ origin, radius, type }).removeAll();
}

/** Counts an item across inventory slots. waitForItem also counts the selected slot a second time. */
async function countInInventory(ctx: TeaKitTestContext, item: ItemId): Promise<number> {
  const { items } = await ctx.player.inventory();
  // The runtime reports the item as itemId, not the declared id.
  return items
    .filter((stack) => (stack["itemId"] ?? stack.id) === item)
    .reduce((total, stack) => total + (stack.count ?? 0), 0);
}

async function setExperience(ctx: TeaKitTestContext, points: number) {
  await ctx.commands.assert("/xp set @s 0 levels");
  await ctx.commands.assert(`/xp set @s ${points} points`);
}

async function toggleRequireCorrectTool(ctx: TeaKitTestContext) {
  await toggleConfigEntry(ctx, "Require Correct Tool");
}

async function toggleConfigEntry(ctx: TeaKitTestContext, label: string) {
  await ctx.client.closeMenus();
  await ctx.client.command("/liteminer config");
  let screen = await ctx.client.waitForScreen("Liteminer Configuration", { timeoutMs: 10_000 });
  screen = await scrollToConfigEntry(ctx, label);
  const row = screen.lists("selection_list").entries().find((entry) => entry.label === label);
  if (!row) throw new Error(`Liteminer config entry disappeared: ${label}`);
  // Activating a Konfig row only selects it; the value button sits at the row's right edge.
  await ctx.client.click({ x: row.x + row.width - 30, y: row.y + row.height / 2 });
  await ctx.runtime.wait(300);
  await ctx.client.closeMenus();
  await ctx.runtime.wait(500);
}

/** Opens a two-option Konfig dropdown and moves to the other option. Konfig wraps the selection. */
async function cycleConfigDropdown(ctx: TeaKitTestContext, label: string) {
  await ctx.client.closeMenus();
  await ctx.client.command("/liteminer config");
  let screen = await ctx.client.waitForScreen("Liteminer Configuration", { timeoutMs: 10_000 });
  screen = await scrollToConfigEntry(ctx, label);
  const row = screen.lists("selection_list").entries().find((entry) => entry.label === label);
  if (!row) throw new Error(`Liteminer config entry disappeared: ${label}`);
  await ctx.client.click({ x: row.x + row.width - 30, y: row.y + row.height / 2 });
  await ctx.runtime.wait(300);
  await ctx.client.key(KEY_DOWN);
  await ctx.client.key(KEY_ENTER);
  await ctx.runtime.wait(300);
  await ctx.client.closeMenus();
  await ctx.runtime.wait(500);
}

const KEY_DOWN = 264;
const KEY_ENTER = 257;

async function scrollToConfigEntry(ctx: TeaKitTestContext, label: string) {
  for (let attempt = 0; attempt < 20; attempt += 1) {
    let screen = await ctx.client.screen();
    if (screen.lists().entries().some((entry) => entry.label === label)) return screen;
    screen = await screen.scroll({ vertical: -4 });
    await ctx.runtime.wait(150);
  }
  throw new Error(`Timed out scrolling to Liteminer config entry: ${label}`);
}

async function holdVeinmineAndMine(ctx: TeaKitTestContext, target: BlockPos, lookTarget: Vec3) {
  await ctx.client.closeMenus();
  await ctx.client.lookAt(lookTarget);
  await ctx.client.keyState(96, true);
  await ctx.runtime.wait(1_200);
  await ctx.player.mine(target, { timeoutMs: 5_000 });
  await ctx.runtime.wait(500);
  await ctx.client.keyState(96, false);
}

async function mine(ctx: TeaKitTestContext, target: BlockPos, lookTarget: Vec3) {
  await ctx.client.keyState(96, false);
  await ctx.runtime.wait(250);
  await ctx.client.lookAt(lookTarget);
  await ctx.client.keyState(96, true);
  await ctx.runtime.wait(1_200);
  await ctx.player.mine(target, { timeoutMs: 5_000 });
  await ctx.client.keyState(96, false);
}

async function mineWithClientAttack(ctx: TeaKitTestContext, target: BlockPos, lookTarget: Vec3) {
  await ctx.client.keyState(96, false);
  await ctx.runtime.wait(250);
  await ctx.client.lookAt(lookTarget);
  await ctx.client.keyState(96, true);
  await ctx.runtime.wait(1_200);
  await attackBlock(ctx, target);
  await ctx.client.keyState(96, false);
}

/** Holds the vein mining key and breaks a block by attacking it, for blocks that turn into something other than air. */
async function holdVeinmineAndAttack(ctx: TeaKitTestContext, target: BlockPos, lookTarget: Vec3, becomes: BlockId) {
  await ctx.client.closeMenus();
  await ctx.client.lookAt(lookTarget);
  await ctx.client.keyState(KEY_VEINMINE, true);
  await ctx.runtime.wait(1_200);
  await attackBlock(ctx, target, becomes);
  await ctx.runtime.wait(500);
  await ctx.client.keyState(KEY_VEINMINE, false);
}

async function attackBlock(ctx: TeaKitTestContext, target: BlockPos, becomes: BlockId = "minecraft:air") {
  await ctx.client.click({ x: 213.5, y: 120, button: 0, release: false });
  try {
    await expect(async () => (await ctx.world.block(target)).id).toEventuallyEqual(becomes, {
      timeout: 5_000,
      interval: 50,
    });
  } finally {
    await ctx.client.click({ x: 213.5, y: 120, button: 0, release: true });
  }
}

async function setBlocks(ctx: TeaKitTestContext, blocks: BlockPlacement[]) {
  for (const placement of blocks) {
    await ctx.world.setBlock(placement.pos, placement.id);
  }
}

async function assertBlocks(ctx: TeaKitTestContext, blocks: BlockPlacement[]) {
  for (const placement of blocks) {
    await assertBlock(ctx, placement.pos, placement.id);
  }
}

async function assertBlock(ctx: TeaKitTestContext, position: BlockPos, expected: BlockId) {
  expect(await ctx.world.block(position)).toHaveId(expected);
}

async function waitForAir(ctx: TeaKitTestContext, positions: BlockPos[], timeoutMs = 3_000) {
  for (const position of positions) {
    try {
      await expect(async () => (await ctx.world.block(position)).id).toEventuallyEqual("minecraft:air", {
        timeout: timeoutMs,
        interval: 50,
      });
    } catch (cause) {
      throw new Error(`Block ${position.x} ${position.y} ${position.z} did not become air`, { cause });
    }
  }
}

function block(x: number, y: number, z: number, id: BlockId = "minecraft:coal_ore"): BlockPlacement {
  return { pos: { x, y, z }, id };
}

function box(min: BlockPos, max: BlockPos): Area {
  return { min, max };
}

function cuboidPositions(min: BlockPos, max: BlockPos): BlockPos[] {
  const positions: BlockPos[] = [];
  for (let x = min.x; x <= max.x; x += 1) {
    for (let y = min.y; y <= max.y; y += 1) {
      for (let z = min.z; z <= max.z; z += 1) {
        positions.push({ x, y, z });
      }
    }
  }
  return positions;
}

interface Area {
  min: BlockPos;
  max: BlockPos;
}

interface BlockPlacement {
  pos: BlockPos;
  id: BlockId;
}

const KEY_VEINMINE = 96;

/** One real key press. TeaKit's own key() presses twice as far as key mappings are concerned, which toggles twice. */
async function tapVeinmineKey(ctx: TeaKitTestContext) {
  await ctx.client.keyState(KEY_VEINMINE, true);
  await ctx.runtime.wait(100);
  await ctx.client.keyState(KEY_VEINMINE, false);
}

function assertMatches(text: string, pattern: RegExp, what: string) {
  if (!pattern.test(text)) throw new Error(`Expected ${what} to match ${pattern}, got ${text.slice(0, 600)}`);
}

/** The newest confirmed request. Mining state is only applied once the server's reply is acknowledged. */
async function lastAppliedState(acks: SpyHandle): Promise<{ active?: boolean; shape?: string }> {
  const applied = (await acks.$calls()).filter((call) => call.returned === true);
  const reply = applied[applied.length - 1]?.args?.[0] as { active?: boolean; shape?: string } | undefined;
  if (!reply) throw new Error("The server confirmed no mining change");
  return reply;
}

async function expectMiningState(acks: SpyHandle, active: boolean) {
  const state = await lastAppliedState(acks);
  if (state.active !== active) {
    throw new Error(`Expected vein mining ${active ? "on" : "off"}, saw ${JSON.stringify((await acks.$calls()).map((call) => [call.args, call.returned]))}`);
  }
}

/** A survival player with a netherite pickaxe on a stone floor, built while still in creative so nothing falls. */
async function prepareSurvivalLine(ctx: TeaKitTestContext, area: Area, playerPos: Vec3) {
  await ctx.client.closeMenus();
  await ctx.client.keyState(KEY_VEINMINE, false);
  await setShape(ctx, 0);
  await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
  await removeEntities(ctx, playerPos, 12, "minecraft:item");
  await ctx.world.clear(area.min, area.max);
  await ctx.world.fill({ x: area.min.x, y: 69, z: area.min.z }, { x: area.max.x, y: 69, z: area.max.z }, "minecraft:stone");
  await ctx.player.reset({ gameMode: "survival", inventory: "clear", food: 20, saturation: 0 });
  await ctx.player.teleport(playerPos);
  await ctx.player.give("minecraft:netherite_pickaxe");
  await ctx.player.inventory().selectHotbar(0);
}

/** The class that draws highlight lines depends on how the Minecraft version renders them. */
async function highlightLineClass(ctx: TeaKitTestContext): Promise<string> {
  const version = await minecraftVersion(ctx);
  if (atLeast(version, 1, 21, 11)) return "ModernLines";
  if (atLeast(version, 1, 21, 5)) return "PipelineLines";
  return "ClassicLines";
}

interface RenderingSpies {
  lines: SpyHandle;
  /** The HUD's block count line, built once per frame while the HUD is drawn. */
  hud: SpyHandle;
  shape: SpyHandle;
  detach(): Promise<void>;
}

/** Watches what Liteminer hands to the renderer: outline lines, and HUD text lines with their x position. */
async function watchRendering(ctx: TeaKitTestContext): Promise<RenderingSpies> {
  const lines = await ctx.spy.method("rendering.lines", `com.iamkaf.liteminer.compat.${await highlightLineClass(ctx)}#draw`);
  const hud = await ctx.spy.method("rendering.hud", "com.iamkaf.liteminer.rendering.HUD#statusLine");
  const shape = await ctx.spy.method("rendering.shape", "com.iamkaf.liteminer.api.shape.LiteminerShape#displayName");
  return {
    lines,
    hud,
    shape,
    detach: async () => {
      await lines.$detach();
      await hud.$detach();
      await shape.$detach();
    },
  };
}

/** Four coal ore blocks in an L, selected by holding the key while looking at the end of the line. */
async function prepareSelection(ctx: TeaKitTestContext, area: Area) {
  await prepareCreativeTest(ctx, { x: 12, y: 70, z: 0 }, area, 12);
  await ctx.world.fill({ x: 8, y: 69, z: 0 }, { x: 16, y: 69, z: 6 }, "minecraft:stone");
  await setBlocks(ctx, [block(12, 70, 3), block(12, 70, 4), block(12, 70, 5), block(12, 71, 5)]);
  await setShape(ctx, 0);
  await ctx.runtime.wait(500);
  await ctx.client.lookAt({ x: 12.5, y: 70.5, z: 3.5 });
  await ctx.client.keyState(KEY_VEINMINE, true);
  await ctx.runtime.wait(1_600);
}

async function openConfigRow(ctx: TeaKitTestContext, label: string) {
  await ctx.client.closeMenus();
  await ctx.client.command("/liteminer config");
  await ctx.client.waitForScreen("Liteminer Configuration", { timeoutMs: 10_000 });
  const screen = await scrollToConfigEntry(ctx, label);
  const row = screen.lists("selection_list").entries().find((entry) => entry.label === label);
  if (!row) throw new Error(`Liteminer config entry disappeared: ${label}`);
  return row;
}

/** Konfig right-aligns a control min(200, max(132, width / 2)) wide, with a 4 pixel slider margin. */
function sliderPoint(row: { x: number; y: number; width: number; height: number }, fraction: number) {
  const controlWidth = Math.min(200, Math.max(132, Math.floor(row.width / 2)));
  const left = row.x + row.width - controlWidth;
  return { x: Math.round(left + 4 + (controlWidth - 8) * fraction), y: Math.round(row.y + row.height / 2) };
}

/** Clicks a Konfig slider at a fraction of its length, then presses arrow keys for fine steps. */
async function setConfigSlider(ctx: TeaKitTestContext, label: string, fraction: number, arrows: number[] = []) {
  const row = await openConfigRow(ctx, label);
  await ctx.client.click(sliderPoint(row, fraction));
  await ctx.runtime.wait(300);
  for (const arrow of arrows) {
    await ctx.client.key(arrow, { release: true });
    await ctx.runtime.wait(150);
  }
  await ctx.client.closeMenus();
  await ctx.runtime.wait(500);
}

/**
 * Sets an integer slider exactly. A click lands near the target, one arrow press reveals where, and arrow presses
 * walk the rest of the way. Konfig's own Reset only returns to the values the screen opened with, and a right
 * click never reaches the slider, so this is how a test puts a setting back.
 */
async function setConfigInt(ctx: TeaKitTestContext, label: string, target: number, guess: number) {
  const steps = await ctx.spy.method("config.steps", "com.iamkaf.konfig.impl.v1.client.field.KonfigFieldValues#stepInt");
  const current = async () => {
    const calls = await steps.$calls();
    return Number(calls[calls.length - 1]?.returned);
  };
  try {
    const row = await openConfigRow(ctx, label);
    await ctx.client.click(sliderPoint(row, guess));
    await ctx.runtime.wait(300);
    await ctx.client.key(KEY_RIGHT, { release: true });
    await ctx.runtime.wait(150);
    let value = await current();
    for (let attempt = 0; value !== target; attempt += 1) {
      if (attempt > 40 || Number.isNaN(value)) throw new Error(`Could not set ${label} to ${target}, stuck at ${value}`);
      await ctx.client.key(target > value ? KEY_RIGHT : KEY_LEFT, { release: true });
      await ctx.runtime.wait(100);
      value = await current();
    }
  } finally {
    await steps.$detach();
    await ctx.client.closeMenus();
    await ctx.runtime.wait(500);
  }
}

const KEY_RIGHT = 262;
const KEY_LEFT = 263;

/** Opens a color row's editor and moves one channel slider to its start or end. */
async function setColorChannel(ctx: TeaKitTestContext, label: string, channel: string, fraction: number, exact?: number) {
  const row = await openConfigRow(ctx, label);
  await ctx.client.click({ x: row.x + row.width - 30, y: row.y + row.height / 2 });
  await ctx.runtime.wait(500);
  const slider = (await ctx.client.screen()).widgets().all().find((widget) => widget.label.startsWith(`${channel}:`));
  if (!slider) throw new Error(`The color editor for ${label} has no ${channel} slider`);
  await ctx.client.click({ x: Math.round(slider.x + 4 + (slider.width - 8) * fraction), y: slider.y + slider.height / 2 });
  await ctx.runtime.wait(300);
  // The slider names its value, so arrow presses can walk an inexact click to the exact channel.
  for (let attempt = 0; exact !== undefined && attempt < 12; attempt += 1) {
    const value = Number(/(\d+)$/.exec((await ctx.client.screen()).widgets().all().find((widget) => widget.label.startsWith(`${channel}:`))?.label ?? "")?.[1]);
    if (value === exact) break;
    await ctx.client.key(value < exact ? KEY_RIGHT : KEY_LEFT, { release: true });
    await ctx.runtime.wait(150);
  }
  await ctx.client.screen().then((screen) => screen.widgets().activate("Done"));
  await ctx.runtime.wait(300);
  await ctx.client.closeMenus();
  await ctx.runtime.wait(500);
}

