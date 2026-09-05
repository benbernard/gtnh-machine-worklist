package com.benbernard.machineworklist;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.List;

/** Exact matching for overlapping ingredient alternatives; every unit of stock is allocated at most once. */
public final class BatchReadiness {

    private BatchReadiness() {}

    public static final class Ingredient {

        public final long perRun;
        public final boolean reusable;
        public final int[] matchingSlots;

        public Ingredient(long perRun, boolean reusable, int... matchingSlots) {
            if (perRun <= 0) throw new IllegalArgumentException("Ingredient amount must be positive");
            this.perRun = perRun;
            this.reusable = reusable;
            this.matchingSlots = matchingSlots.clone();
        }
    }

    public static long maximumRuns(long requested, long[] stock, List<Ingredient> ingredients) {
        if (requested < 0) throw new IllegalArgumentException("Negative run count");
        for (long amount : stock) if (amount < 0) throw new IllegalArgumentException("Negative stock");
        long low = 0;
        long high = requested;
        while (low < high) {
            long middle = low + (high - low) / 2 + (high - low) % 2;
            if (canRun(middle, stock, ingredients)) low = middle;
            else high = middle - 1;
        }
        return low;
    }

    private static boolean canRun(long runs, long[] stock, List<Ingredient> ingredients) {
        int sink = 1 + stock.length + ingredients.size();
        long[][] residual = new long[sink + 1][sink + 1];
        for (int slot = 0; slot < stock.length; slot++) residual[0][slot + 1] = stock[slot];
        for (int index = 0; index < ingredients.size(); index++) {
            Ingredient ingredient = ingredients.get(index);
            int node = stock.length + 1 + index;
            long demand;
            try {
                demand = Math.multiplyExact(ingredient.perRun, ingredient.reusable ? 1 : runs);
            } catch (ArithmeticException overflow) {
                return false;
            }
            residual[node][sink] = demand;
            for (int slot : ingredient.matchingSlots) {
                if (slot < 0 || slot >= stock.length) throw new IllegalArgumentException("Invalid matching slot");
                residual[slot + 1][node] = demand;
            }
        }
        while (true) {
            int[] parent = new int[sink + 1];
            Arrays.fill(parent, -1);
            parent[0] = 0;
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(0);
            while (!queue.isEmpty() && parent[sink] < 0) {
                int from = queue.removeFirst();
                for (int to = 1; to <= sink; to++) {
                    if (parent[to] < 0 && residual[from][to] > 0) {
                        parent[to] = from;
                        queue.addLast(to);
                    }
                }
            }
            if (parent[sink] < 0) break;
            long amount = Long.MAX_VALUE;
            for (int node = sink; node != 0; node = parent[node])
                amount = Math.min(amount, residual[parent[node]][node]);
            for (int node = sink; node != 0; node = parent[node]) {
                residual[parent[node]][node] -= amount;
                residual[node][parent[node]] += amount;
            }
        }
        for (int index = 0; index < ingredients.size(); index++) {
            if (residual[stock.length + 1 + index][sink] != 0) return false;
        }
        return true;
    }
}
