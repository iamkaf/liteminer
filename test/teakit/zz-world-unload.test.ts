import { Capability, Readiness, describe, expect, test } from "@teakit/test";

describe.configure({
  timeout: "2m",
  readiness: [Readiness.World, Readiness.Player],
  capabilities: [Capability.ClientScreens],
});

// TeaKit runs top-level tests before suites, so keep world teardown in a final suite.
describe("World exit", () => {
  test("leaves a world without crashing", async ({ client }) => {
    const result = await client.leaveWorld();
    expect(result.hadLevel).toBe(true);
    await new Promise(resolve => setTimeout(resolve, 3_000));
  });
});
