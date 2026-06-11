import { Capability, describe, expect, test } from "@teakit/test";
import type { ScenarioResult } from "@teakit/test";

const legacyScenarios = [
  "veinmine-basic.json",
  "veinmine-shapes.json",
  "veinmine-staircase-direction.json",
  "veinmine-staircase-invalid-lower-block.json",
  "veinmine-staircase-limit.json",
] as const;

describe("Liteminer legacy scenario suite", () => {
  // FIXME: replace that mess with proper tests.
  test.each(legacyScenarios)(
    "%s",
    async (scenarioFile, { scenario }) => {
      const result = await scenario.run({
        name: `liteminer-${scenarioFile.replace(/\.json$/, "")}`,
        steps: [
          {
            action: "run_scenario",
            scenario: scenarioFile,
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
