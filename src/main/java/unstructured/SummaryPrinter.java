package unstructured;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.CommonState;

public class SummaryPrinter implements Control {

    private static final String PAR_EXPECTED = "expectedQueries";
    private final int expected;

    public SummaryPrinter(String prefix) {
        this.expected = Configuration.getInt(prefix + "." + PAR_EXPECTED, 0);
    }

    @Override
    public boolean execute() {
        int done = Metrics.completedCount();
        double avgLat = Metrics.averageLatencyCycles();
        int t = CommonState.getIntTime();

        double throughputPerCycle = (t > 0) ? (done / (double) t) : 0.0;

        System.out.println("=== SUMMARY ===");
        System.out.println("time_cycles=" + t);
        System.out.println("completed_queries=" + done + (expected > 0 ? ("/" + expected) : ""));
        System.out.println("avg_latency_cycles=" + avgLat);
        System.out.println("throughput_queries_per_cycle=" + throughputPerCycle);
        System.out.println("===============");
        return false;
    }
}
