package com.benbernard.machineworklist;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

/** Display ordering only; never feeds back into NEI's plan or crafting-chain scheduling. */
final class WorklistOrder {

    private WorklistOrder() {}

    static void sortMachineRows(List<WorklistPlan.Step> rows) {
        List<WorklistPlan.Step> machines = new ArrayList<>();
        for (WorklistPlan.Step step : rows) if (!step.crafting) machines.add(step);
        machines.sort(
            Comparator.comparing((WorklistPlan.Step step) -> step.readyRuns == 0)
                .thenComparing(
                    Comparator.comparingLong((WorklistPlan.Step step) -> step.runs)
                        .reversed())
                .thenComparing(step -> step.machine));
        // Keep crafting rows in their existing positions in All; replace only machine rows.
        int index = 0;
        for (ListIterator<WorklistPlan.Step> it = rows.listIterator(); it.hasNext();)
            if (!it.next().crafting) it.set(machines.get(index++));
    }
}
