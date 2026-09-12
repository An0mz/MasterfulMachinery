package io.ticticboom.mods.mm.port.energy;

import net.minecraft.core.BlockPos;

import java.util.*;

public class EnergyPortRouting {
    public static long distributeFillThrough(long requestedAmount, EnergyPortStorage source, Map<BlockPos, EnergyPortStorage> candidates) {
        if (requestedAmount <= 0) return 0;
        long remaining = requestedAmount;

        Map<Integer, List<Map.Entry<BlockPos, EnergyPortStorage>>> grouped = new HashMap<>();
        for (var entry : candidates.entrySet()) {
            var storage = entry.getValue();
            long canReceive = storage.internalInsert(remaining, true);
            if (canReceive <= 0) continue;
            grouped.computeIfAbsent(storage.getPriority(), k -> new ArrayList<>()).add(entry);
        }

        List<Integer> priorities = new ArrayList<>(grouped.keySet());
        priorities.sort(Comparator.reverseOrder());

        for (int prio : priorities) {
            var list = grouped.get(prio);
            long remForSort = remaining;
            list.sort((a, b) -> {
                long sa = a.getValue().internalInsert(remForSort, true);
                long sb = b.getValue().internalInsert(remForSort, true);
                if (sa != sb) return Long.compare(sb, sa);
                return Long.compare(a.getKey().asLong(), b.getKey().asLong());
            });

            for (var entry : list) {
                if (remaining <= 0) break;
                var target = entry.getValue();
                long canAccept = target.internalInsert(remaining, true);
                if (canAccept <= 0) continue;
                long accepted = target.internalInsert(canAccept, false);
                if (accepted > 0) {
                    source.internalExtract(accepted, false);
                    remaining -= accepted;
                }
            }
            if (remaining <= 0) break;
        }

        return requestedAmount - remaining;
    }
}
