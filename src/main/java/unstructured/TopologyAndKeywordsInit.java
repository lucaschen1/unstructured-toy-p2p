package unstructured;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.util.*;

public class TopologyAndKeywordsInit implements Control {

    private static final String PAR_PID = "protocol";
    private static final String PAR_FILES = "filesPerPeer";

    private final int pid;
    private final int filesPerPeer;

    private static final String[] KEY_POOL = {
        "rock", "jazz", "hiphop", "classical", "blues",
        "metal", "pop", "edm", "indie", "reggae",
        "folk", "country", "punk", "soul", "techno"
    };

    public TopologyAndKeywordsInit(String prefix) {
        this.pid = Configuration.getPid(prefix + "." + PAR_PID);
        this.filesPerPeer = Configuration.getInt(prefix + "." + PAR_FILES, 5);
    }

    @Override
    public boolean execute() {
        int n = Network.size();
        if (n < 5) throw new IllegalStateException("Need at least 5 peers");

        // ---- Skewed topology ----
        // Hub-and-spoke: 0 connected to all others
        for (int i = 0; i < n; i++) {
            Node node = Network.get(i);
            UnstructuredFlooding p = (UnstructuredFlooding) node.getProtocol(pid);

            List<Integer> neigh = new ArrayList<>();
            if (i == 0) {
                for (int j = 1; j < n; j++) neigh.add(j);
            } else {
                neigh.add(0);
            }
            // extra edges
            if (i == 2 && n > 3) neigh.add(3);
            if (i == 3) {
                if (n > 2) neigh.add(2);
                if (n > 4) neigh.add(4);
            }
            if (i == 4 && n > 3) neigh.add(3);

            LinkedHashSet<Integer> uniq = new LinkedHashSet<>(neigh);
            uniq.remove(i);
            p.setNeighbors(uniq);

            // ---- Keywords per peer ----
            // Deterministic overlap: each peer gets a sliding window over KEY_POOL
            Set<String> keys = new LinkedHashSet<>();
            int start = (i * 3) % KEY_POOL.length;
            for (int k = 0; k < filesPerPeer; k++) {
                keys.add(KEY_POOL[(start + k) % KEY_POOL.length]);
            }
            p.setLocalKeywords(keys);
        }

        return false; // run once
    }
}
