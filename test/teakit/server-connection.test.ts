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

  test("leaves blocks in an Open Parties and Claims claim", { target: { mods: "openpartiesandclaims" } }, async (ctx) => {
    // The chunk border sits between x 111 and x 112, and the chunk from x 112 is claimed.
    const wilderness = [110, 111].map((x) => ({ x, y: 200, z: 102 }));
    const claimed = [112, 113].map((x) => ({ x, y: 200, z: 102 }));
    let claimMade = false;
    // OPAC renamed /openpac-claims to /oclaims. The older builds some versions still use have the old name.
    const claims = (await ctx.commands.run("/help oclaims")).success ? "/oclaims" : "/openpac-claims";
    try {
      await ctx.commands.batch(["/gamemode creative @s", "/forceload add 106 96 118 106"]);
      await ctx.runtime.wait(500);
      await ctx.world.fill({ x: 106, y: 199, z: 96 }, { x: 118, y: 199, z: 106 }, "minecraft:stone");
      await ctx.world.clear({ x: 106, y: 200, z: 96 }, { x: 118, y: 204, z: 106 });
      // OPAC only lets operators make server claims, and only the server's own command source can op the player.
      // A singleplayer host already is one, so this can fail there.
      await ctx.server.command("/op @a");
      // server-claim-mode toggles, so the batch turns server claiming on for the claim and back off.
      await ctx.commands.batch(["/tp @s 116.5 200 100.5", `${claims} server-claim-mode`, `${claims} claim`, `${claims} server-claim-mode`], { requireSuccess: true });
      claimMade = true;
      await ctx.runtime.wait(1_000);
      // On a dedicated server, operators may still mine in server claims, so the player mines as a regular player.
      await ctx.server.command("/deop @a");
      for (const pos of [...wilderness, ...claimed]) await ctx.world.setBlock(pos, "minecraft:coal_ore");
      await ctx.commands.batch([
        "/tp @s 110.5 200 100.5 0 0",
        "/item replace entity @s weapon.mainhand with minecraft:netherite_pickaxe",
      ], { requireSuccess: true });
      await ctx.client.closeMenus();
      await ctx.client.lookAt({ x: 110.5, y: 200.5, z: 102.5 });
      await ctx.client.keyState(96, true);
      // The key press waits for the server to confirm before it counts as vein mining.
      await ctx.runtime.wait(1_600);

      await ctx.client.click({ x: 213.5, y: 120, button: 0, release: false });
      try {
        for (const pos of wilderness) {
          await expect(async () => (await ctx.world.block(pos)).id).toEventuallyEqual("minecraft:air", {
            timeout: 5_000,
            interval: 50,
          });
        }
      } finally {
        await ctx.client.click({ x: 213.5, y: 120, button: 0, release: true });
      }
      await ctx.runtime.wait(500);
      for (const pos of claimed) expect((await ctx.world.block(pos)).id).toBe("minecraft:coal_ore");
    } finally {
      await ctx.client.keyState(96, false);
      if (claimMade) {
        await ctx.server.command("/op @a");
        await ctx.commands.batch(["/tp @s 116.5 200 100.5", `${claims} server-claim-mode`, `${claims} unclaim`, `${claims} server-claim-mode`], { requireSuccess: true });
      }
      await ctx.server.command("/deop @a");
      await ctx.world.clear({ x: 106, y: 200, z: 96 }, { x: 118, y: 204, z: 106 });
    }
  });
});
