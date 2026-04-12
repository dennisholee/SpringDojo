When using GitHub Spec Kit, the /speckit.specify command is the first step in Spec-Driven Development (SDD). It transforms a high-level idea into a structured specification that guides AI agents during planning and implementation.

The golden rule for a /specify prompt is simple: describe the "What" and the "Why" clearly, and defer the "How" (implementation and technology choices) to the later /plan phase.

## Guideline: The 5 Pillars of a Strong /specify Prompt
To get the most value from Spec Kit, a /specify prompt should include these five areas.

## 1. Core Objective (The "Why")
- One-line summary of the feature or change.
- The problem you're solving and the business motivation.
- The primary measurable outcome or user value.

Example: "Build a photo organization app to help users manage large galleries by grouping images into albums by date."

## 2. User Scenarios & Journeys
- Identify personas and roles (e.g., admin, regular user).
- Describe the main success flow and important alternate/error flows.
- Provide short, concrete scenarios or sample tasks.

Tip: Use numbered bullets for flows (happy path + 1–2 alternates).

## 3. Functional Requirements
- Concrete behaviors, inputs and outputs, and validation rules.
- Expected UI/UX outcomes or API response shapes (high level).
- Prioritize requirements (MVP vs. nice-to-have) when relevant.

Example: "Albums cannot be nested; each album shows a tile preview of its photos."

## 4. Constraints & Business Logic
- Business rules, regulatory or privacy constraints, compatibility limits.
- Non-functional constraints: scale, latency, storage limits.
- Explicitly list things to avoid (implementation constraints belong in /plan).

## 5. Success Criteria (The "Done" State)
- Measurable acceptance criteria and example tests (Given/When/Then).
- Non-functional targets: performance, accessibility level, reliability.
- Clear deliverables the spec should produce (docs, stories, tests).

Example: "Dashboard loads in <200ms with 1,000 tasks; passes WCAG AA for main flows."

---
Short Spec Template (copy and paste)

Title:
Context:
Users / Personas:
Primary Scenarios (1–3):
Functional Requirements:
Constraints & Business Rules:
Success Criteria / Acceptance Tests:
Desired Outputs (e.g., spec.md, user-stories.md, acceptance-tests.md):

Example Prompts

- Short (quick):
/speckit.specify "Create a task manager where users can create/edit/delete tasks with priorities and due dates; dashboard shows upcoming deadlines."

- Detailed (covers five pillars):
/speckit.specify "Title: Task manager. Context: Small teams need a simple task tracker. Users: team member, manager. Scenarios: 1) Create task with priority/due date; 2) Manager assigns tasks; 3) Dashboard shows upcoming deadlines. Functional requirements: create/edit/delete tasks, filter by priority, email reminders. Constraints: no nested tasks, must work offline for 10 minutes. Success criteria: dashboard loads in <200ms with 1,000 tasks; accessibility AA."

Best Practices & Tips

- Start with a one-line title and a short context paragraph.
- Prefer bullet lists for scenarios and requirements.
- Make success criteria measurable and testable.
- Include sample inputs/outputs or example data when helpful.
- Specify the desired output format(s) (spec, user stories, tests).
- Keep prompts concise but complete (aim for ~100–400 words).
- Avoid naming frameworks, libraries, or specific infrastructure in /specify.

Common Mistakes to Avoid

- Mixing implementation details (the "How") into /specify prompts.
- Vague goals without user scenarios or acceptance criteria.
- Overly long background that buries the actual requirements.
- Not specifying the desired output format.

Prompt Checklist & Summary

| DO Include (The "What") | DON'T Include (The "How") |
|---|---|
| Clear goal and motivation | Frameworks, libraries, DB schema |
| Concrete user scenarios | Folder layouts, endpoint names |
| Measurable success criteria | Prescriptive implementation details |
| Edge cases & constraints | Deployment or infra instructions |

What /speckit.specify should produce

- A concise spec document covering the five pillars.
- Prioritized user stories and functional requirements.
- Suggested acceptance tests (Given/When/Then) and success criteria.
- A short list of next actions for the /plan phase.

Example of a Strong Prompt

/speckit.specify "Create a task management system where users can create, edit, and delete tasks. Tasks must have priority levels (Low, Med, High) and due dates. Users need a dashboard view showing upcoming deadlines. Ensure the interface is responsive and follows a minimalist aesthetic."

---

