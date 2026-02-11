# Unstructured Toy P2P Search System

## Overview

This project implements a toy unstructured Peer-to-Peer (P2P) search system using the PeerSim simulator (link: https://sourceforge.net/projects/peersim/). The system models a keyword-based file sharing overlay network where peers issue search queries that are propagated using flooding-based routing with a configurable TTL and forwarding parameters.

The goals of this project are to:
- Evaluate search performance in an unstructured overlay
- Measure latency and throughput under different workloads
- Compare the default routing protocol with a proposed routing modification
- Analyze scalability and routing tradeoffs

---

## System Architecture

### Overlay Model
- Unstructured P2P overlay  
- Peers connected in a skewed hub-and-spoke topology 
- No centralized index  
- Each peer stores a local set of keyword-named files  

### Search Model

Search is performed using query flooding:
1. A peer generates a keyword query.
2. The query is forwarded to all neighbors except the sender.
3. Each forwarding decrements the TTL (hop counter).
4. When TTL reaches 0, propagation stops.
5. If a peer contains the requested file, it returns a hit.

---

## How to Run

Compile the Java Source
```
mkdir -p out
javac -d out -cp "peersim-1.0.5/peersim-1.0.5.jar:peersim-1.0.5/jep-2.3.0.jar:peersim-1.0.5/djep-1.0.0.jar" \
  $(find src -name "*.java")
```

Run a single simulation config; set the classpath:
```
RUNCP="out:peersim-1.0.5/peersim-1.0.5.jar:peersim-1.0.5/jep-2.3.0.jar:peersim-1.0.5/djep-1.0.0.jar"
```

Run the default vs proposed routing experiments (prints summary only):

### Default (full flooding, kforward = 0)
```
for cfg in configs/unstructured_2_2_baseline.cfg \
           configs/unstructured_2_2_baseline_20.cfg \
           configs/unstructured_2_2_baseline_40.cfg
do
  echo "===== $cfg ====="
  java -cp "$RUNCP" peersim.Simulator "$cfg" 2>&1 | sed -n '/^=== SUMMARY ===$/,$p'
done
```

### Proposed (partial flooding, kforward = 2)
```
for cfg in configs/unstructured_2_2_k2.cfg \
           configs/unstructured_2_2_k2_20.cfg \
           configs/unstructured_2_2_k2_40.cfg
do
  echo "===== $cfg ====="
  java -cp "$RUNCP" peersim.Simulator "$cfg" 2>&1 | sed -n '/^=== SUMMARY ===$/,$p'
done
```

---

## Routing Protocols Compared

### Default Routing Protocol
- Flooding-based propagation  
- Fixed TTL
- Forward to ALL neighbors except sender  

This maximizes search coverage but generates more network traffic.

DEFAULT RUN STATISTICS
```bash
===== configs/unstructured_2_2_baseline.cfg =====
=== SUMMARY ===
time_cycles=199
completed_queries=10/10
avg_latency_cycles=1.0
throughput_queries_per_cycle=0.05025125628140704
===============
CDSimulator: cycle 199 done
===== configs/unstructured_2_2_baseline_20.cfg =====
=== SUMMARY ===
time_cycles=199
completed_queries=18/20
avg_latency_cycles=1.0
throughput_queries_per_cycle=0.09045226130653267
===============
CDSimulator: cycle 199 done
===== configs/unstructured_2_2_baseline_40.cfg =====
=== SUMMARY ===
time_cycles=199
completed_queries=34/40
avg_latency_cycles=1.0
throughput_queries_per_cycle=0.1708542713567839
===============
```


### Proposed Routing Protocol (Partial Flooding)
- Flooding-based propagation with a cap on fanout per hop
- Fixed TTL
- Forward to (2) randomly chosen neighbors instead of ALL

This reduces routing overhead by limiting how many neighbors each node forwards to, at the cost of reduced query coverage.

PROPOSED RUN STATISTICS
```bash
===== configs/unstructured_2_2_k2.cfg =====
=== SUMMARY ===
time_cycles=199
completed_queries=10/10
avg_latency_cycles=1.1
throughput_queries_per_cycle=0.05025125628140704
===============
CDSimulator: cycle 199 done
===== configs/unstructured_2_2_k2_20.cfg =====
=== SUMMARY ===
time_cycles=199
completed_queries=17/20
avg_latency_cycles=1.1176470588235294
throughput_queries_per_cycle=0.08542713567839195
===============
CDSimulator: cycle 199 done
===== configs/unstructured_2_2_k2_40.cfg =====
=== SUMMARY ===
time_cycles=199
completed_queries=32/40
avg_latency_cycles=1.03125
throughput_queries_per_cycle=0.16080402010050251
===============
```

Compared to full flooding, kforward=2 slightly reduced completion rate and slightly increased average latency, which lowered the measured throughput. The main benefit of k-forwarding is reduced message propagation/routing overhead.

---

## Workload Configuration

Experiments vary:
- Number of files per peer (5 → 10 → 20)
- Number of queries (10 → 20 → 40)
- Number of peers (5 → 10 → 20)

For each configuration, the following metrics are collected:
- **Average Latency** (cycles per query)
- **Throughput** (queries per cycle)

---

## Observations

From the experiments, changing the k-forwarding parameter directly impacts performance. With full flooding (`kforward=0`), queries reach more peers, leading to higher completion rates and very low latency, but at the cost of increased message propagation across the network. When limiting forwarding to a subset of neighbors (`kforward=2`), fewer peers receive each query, which reduces routing overhead but slightly lowers completion rate and slightly increases average latency. As the number of queries increases, the tradeoff becomes more noticeable: partial flooding performs less work overall but may miss some results. Overall, this demonstrates the classic tradeoff between search coverage and routing efficiency in unstructured P2P networks.
