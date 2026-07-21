---
title: Personnel Vehicle Management
description: >-
  Add a Vehicle entity (loại xe/hãng xe/hiệu xe/biển số + multiple images) owned
  1:1 by MilitaryPersonnel, created alongside personnel creation, with
  independent CRUD.
status: completed
priority: P2
branch: master
tags:
  - backend
  - dynamodb
  - s3
  - personnel
blockedBy: []
blocks: []
created: '2026-07-21T04:42:52.289Z'
createdBy: 'ck:plan'
source: skill
---

# Personnel Vehicle Management

## Overview

Add "phương tiện" (vehicle) as a new field on the military personnel management feature. Each `MilitaryPersonnel` owns exactly one `Vehicle` (1:1), and each `Vehicle` can have multiple images (1:N). Vehicle info (loại xe / vehicle type, hãng xe / brand, hiệu xe / model, biển số xe / license plate, images) is captured at personnel-creation time.

**Scope decision (user-selected: SCOPE EXPANSION):** `Vehicle` is modeled as an independent entity with its own DynamoDB table (`military_vehicles`) and its own REST API (`/api/vehicles`) — list/search, get, update, delete-one-image, delete — rather than embedding it as a nested field inside `MilitaryPersonnelItem`. Vehicle is still only ever **created** as part of `POST /api/personnel` (no standalone public create endpoint) — the 1:1 invariant is enforced by construction, not by a uniqueness check.

**Key architecture decisions (confirmed via scope challenge):**
- Data model: separate `Vehicle` entity + separate `military_vehicles` DynamoDB table (not embedded).
- Vehicle type (loại xe): fixed enum `EVehicleType` (CAR, MOTORBIKE, OTHER), matching the existing `EMilitaryRank`/`EMilitaryPosition` pattern.
- Brand (hãng xe) and model (hiệu xe): free-text strings — open-ended sets, not enums.
- Multi-image upload: reuse the existing generic `/api/common/upload-image` endpoint with a new `category=vehicle` (no new upload endpoint). Individual image *deletion* is a dedicated `VehicleController` endpoint since that requires diffing/persisting the owning vehicle's image list.
- Personnel deletion cascades to vehicle deletion (including its S3 images) to avoid orphaned data — vehicle deleted *before* personnel (see Decisions Log / Red Team Finding 4).
- Access control mirrors the existing `AccessScope` (systemAdmin / adminUnit / user) pattern already used in `MilitaryPersonnelServiceImpl`, via an explicit `canAccessVehicle()` helper (Red Team Finding 1).
- No test suite exists in this repo — verification is compile (JDK 17) + optional live curl against the Lambda deployment (gated by explicit user confirmation before any deploy, per established session precedent).

## Decisions Log

Resolved after the red-team review surfaced them as unvalidated assumptions (see `## Red Team Review` below):

1. **Vehicle is optional at personnel-creation time** — for both admin-created and self-registered (signup) personnel, with no distinction between the two callers. A personnel record can legitimately have zero vehicles.
2. **A vehicle can be attached later** to any personnel who doesn't have one yet, via `POST /api/vehicles?personnelId={id}` (Phase 3) — this also closes the "no remediation path for legacy personnel" gap the optional-vehicle decision would otherwise have left open, and gives failed/aborted vehicle-creation attempts (Finding 3) a recovery path.
3. Consequently, `PERSONNEL_VEHICLE_REQUIRED` was dropped from the error code set; `VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL` was added instead to guard the 1:1 invariant now that two creation paths exist (internal, at personnel-creation; public, via the new attach endpoint).

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Data Model & DynamoDB Infra](./phase-01-data-model-dynamodb-infra.md) | Completed |
| 2 | [Vehicle Service Layer](./phase-02-vehicle-service-layer.md) | Completed |
| 3 | [Vehicle Controller & API](./phase-03-vehicle-controller-api.md) | Completed |
| 4 | [Wire Into Personnel Creation](./phase-04-wire-into-personnel-creation.md) | Completed |
| 5 | [Build & Verify](./phase-05-build-verify.md) | Completed |

