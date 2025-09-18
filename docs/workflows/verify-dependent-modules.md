# Verify Dependent Modules (Manual Workflow)

## When to run
- Run after any change that updates Maven coordinates, shared APIs, or integration points for dependent services.
- Trigger before tagging releases or merging PRs that downstream modules rely on.
- Re-run after rebasing or if the previous attempt was cancelled or failed due to infrastructure.

## How to run (UI/CLI)
1. GitHub UI: Actions → Verify Dependent Modules → Run workflow → choose branch → Run.
2. Wait for the summary comment on the PR and inspect module rows for failures.

## Parameters
- Inputs: none required; workflow uses the current branch ref.
- Dependent modules: mgr-applications, mgr-tenants, mgr-tenant-entitlements, mod-roles-keycloak, mod-login-keycloak, mod-users-keycloak, mod-scheduler, mod-consortia-keycloak, folio-module-sidecar.

## Requirements/permissions
- GitHub write access to trigger manual workflows and update pull request comments.
- Builds run on Temurin JDK 21 with Maven; no repository secrets are consumed.
- Contact @folio-org/eureka for workflow ownership or matrix updates.

