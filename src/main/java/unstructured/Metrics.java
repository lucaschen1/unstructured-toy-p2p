package unstructured;

import peersim.core.CommonState;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Metrics {
    private Metrics() {}

    private static final Map<Long, Long> startCycle = new HashMap<>();
    private static final Map<Long, Long> endCycle = new HashMap<>();
    private static final Set<Long> completed = new HashSet<>();

    public static void recordStart(long qid) {
        startCycle.put(qid, (long) CommonState.getIntTime());
    }

    public static void recordCompletionIfFirst(long qid) {
        if (completed.add(qid)) {
            endCycle.put(qid, (long) CommonState.getIntTime());
        }
    }

    public static int completedCount() {
        return completed.size();
    }

    public static double averageLatencyCycles() {
        if (completed.isEmpty()) return 0.0;
        long sum = 0;
        for (long qid : completed) {
            Long s = startCycle.get(qid);
            Long e = endCycle.get(qid);
            if (s != null && e != null) {
                sum += (e - s);
            }
        }
        return sum / (double) completed.size();
    }
}
