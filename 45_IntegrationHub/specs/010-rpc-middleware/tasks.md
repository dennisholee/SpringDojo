# Tasks: RPC Middleware (gRPC)

Feature: [spec.md](spec.md)

- [ ] T001 FR-001: Draft Protobuf contracts (messages & services)
- [ ] T002 FR-002: Document transport-to-domain mapping rules (adapters perform mapping)
- [ ] T003 FR-003: Define performance benchmark harness and nightly execution plan
- [ ] T004 FR-004: Implement schema-evolution/versioning policy and tests
- [ ] T005 Implement `protobuf-maven-plugin` config and generate stubs locally
- [ ] T006 Implement adapter mapping classes and unit tests for correctness
- [ ] T007 Add contract integration tests and PR smoke tests
- [ ] T008 Add CI workflows: PR smoke and nightly performance benchmark jobs
- [ ] T009 Write `quickstart.md` with generation and test commands
- [ ] T010 Open feature branch, commit changes, and request review
- [ ] T011 Add ArchUnit fitness-function tests and enforce them in CI (module-level)
- [ ] T012 Add Pitest configuration and CI job enforcing mutation thresholds (core >=90%, adapters >=80%)
- [ ] T013 Clarify generated-sources policy in `plan.md` and configure CI to generate stubs during build (do not commit generated files)
- [ ] T014 Define representative performance load profile and implement benchmark harness (smoke + nightly)