## Dependencies

None — greenfield addition, no other in-flight plans in this repo.

## Red Team Review

### Session — 2026-07-21
**Findings:** 9 (9 accepted, 0 rejected)
**Severity breakdown:** 3 Critical, 2 High, 4 Medium

**Methodology note:** the 3 parallel hostile-reviewer subagents (Security Adversary, Failure Mode Analyst, Assumption Destroyer) all failed mid-review due to the platform session limit being hit, before producing any report. The planner performed the same 3-lens adversarial review directly instead, with the same evidence-citation discipline (every finding backed by a `file:line` grep citation, no finding accepted without codebase evidence). Full findings: `reports/from-planner-to-planner-red-team-consolidated-plan-review-report.md`.

| # | Finding | Severity | Disposition | Applied To |
|---|---------|----------|-------------|------------|
| 1 | Under-specified per-tier authorization on VehicleController (IDOR risk) | Critical | Accept | Completed |
| 2 | Vehicle images publicly readable via `/api/common/images/**` (inherited, pre-existing pattern) | Medium | Accept (documented) | Completed |
| 3 | Non-atomic personnel+vehicle creation risks orphaned/duplicate personnel on retry | Critical | Accept | Completed |
| 4 | Cascade-delete ordering can orphan the vehicle if delete fails mid-way | High | Accept | Completed |
| 5 | Orphaned S3 uploads amplified ~10x by multi-image design (inherited, pre-existing pattern) | Medium | Accept (documented) | Completed |
| 6 | Making `vehicle` required-on-create silently forces it onto the self-service signup flow | Critical | Accept → resolved via user decision (Decisions Log #1) | plan.md, Phase 4 |
| 7 | No remediation path for personnel left without a vehicle | High | Accept → resolved via user decision (Decisions Log #2) | Phase 3 |
| 8 | `GET /api/vehicles/by-personnel/{id}` lacks a validated standalone use case | Medium | Accept (documented, kept as low-cost convenience) | Phase 3 |
| 9 | "Every personnel has exactly one vehicle" asserted as universal without confirmation | Medium | Accept → resolved via user decision (Decisions Log #1) | plan.md |

Findings 6 and 9 were the same underlying question (should vehicle be mandatory, and for which callers) — resolved together by the user: vehicle is optional, uniformly, for all creation callers (Decisions Log #1). Finding 7 was resolved as a side effect: the optional-vehicle decision reopened the need for a later-attach path, which is now `POST /api/vehicles?personnelId={id}` (Decisions Log #2).

### Whole-Plan Consistency Sweep
- Files reread: `plan.md`, `phase-01-data-model-dynamodb-infra.md`, `phase-02-vehicle-service-layer.md`, `phase-03-vehicle-controller-api.md`, `phase-04-wire-into-personnel-creation.md`, `phase-05-build-verify.md`
- Decision deltas checked: 4 — (a) `PERSONNEL_VEHICLE_REQUIRED` removed / `VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL` added (Phase 1), (b) `create()` split into internal `create()` + public `attachToPersonnel()` (Phase 2), (c) `POST /api/vehicles?personnelId={id}` added, "no POST" language removed (Phase 3), (d) vehicle-required language replaced with vehicle-optional language + compensating delete + reordered cascade delete (Phase 4)
- Reconciled stale references: all `PERSONNEL_VEHICLE_REQUIRED` mentions removed from Phase 1, Phase 4 (Requirements, Architecture, Implementation Steps, Success Criteria) and Phase 5 (verification checklist, curl checks); Phase 3's "No `POST /api/vehicles` — deliberate" statement replaced with the new endpoint's spec in both prose and the endpoint-tree diagram; Phase 5's success-criteria/checklist items updated to match the optional-vehicle flow
- Unresolved contradictions: 0

## Open Questions

None remaining — both items from the original draft were resolved via the red-team review + user decisions above (see Decisions Log).
