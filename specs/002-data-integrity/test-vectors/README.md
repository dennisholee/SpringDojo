# Test vectors for 002 — Data Integrity

Place canonical test vectors here. Tests should load these files to exercise `ValidationService` and checksum verification.

Files:
- `echo_valid.json` — valid EchoRequest payload
- `echo_invalid_empty.json` — empty `message` field (invalid)
- `echo_large.json` — large payload for edge-case/performance tests
