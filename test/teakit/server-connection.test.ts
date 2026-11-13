import { Capability, Readiness, describe, expect, test } from "@teakit/test";

// These steps use client input and server commands only, so they run the same on an integrated server and
// across a connection to a dedicated one.
describe.configure({
  timeout: "2m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [
    Capability.ClientInput,
    Capability.ServerCommands,
    Capability.WorldBlock,
    Capability.WorldClear,
    Capability.WorldFill,
    Capability.WorldSetBlock,
  ],
});

describe("Liteminer over a server connection", () => {
  test("vein mines a line of ore broken with the client", async (ctx) => {
    const ore = [102, 103, 104].map((z) => ({ x: 100, y: 200, z }));
    try {
      await ctx.commands.batch(["/gamemode creative @s", "/forceload add 96 96 104 106"]);
      await ctx.runtime.wait(500);
      await ctx.world.fill({ x: 96, y: 199, z: 96 }, { x: 104, y: 199, z: 106 }, "minecraft:stone");
      await ctx.world.clear({ x: 96, y: 200, z: 96 }, { x: 104, y: 204, z: 106 });
      for (const pos of ore) await ctx.world.setBlock(pos, "minecraft:coal_ore");
      await ctx.commands.batch([
        "/tp @s 100.5 200 100.5 0 0",
        "/item replace entity @s weapon.mainhand with minecraft:netherite_pickaxe",
      ], { requireSuccess: true });
      await ctx.client.closeMenus();
      await ctx.client.lookAt({ x: 100.5, y: 200.5, z: 102.5 });
      await ctx.client.keyState(96, true);
      // The key press waits for the server to confirm before it counts as vein mining.
      await ctx.runtime.wait(1_600);

      await ctx.client.click({ x: 213.5, y: 120, button: 0, release: false });
      try {
        for (const pos of ore) {
          await expect(async () => (await ctx.world.block(pos)).id).toEventuallyEqual("minecraft:air", {
            timeout: 5_000,
            interval: 50,
          });
        }
      } finally {
        await ctx.client.click({ x: 213.5, y: 120, button: 0, release: true });
      }
    } finally {
      await ctx.client.keyState(96, false);
      await ctx.world.clear({ x: 96, y: 200, z: 96 }, { x: 104, y: 204, z: 106 });
    }
  });
});
