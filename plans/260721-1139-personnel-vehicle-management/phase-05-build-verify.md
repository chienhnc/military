---
phase: 5
title: Build & Verify
status: completed
priority: P2
effort: 1h
dependencies:
  - 1
  - 2
  - 3
  - 4
---

# Phase 5: Build & Verify

## Overview

Compile the full change set, run a code-review pass against the risks flagged in Phases 1-4, and (only with explicit user confirmation, per this session's established deploy protocol) redeploy to AWS Lambda and verify the new endpoints live against the actual DynamoDB/S3 infra.

## Requirements

- Non-functional: this repo has no automated test suite (`src/test` is empty — confirmed earlier in this session's build output: "No sources to compile" for `testCompile`). Verification is compile-correctness + manual/curl checks, not unit/integration tests. Do not fabricate a test suite as part of this plan — out of scope unless the user asks for it separately.

## Architecture

N/A — this phase is verification-only, no new code.

## Related Code Files

**Modify:** none (verification phase).

## Implementation Steps

1. **Compile** with JDK 17 (this project requires JDK 17 — JDK 25 breaks Lombok annotation processing, confirmed earlier this session):
   ```powershell
   $env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.18"
   $env:Path = "C:\Program Files\Java\jdk-17.0.18\bin;$env:Path"
   mvn -q -DskipTests clean compile
   ```
   Fix any compile errors before proceeding — do not skip to deploy on a broken build.

2. **Code-review checklist** (self-review against the Risk Assessment sections of Phases 1-4):
   - [ ] `VehicleItem`'s `List<String> imagePaths` maps correctly through DynamoDB Enhanced Client (no `@DynamoDbBean`-specific collection annotation missing)
   - [ ] `VehicleServiceImpl.update()` actually deletes S3 objects for images removed from the list (not just updates the DynamoDB row)
   - [ ] `MilitaryPersonnelServiceImpl.create()` runs the compensating `militaryPersonnelRepository.delete(saved)` if `vehicleService.create()` throws (Finding 3)
   - [ ] `MilitaryPersonnelServiceImpl.delete()` calls `vehicleService.deleteByPersonnelId()` BEFORE `militaryPersonnelRepository.delete(personnel)`, not after (Finding 4)
   - [ ] `VehicleServiceImpl` has a concrete `canAccessVehicle()` helper used by every caller-facing method, not just a vague "check AccessScope" (Finding 1)
   - [ ] `VehicleServiceImpl.create()` (internal) has no auth check; `attachToPersonnel()` (public) does — confirm the signup path still works with no authenticated principal
   - [ ] No `@CrossOrigin` annotation was added to `VehicleController` (must stay centralized in `WebSecurityConfig`, per the earlier CORS fix in this session)
   - [ ] `ErrorCode` new entries have unique `MIL000xx` codes with no collisions, and matching keys exist in BOTH `messages.properties` and `messages_vi.properties`
   - [ ] `template.yaml`'s new `VehiclesTable` resource doesn't accidentally break the existing `Replacement: False` guarantee for `MilitaryApiFunction`/`MilitaryHttpApi` (i.e. the changeset should show `VehiclesTable` as a pure addition, same shape as when `UnitsTable` was originally added)

3. **Optional: redeploy + live verification** — **ask the user for explicit confirmation before running this step**, per this session's established pattern (deploying is a hard-to-reverse, real-AWS-cost action):
   - Rebuild + `sam deploy` (same command shape used earlier this session in `deploy_lambda.ps1`, reusing the same `S3Bucket`/`Region`/`StackName` — note a **new** `JwtSecret` will be required again unless a `samconfig.toml` was saved, since it's `NoEcho` in CloudFormation and cannot be retrieved from the existing stack).
   - Confirm the changeset shows `VehiclesTable` as `Create` (not `Replace`/`Delete` on any existing resource) before letting `sam deploy` proceed.
   - Live curl checks against the deployed API:
     - `POST /api/personnel` without `vehicle` → expect `200`, personnel created with no vehicle; `GET /api/vehicles/by-personnel/{id}` → expect `404 VEHICLE_NOT_FOUND`.
     - `POST /api/vehicles?personnelId={id}` for that same personnel → expect `200`, vehicle now attached; repeating the same call → expect `400 VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL`.
     - `POST /api/personnel` with a valid `vehicle` (no images yet) → expect `200`, then `GET /api/vehicles/by-personnel/{id}` → expect the created vehicle.
     - `POST /api/common/upload-image?category=vehicle` with a real image file → expect `200` + filename returned.
     - `PUT /api/vehicles/{id}` adding that filename to `imagePaths` → expect `200`, `imageUrls` includes `/api/common/images/vehicle/{filename}`.
     - `GET /api/common/images/vehicle/{filename}` → expect the image bytes back.
     - `DELETE /api/vehicles/{id}/images?imagePath={filename}` → expect `200`, then re-`GET` the vehicle → `imageUrls` no longer contains it, and the S3 object is gone (verify via a repeat `GET /api/common/images/vehicle/{filename}` → expect `404`/`VEHICLE_IMAGE_NOT_FOUND` equivalent from `MilitaryUnitServiceImpl`-style `loadImage` semantics... actually `loadImage` on a deleted S3 key returns `NoSuchKeyException` → mapped to `VEHICLE_NOT_FOUND` per Phase 2's `loadImage` design mirroring `MilitaryUnitServiceImpl.loadLogo`'s `NoSuchKeyException → *_NOT_FOUND` mapping).
     - `DELETE /api/personnel/{id}` → then `GET /api/vehicles/by-personnel/{id}` → expect `404` `VEHICLE_NOT_FOUND` (cascade delete confirmed).

## Success Criteria

- [ ] `mvn clean compile` succeeds with zero errors on JDK 17
- [ ] Code-review checklist fully checked off
- [ ] (If user approves deploy) all live curl checks in Step 3 pass as described

## Risk Assessment

- **No automated regression protection** — this entire feature ships without unit/integration tests, matching the rest of this codebase. Future changes to `MilitaryPersonnelServiceImpl`/`VehicleServiceImpl` could silently break the create→vehicle-creation or delete→cascade-delete wiring with no test to catch it. Explicitly out of scope for this plan (no test infra exists to extend) — flag to the user as a standing gap, not something this plan silently accepts as "fine."
- **Deploy step is destructive-adjacent** — creates a real new DynamoDB table and modifies the live Lambda function. Must not run without explicit user confirmation, consistent with this session's established protocol (the CORS-fix deploy and the earlier region-removal deploy were both gated the same way).
