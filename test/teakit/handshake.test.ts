import { Capability, Readiness, describe, expect, test } from "@teakit/test";

describe.configure({
  timeout: "2m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [Capability.ClientInput, Capability.ClientScreen, Capability.ClientScreens, Capability.ClientScreenshot, Capability.SpyInstrumentation,
    Capability.RuntimeTiming, Capability.ServerCommands],
});

test("confirms mining state and renders local Doctor", async ({ client, commands, spy, runtime }) => {
  const acknowledgments = await spy.method("mining.confirmations", "com.iamkaf.liteminer.networking.ClientMiningState#acknowledge");
  const reports = await spy.method("doctor.reports", "com.iamkaf.amber.doctor.DoctorReports#render");
  const colors = await spy.method("mining.colors", "com.iamkaf.liteminer.networking.ClientHandshake#highlightColor");
  try {
    await commands.batch(["/gamemode creative @s", "/forceload add 96 96 104 106"]);
    await runtime.wait(500);
    await commands.batch([
      "/fill 96 199 96 104 199 106 minecraft:stone",
      "/fill 96 200 96 104 204 106 minecraft:air",
      "/fill 100 200 102 100 200 104 minecraft:coal_ore",
      "/spawnpoint @s 100 200 100",
    ]);
    await commands.assert("/execute if block 100 199 100 minecraft:stone if block 100 200 100 minecraft:air if block 100 201 100 minecraft:air");
    if ((await client.screen()).title === "You Died!") {
      await runtime.wait(1_500);
      await (await client.screen()).widgets().find("Respawn").activate();
      await client.waitForWorld({ timeoutMs: 10_000 });
    }
    await client.closeMenus();
    await commands.batch([
      "/tp @s 100.5 200 100.5 0 29",
      "/item replace entity @s weapon.mainhand with minecraft:netherite_pickaxe",
    ], { requireSuccess: true });
    await client.command("/liteminer shape set 0");
    await runtime.wait(1_600);
    await client.keyState(96, true);
    await runtime.wait(500);
    const replies = await acknowledgments.$calls();
    const applied = replies.filter(reply => reply.returned === true);
    expect(applied.length).toBeGreaterThan(0);
    expect(JSON.stringify(applied.at(-1)?.args)).toContain('"active":true');
    expect(JSON.stringify(applied.at(-1)?.args)).toContain('"result":"APPLIED"');
    await client.waitForFrames(10);
    await client.screenshot("liteminer-confirmed-preview", { hideOverlay: true });
    const renderedColors = await colors.$calls();
    expect(renderedColors.length).toBeGreaterThan(0);
    expect(renderedColors.every(call => call.returned === call.args?.[0])).toBe(true);
    await client.command("/amber doctor");
    await runtime.wait(300);
    const rendered = await reports.$calls();
    expect(rendered.length).toBe(1);
    const output = JSON.stringify(rendered[0]?.args?.[0]);
    expect(output).toContain("id=liteminer");
    expect(output).toContain("liteminer.connection.healthy");
    await client.keyState(96, false);
    await client.key(84);
    await client.waitForFrames(4);
    await client.screenshot("liteminer-doctor-healthy", { hideOverlay: true });
    await client.key(256);
  } finally {
    await client.keyState(96, false);
    await acknowledgments.$detach();
    await reports.$detach();
    await colors.$detach();
  }
});