Next step: use /speckit.plan to convert this spec into an implementation plan.
When using GitHub Spec Kit, the /speckit.specify command is the first step in Spec-Driven Development (SDD). It transforms a high-level idea into a structured specification that guides AI agents during planning and implementation.

The golden rule for a /specify prompt is simple: describe the "What" and the "Why" clearly, and defer the "How" (implementation and technology choices) to the later /plan phase.

## Guideline: The 5 Pillars of a Strong /specify Prompt
To get the most value from Spec Kit, a /specify prompt should include these five areas.

## 1. Core Objective (The "Why")
- One-line summary of the feature or change.
- The problem you're solving and the business motivation.
- The primary measurable outcome or user value.

Example: "Build a photo organization app to help users manage large galleries by grouping images into albums by date."

## 2. User Scenarios & Journeys
- Identify personas and roles (e.g., admin, regular user).
- Describe the main success flow and important alternate/error flows.
- Provide short, concrete scenarios or sample tasks.

Tip: Use numbered bullets for flows (happy path + 1–2 alternates).

## 3. Functional Requirements
- Concrete behaviors, inputs and outputs, and validation rules.
- Expected UI/UX outcomes or API response shapes (high level).
- Prioritize requirements (MVP vs. nice-to-have) when relevant.

Example: "Albums cannot be nested; each album shows a tile preview of its photos."

## 4. Constraints & Business Logic
- Business rules, regulatory or privacy constraints, compatibility limits.
- Non-functional constraints: scale, latency, storage limits.
- Explicitly list things to avoid (implementation constraints belong in /plan).

## 5. Success Criteria (The "Done" State)
- Measurable acceptance criteria and example tests (Given/When/Then).
- Non-functional targets: performance, accessibility level, reliability.
- Clear deliverables the spec should produce (docs, stories, tests).

Example: "Dashboard loads in <200ms with 1,000 tasks; passes WCAG AA for main flows."

---

Short Spec Template (copy and paste)

Title:
Context:
Users / Personas:
Primary Scenarios (1–3):
Functional Requirements:
Constraints & Business Rules:
Success Criteria / Acceptance Tests:
Desired Outputs (e.g., spec.md, user-stories.md, acceptance-tests.md):

Example Prompts

- Short (quick):
/speckit.specify "Create a task manager where users can create/edit/delete tasks with priorities and due dates; dashboard shows upcoming deadlines."

- Detailed (covers five pillars):
/speckit.specify "Title: Task manager. Context: Small teams need a simple task tracker. Users: team member, manager. Scenarios: 1) Create task with priority/due date; 2) Manager assigns tasks; 3) Dashboard shows upcoming deadlines. Functional requirements: create/edit/delete tasks, filter by priority, email reminders. Constraints: no nested tasks, must work offline for 10 minutes. Success criteria: dashboard loads in <200ms with 1,000 tasks; accessibility AA."

Best Practices & Tips

- Start with a one-line title and a short context paragraph.
- Prefer bullet lists for scenarios and requirements.
- Make success criteria measurable and testable.
- Include sample inputs/outputs or example data when helpful.
- Specify the desired output format(s) (spec, user stories, tests).
- Keep prompts concise but complete (aim for ~100–400 words).
- Avoid naming frameworks, libraries, or specific infrastructure in /specify.

Common Mistakes to Avoid

- Mixing implementation details (the "How") into /specify prompts.
- Vague goals without user scenarios or acceptance criteria.
- Overly long background that buries the actual requirements.
- Not specifying the desired output format.

Prompt Checklist & Summary

| DO Include (The "What") | DON'T Include (The "How") |
|---|---|
| Clear goal and motivation | Frameworks, libraries, DB schema |
| Concrete user scenarios | Folder layouts, endpoint names |
| Measurable success criteria | Prescriptive implementation details |
| Edge cases & constraints | Deployment or infra instructions |

What /speckit.specify should produce

- A concise spec document covering the five pillars.
- Prioritized user stories and functional requirements.
- Suggested acceptance tests (Given/When/Then) and success criteria.
- A short list of next actions for the /plan phase.

Example of a Strong Prompt

/speckit.specify "Create a task management system where users can create, edit, and delete tasks. Tasks must have priority levels (Low, Med, High) and due dates. Users need a dashboard view showing upcoming deadlines. Ensure the interface is responsive and follows a minimalist aesthetic."

---

Next step: use /speckit.plan to convert this spec into an implementation plan.

