import { Capability, describe, expect, test } from "@teakit/test";
import type { ScenarioResult } from "@teakit/test";

describe("Liteminer visual overlays", () => {
  test(
    "renders highlight lines while selecting a vein",
    async ({ scenario }) => {
      const result = await scenario.run({
        name: "liteminer-veinmine-highlight-visual",
        steps: [
          {
            action: "run_scenario",
            scenario: "veinmine-highlight-visual.json",
          },
        ],
      });

      expect(failedSteps(result)).toEqual([]);
    },
    { capabilities: [Capability.LegacyJsonScenarios] },
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
