# Sample Mapping Contract: SimpleMessageMapping

Purpose
-------
Provide a minimal example mapping contract that renames an incoming `message` field to `text` and preserves other fields.

Input schema
------------
- `message`: string
- `id`: integer

Output schema
-------------
- `text`: string
- `id`: integer

Mapping rules
-------------
1. If `message` exists, set `text` to the same value in the output.
2. Copy all other fields unchanged.

Notes
-----
This contract is intentionally simple — it exists to exercise purity/idempotence tests for the Data Integrity feature.
