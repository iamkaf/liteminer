import { Capability, Readiness, describe, expect, test } from "@teakit/test";

describe.configure({
  timeout: "2m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [Capability.ClientInput, Capability.ClientScreenshot, Capability.SpyInstrumentation,
    Capability.RuntimeTiming, Capability.ServerCommands],
});

test("confirms mining state and renders local Doctor", async ({ client, spy, runtime }) => {
  const acknowledgments = await spy.method("mining.confirmations", "com.iamkaf.liteminer.networking.ClientMiningState#acknowledge");
  const reports = await spy.method("doctor.reports", "com.iamkaf.amber.doctor.DoctorReports#render");
  try {
    await client.closeMenus();
    await runtime.wait(500);
    await client.keyState(96, true);
    await runtime.wait(500);
    const replies = await acknowledgments.$calls();
    const applied = replies.filter(reply => reply.returned === true);
    expect(applied.length).toBeGreaterThan(0);
    expect(JSON.stringify(applied.at(-1)?.args)).toContain('"active":true');
    expect(JSON.stringify(applied.at(-1)?.args)).toContain('"result":"APPLIED"');
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
  }
});
