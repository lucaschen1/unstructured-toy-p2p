package unstructured;

import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.cdsim.CDProtocol;

import java.util.*;

/**
 * Unstructured keyword search with flooding + TTL (cycle-based).
 *
 * Each node has:
 *  - a local set of keywords (representing its shared files)
 *  - a neighbor list (overlay)
 *  - inbox queues for messages
 *  - seen query IDs to avoid infinite forwarding
 */
public class UnstructuredFlooding implements CDProtocol, Cloneable {

    // ---------- Message types ----------
    public interface Msg {}

    public static final class Query implements Msg {
        public final long qid;
        public final int originIndex;
        public final String keyword;
        public int ttl;

        public Query(long qid, int originIndex, String keyword, int ttl) {
            this.qid = qid;
            this.originIndex = originIndex;
            this.keyword = keyword;
            this.ttl = ttl;
        }
    }

    public static final class Response implements Msg {
        public final long qid;
        public final int foundAtIndex;

        public Response(long qid, int foundAtIndex) {
            this.qid = qid;
            this.foundAtIndex = foundAtIndex;
        }
    }

    // ---------- Config ----------
    private static final String PAR_TTL = "ttl";
    private static final String PAR_KFWD = "kforward"; // 0 means "flood to all neighbors"

    private final int defaultTtl;
    private final int kForward;

    // ---------- Per-node state ----------
    private final Set<String> localKeywords = new HashSet<>();
    private final Set<Long> seenQids = new HashSet<>();
    private final List<Integer> neighbors = new ArrayList<>();

    private final ArrayDeque<Msg> inbox = new ArrayDeque<>();
    private final ArrayDeque<Msg> nextInbox = new ArrayDeque<>();

    public UnstructuredFlooding(String prefix) {
        this.defaultTtl = Configuration.getInt(prefix + "." + PAR_TTL, 5);
        this.kForward = Configuration.getInt(prefix + "." + PAR_KFWD, 0);
    }

    @Override
    public Object clone() {
        UnstructuredFlooding copy = new UnstructuredFlooding("protocol.search");
        return copy;
    }

    // ---------- Helpers ----------
    public void setLocalKeywords(Collection<String> keys) {
        localKeywords.clear();
        localKeywords.addAll(keys);
    }

    public void setNeighbors(Collection<Integer> neigh) {
        neighbors.clear();
        neighbors.addAll(neigh);
    }

    public void injectQueryFromDriver(String keyword, int originIndex) {
        long qid = CommonState.r.nextLong();
        Metrics.recordStart(qid);
        nextInbox.addLast(new Query(qid, originIndex, keyword, defaultTtl));
    }

    // ---------- Simulation step ----------
    @Override
    public void nextCycle(Node node, int pid) {
        // Move nextInbox -> inbox at the start of the cycle
        inbox.addAll(nextInbox);
        nextInbox.clear();

        while (!inbox.isEmpty()) {
            Msg m = inbox.removeFirst();

            if (m instanceof Query q) {
                handleQuery(node, pid, q);
            } else if (m instanceof Response r) {
                handleResponse(node, r);
            }
        }
    }

    private void handleQuery(Node node, int pid, Query q) {
        if (!seenQids.add(q.qid)) return;

        int myIndex = (int) node.getID();
        boolean hit = localKeywords.contains(q.keyword.toLowerCase(Locale.ROOT));

        if (hit) {
            // Deliver response to origin
            Node origin = Network.get(q.originIndex);
            UnstructuredFlooding p = (UnstructuredFlooding) origin.getProtocol(pid);
            p.nextInbox.addLast(new Response(q.qid, myIndex));
            return;
        }

        if (q.ttl <= 0) return;

        q.ttl -= 1;

        // Forward
        List<Integer> targets = chooseForwardTargets();
        for (int neiIndex : targets) {
            Node nei = Network.get(neiIndex);
            UnstructuredFlooding p = (UnstructuredFlooding) nei.getProtocol(pid);
            p.nextInbox.addLast(new Query(q.qid, q.originIndex, q.keyword, q.ttl));
        }
    }

    private void handleResponse(Node node, Response r) {
        // Only origin cares; if it receives multiple responses, count once.
        Metrics.recordCompletionIfFirst(r.qid);
    }

    private List<Integer> chooseForwardTargets() {
        if (neighbors.isEmpty()) return Collections.emptyList();
        if (kForward <= 0 || kForward >= neighbors.size()) return neighbors;

        // choose k random distinct neighbors
        ArrayList<Integer> shuffled = new ArrayList<>(neighbors);
        Collections.shuffle(shuffled, CommonState.r);
        return shuffled.subList(0, kForward);
    }
}
