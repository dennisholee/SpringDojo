This PR adds a manual `workflow_dispatch` trigger to the Pitest workflow so the job can be executed on demand for debugging and verification.

Why

- Some failing runs are hard to reproduce from merged commits; allowing a manual dispatch lets maintainers reproduce and iterate quickly without pushing commits to master.

What

- Adds `workflow_dispatch` to `.github/workflows/pitest.yml`.

How to run

- After this PR is merged (or from this branch), run the workflow manually via the Actions UI or:

```
gh workflow run .github/workflows/pitest.yml --ref <branch>
```

Notes

- This change does not alter existing push/PR triggers.
