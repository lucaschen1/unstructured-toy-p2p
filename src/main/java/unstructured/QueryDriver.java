package unstructured;

import peersim.config.Configuration;
import peersim.core.Control;
import peersim.core.Network;
import peersim.core.Node;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class QueryDriver implements Control {
    private static final String PAR_PID = "protocol";
    private static final String PAR_ORIGIN = "origin_index";
    private static final String PAR_FILE = "query_file";

    private final int pid;
    private final int originIndex;
    private final List<String> queries = new ArrayList<>();
    private int idx = 0;

    public QueryDriver(String prefix) {
        this.pid = Configuration.getPid(prefix + "." + PAR_PID);
        this.originIndex = Configuration.getInt(prefix + "." + PAR_ORIGIN, 0);

        String file = Configuration.getString(prefix + "." + PAR_FILE);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String q = line.trim();
                if (!q.isEmpty()) {
                    queries.add(q.toLowerCase());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to read query file: " + file, e);
        }
    }

    public boolean execute() {
        if (idx >= queries.size()) return false;

        Node origin = Network.get(originIndex);
        UnstructuredFlooding p = (UnstructuredFlooding) origin.getProtocol(pid);
        p.injectQueryFromDriver(queries.get(idx), originIndex);
        idx++;

        return false;
    }
}
