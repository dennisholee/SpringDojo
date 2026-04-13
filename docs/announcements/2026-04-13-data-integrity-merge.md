**Data Integrity — Merge & Hotfix (2026-04-13)**

Summary
-------
The Data Integrity changes and accompanying CI/Pitest fixes were merged and a hotfix branch was created to ensure CI artifacts can be collected for debugging.

What changed
------------
- Excluded generated protobuf classes from Pitest runs.
- Added an artifact upload step for `45_IntegrationHub/target/pit-reports/**` to assist remote debugging.
- Created hotfix branch: `hotfix/pitest-ci-2026-04-13` and PR #28 (merged).
- Re-enabled the Pitest job on `master` after the hotfix.

Where to look
-------------
- Release notes: [docs/release-notes/2026-04-13-data-integrity.md](docs/release-notes/2026-04-13-data-integrity.md)
- Hotfix PR: https://github.com/dennisholee/SpringDojo/pull/28

Next steps / ask
----------------
- CI is re-enabled; if you see Pitest failures, please share runner logs or grant access so I can collect artifacts.
- If you want the fixes on a maintenance branch, tell me the branch name and I'll create backport PRs.

Contact
-------
Ping @dennisholee for questions or to request backports.
