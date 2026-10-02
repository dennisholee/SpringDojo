# ADR-006: Record the Isolation-Over-Topology Conclusion

## Problem statement
Does running the same invariant on a MongoDB *sharded* cluster change the write-skew outcome, and
which axis should drive the MongoDB-versus-distributed-SQL decision?

## Background
A sharded MongoDB cluster still runs snapshot-isolated transactions on each shard; sharding scales
writes but does not raise the isolation level. The invariant is also per-party, so both signatories
key to the same shard even in a sharded cluster. The demonstrated behaviour therefore generalises
from the single-node PoC (see [ADR-003](ADR-003-single-node-engines.md)).

## Constraints
- The conclusion must follow from the demonstrated isolation behaviour, not from a paid topology.
- The invariant must remain per-party (a single-shard transaction) even if sharding were introduced.

## Assumptions
- Sharding partitions by party, so `alice` and `bob` (same `P1`) land on one shard.
- Strict serializable is the engine-level property that prevents the anomaly, independent of nodes.

## Options

| | Record isolation-over-topology | Demo on a sharded cluster | Treat sharding as the deciding factor |
| --- | --- | --- | --- |
| Summary | Conclude isolation is decisive; topology is irrelevant to the anomaly | Pay for a sharded cluster to confirm | Recommend a topology change to "fix" write skew |
| Correctness | Correct — follows from isolation semantics | Redundant | Incorrect — sharding does not raise isolation |
| Cost | None | High | Misleading |
| Score (out of 5) | 5 | 2 | 1 |
| Remarks | Chosen. | Rejected: proves nothing new. | Rejected: wrong causal attribution. |

## Consequences
- The MongoDB-versus-distributed-SQL decision is framed on the correct axis: **isolation level**, not
  topology.
- A sharded MongoDB cluster is not required to settle the question, saving topology cost.
- Any future recommendation to keep MongoDB must propose application-level mitigation (hot-row
  counters / explicit locks) for count/absence invariants.
