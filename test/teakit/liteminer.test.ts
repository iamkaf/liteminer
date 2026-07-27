import {
  Capability,
  Readiness,
  describe,
  expect,
  test,
} from "@teakit/test";
import type { BlockId, BlockPos, TeaKitTestContext, Vec3 } from "@teakit/test";

describe.configure({
  timeout: "3m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ClientScreens,
    Capability.ClientScreenshot,
    Capability.PlayerInteractions,
    Capability.PlayerInventory,
    Capability.PlayerReset,
    Capability.RuntimeTiming,
    Capability.ServerCommands,
    Capability.WorldBlock,
    Capability.WorldClear,
    Capability.WorldEntities,
    Capability.WorldFill,
    Capability.WorldSetBlock,
  ],
});

describe("Liteminer vein mining", () => {
  test("mines a connected ore vein", async (ctx) => {
    const area = box({ x: 0, y: 69, z: 0 }, { x: 4, y: 73, z: 4 });
    try {
      await prepareCreativeTest(ctx, { x: 0, y: 70, z: 0 }, area, 8);
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

  test("renders highlight lines while selecting a vein", async (ctx) => {
    const area = box({ x: 8, y: 69, z: 0 }, { x: 16, y: 72, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 12, y: 70, z: 0 }, area, 12);
      await ctx.world.fill({ x: 8, y: 69, z: 0 }, { x: 16, y: 69, z: 6 }, "minecraft:stone");
      await setBlocks(ctx, [
        block(12, 70, 3),
        block(12, 70, 4),
        block(12, 70, 5),
        block(12, 71, 5),
      ]);
      await ctx.client.command("/liteminer shape set 0");
      await ctx.runtime.wait(500);
      await ctx.client.lookAt({ x: 12.5, y: 70.5, z: 3.5 });
      await ctx.client.keyState(96, true);
      await ctx.runtime.wait(1_600);
      await ctx.client.screenshot("liteminer-highlight-lines");
    } finally {
      await cleanup(ctx, area, { x: 12, y: 70, z: 0 }, 12);
    }
  });

  test("applies each configured mining shape", async (ctx) => {
    const area = box({ x: -4, y: 66, z: 0 }, { x: 44, y: 75, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 0, y: 70, z: 0 }, area, 24);
      await ctx.world.fill({ x: -4, y: 69, z: 0 }, { x: 44, y: 69, z: 6 }, "minecraft:stone");
      await ctx.client.command("/liteminer shape set 0");
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

      await ctx.client.command("/liteminer shape set 1");
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(10, 70, 2), block(10, 70, 3), block(10, 70, 4), block(11, 70, 3),
      ]);
      await ctx.player.teleport({ x: 10, y: 70, z: 0 });
      await mine(ctx, { x: 10, y: 70, z: 2 }, { x: 10.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 10, y: 70, z: 2 }, { x: 10, y: 70, z: 3 }, { x: 10, y: 70, z: 4 },
      ]);
      await assertBlock(ctx, { x: 11, y: 70, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-small-tunnel");

      await ctx.client.command("/liteminer shape set 2");
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(20, 70, 2), block(20, 71, 2), block(20, 72, 2),
        block(20, 71, 3), block(20, 72, 3), block(20, 73, 3),
        block(20, 72, 4), block(20, 73, 4), block(20, 74, 4), block(21, 72, 3),
      ]);
      await ctx.player.teleport({ x: 20, y: 70, z: 0 });
      await mine(ctx, { x: 20, y: 71, z: 2 }, { x: 20.5, y: 71.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 20, y: 70, z: 2 }, { x: 20, y: 71, z: 2 }, { x: 20, y: 72, z: 2 },
        { x: 20, y: 71, z: 3 }, { x: 20, y: 72, z: 3 }, { x: 20, y: 73, z: 3 },
        { x: 20, y: 72, z: 4 }, { x: 20, y: 73, z: 4 }, { x: 20, y: 74, z: 4 },
      ]);
      await assertBlock(ctx, { x: 21, y: 72, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-staircase-up");

      await ctx.client.command("/liteminer shape set 3");
      await ctx.runtime.wait(500);
      await setBlocks(ctx, [
        block(30, 71, 2), block(30, 70, 2), block(30, 69, 2),
        block(30, 70, 3), block(30, 69, 3), block(30, 68, 3),
        block(30, 69, 4), block(30, 68, 4), block(30, 67, 4), block(31, 69, 3),
      ]);
      await ctx.player.teleport({ x: 30, y: 70, z: 0 });
      await mine(ctx, { x: 30, y: 70, z: 2 }, { x: 30.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, [
        { x: 30, y: 70, z: 2 }, { x: 30, y: 69, z: 2 }, { x: 30, y: 68, z: 2 },
        { x: 30, y: 69, z: 3 }, { x: 30, y: 68, z: 3 }, { x: 30, y: 67, z: 3 },
        { x: 30, y: 68, z: 4 }, { x: 30, y: 67, z: 4 }, { x: 30, y: 66, z: 4 },
      ]);
      await assertBlock(ctx, { x: 31, y: 69, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-staircase-down");

      await ctx.client.command("/liteminer shape set 4");
      await ctx.runtime.wait(500);
      await ctx.world.fill({ x: 39, y: 69, z: 2 }, { x: 41, y: 71, z: 2 }, "minecraft:coal_ore");
      await ctx.world.setBlock({ x: 40, y: 70, z: 3 }, "minecraft:coal_ore");
      await ctx.player.teleport({ x: 40, y: 70, z: 0 });
      await mine(ctx, { x: 40, y: 70, z: 2 }, { x: 40.5, y: 70.5, z: 2.5 });
      await waitForAir(ctx, cuboidPositions({ x: 39, y: 69, z: 2 }, { x: 41, y: 71, z: 2 }));
      await assertBlock(ctx, { x: 40, y: 70, z: 3 }, "minecraft:coal_ore");
      await ctx.client.screenshot("liteminer-shape-3x3");
      await ctx.client.command("/liteminer shape set 0");
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

      await ctx.client.command("/liteminer shape set 2");
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

      await ctx.client.command("/liteminer shape set 3");
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
      await ctx.client.command("/liteminer shape set 2");
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

  test("limits a staircase selection to three layers", async (ctx) => {
    const area = box({ x: 86, y: 66, z: 0 }, { x: 96, y: 76, z: 6 });
    try {
      await prepareCreativeTest(ctx, { x: 90, y: 70, z: 0 }, area, 24);
      await ctx.world.fill({ x: 86, y: 69, z: 0 }, { x: 96, y: 69, z: 6 }, "minecraft:stone");
      await ctx.client.command("/liteminer shape set 2");
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
});

async function prepareCreativeTest(ctx: TeaKitTestContext, playerPos: BlockPos, area: Area, radius: number) {
  await ctx.client.closeMenus();
  await ctx.player.reset({ gameMode: "creative", inventory: "clear" });
  await ctx.player.teleport(playerPos);
  await removeEntities(ctx, playerPos, radius, "minecraft:item");
  await ctx.world.clear(area.min, area.max);
  await ctx.player.give("minecraft:netherite_pickaxe");
  await ctx.player.inventory().selectHotbar(0);
}

async function prepareSurvivalXpTest(ctx: TeaKitTestContext, area: Area) {
  await ctx.client.closeMenus();
  await ctx.client.keyState(96, false);
  await ctx.runtime.wait(250);
  await ctx.client.command("/liteminer shape set 0");
  await ctx.runtime.wait(500);
  await ctx.player.reset({ gameMode: "survival", inventory: "clear" });
  await ctx.player.teleport({ x: 0, y: 70, z: 0 });
  await setExperience(ctx, 0);
  await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:item");
  await removeEntities(ctx, { x: 2, y: 70, z: 2 }, 12, "minecraft:experience_orb");
  await ctx.world.clear(area.min, area.max);
  await ctx.world.fill({ x: 0, y: 69, z: 0 }, { x: 8, y: 69, z: 4 }, "minecraft:stone");
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

async function setExperience(ctx: TeaKitTestContext, points: number) {
  await ctx.commands.assert("/xp set @s 0 levels");
  await ctx.commands.assert(`/xp set @s ${points} points`);
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

async function attackBlock(ctx: TeaKitTestContext, target: BlockPos) {
  await ctx.client.click({ x: 213.5, y: 120, button: 0, release: false });
  try {
    await expect(async () => (await ctx.world.block(target)).id).toEventuallyEqual("minecraft:air", {
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
