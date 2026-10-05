# Security Policy

## Supported versions

This is a single-branch portfolio project (no maintained release lines) - security fixes land on
`main` only, then roll out via the existing CI/CD pipeline to the live deployment. There is no
LTS/backport policy.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for a suspected vulnerability. Instead, use
[GitHub's private vulnerability reporting](../../security/advisories/new) for this repository
(Security tab → "Report a vulnerability"), or contact the maintainer directly via the email on
the [GitHub profile](https://github.com/ronybrand).

Include, where applicable:

- A description of the vulnerability and its potential impact.
- Steps to reproduce (a minimal request/payload is ideal).
- The affected endpoint(s) or component(s).

This project is maintained on a best-effort basis (no SLA), but reports are taken seriously and
triaged as soon as possible. Since this agent is live and publicly reachable, reports affecting
the running deployment get priority.

## Scope

In scope: the application code in this repository (`src/main`), its prompt-injection and
system-prompt-leak guards, and the Dockerfile/CI workflows that build and ship it.

Out of scope: the [`estado`](https://github.com/ronybrand/estado) API it calls via tool calling
and Google Gemini itself, each covered by their own security policies. Dependency vulnerabilities
are tracked automatically via Dependabot and CodeQL (see badges in [README.md](README.md)) rather
than manual reports.

## What this project already does

- Automated dependency updates via Dependabot, auto-merged after CI passes.
- Static analysis on every push/PR via [CodeQL](.github/workflows/codeql.yml).
- No production secrets committed to the repository - the Gemini API key is injected via
  environment variable at deploy time, never hardcoded.
- Deterministic guards against prompt injection and system-prompt leakage, independent of model
  behavior - not something the project relies on the LLM to self-police.
- Per-IP rate limiting (Bucket4j, in-memory) to prevent quota exhaustion/abuse of the upstream
  Gemini API.
