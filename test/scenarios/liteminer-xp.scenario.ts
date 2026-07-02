import { Capability, describe, expect, test } from "@teakit/test";
import type { ScenarioResult } from "@teakit/test";

describe.configure({
  capabilities: [Capability.LegacyJsonScenarios],
  timeout: "90s",
});

describe("Liteminer XP drops", () => {
  test(
    "respects Silk Touch for secondary NeoForge ore XP",
    async ({ scenario }) => {
      const result = await scenario.run({
        name: "liteminer-veinmine-silk-touch-xp",
        steps: [
          {
            action: "run_scenario",
            scenario: "veinmine-silk-touch-xp.json",
          },
        ],
      }, {
        timeoutMs: 90000,
      });

      expect(failedSteps(result)).toEqual([]);
    },
  );
});

function failedSteps(result: ScenarioResult): string[] {
  return ["setup", "steps", "cleanup"].flatMap((phase) => {
    const phaseResults = result[phase];
    if (!Array.isArray(phaseResults)) {
      return [];
    }

    return phaseResults
      .filter((step) => {
        const stepResult = step.result as Record<string, unknown> | undefined;
        return stepResult?.failure != null || stepResult?.failed === true || stepResult?.success === false;
      })
      .map((step) => `${phase}[${step.index ?? "?"}] ${step.action ?? "unknown"}`);
  });
}
