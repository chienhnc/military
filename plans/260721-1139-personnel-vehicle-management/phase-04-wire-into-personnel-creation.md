---
phase: 4
title: Wire Into Personnel Creation
status: completed
priority: P1
effort: 2h
dependencies:
  - 2
  - 3
---

# Phase 4: Wire Into Personnel Creation

## Overview

Connect `Vehicle` to the existing personnel lifecycle: `MilitaryPersonnelRequest` carries a nested `vehicle` payload at creation time (required), `MilitaryPersonnelResponse` embeds the owning vehicle for convenience reads, and personnel deletion cascades to vehicle deletion (including its S3 images) so no orphaned data is left behind.

## Requirements

> **[Red Team applied — decision revised, see `plan.md` Decisions Log]** Vehicle is **optional** at personnel-creation time, uniformly for both admin-created and self-registered (signup) personnel — no caller-context branching. This resolves what would otherwise have been a Critical finding (forcing every self-registering user through `AuthServiceImpl.registerUser()` to declare a vehicle was never confirmed as intended) and a High finding (no remediation path existed for personnel left without a vehicle — now resolved by Phase 3's `POST /api/vehicles?personnelId={id}`).

- Functional:
  - `POST /api/personnel` (and the `AuthServiceImpl.registerUser()` signup path, which delegates to the same `create()`) creates a linked `Vehicle` **only if** the request includes a `vehicle` payload; personnel creation succeeds either way.
  - `GET /api/personnel/{id}` and the list endpoint return `vehicle` embedded in the response; `null` if the personnel has no vehicle (whether because none was ever provided, or the record predates this feature — same, unremarkable case, not an error).
  - `PUT /api/personnel/{id}` (update) does **not** touch vehicle data — vehicle updates go exclusively through `PUT /api/vehicles/{id}` (Phase 3), and a personnel with no vehicle yet gets one via `POST /api/vehicles?personnelId={id}` (Phase 3). `MilitaryPersonnelRequest.vehicle` is simply ignored on update.
  - `DELETE /api/personnel/{id}` also deletes the personnel's vehicle (row + S3 images), if one exists.
- Non-functional: `MilitaryPersonnelRequest` is NOT split into separate create/update DTOs — a single class stays reused (matches existing codebase convention of one request type per controller action pair). No `@NotNull`/manual-required-check needed on `vehicle` since it's genuinely optional now.

## Architecture

```
MilitaryPersonnelRequest.vehicle (VehicleRequest, optional)
        │
        ▼
MilitaryPersonnelServiceImpl.create()
        │  1. save personnel (existing logic, unchanged)
        │  2. if request.getVehicle() != null:
        │       try { vehicleService.create(savedPersonnel.getId(), request.getVehicle()); }
        │       catch (Exception ex) {
        │         militaryPersonnelRepository.delete(saved);  // compensating delete — Finding 3
        │         throw ex;
        │       }
        ▼
MilitaryPersonnelServiceImpl.toResponse()
        │  attempt vehicleRepository.findByPersonnelId(personnel.getId())
        │  present  → response.setVehicle(VehicleResponse-with-imageUrls)
        │  absent   → response.setVehicle(null)   // no vehicle yet, or legacy personnel — not an error
        ▼
MilitaryPersonnelServiceImpl.delete()
        │  1. vehicleService.deleteByPersonnelId(personnel.getId())   // no-op if none exists — BEFORE personnel delete, Finding 4
        │  2. existing delete logic (image cleanup, repository.delete(personnel))
```

## Related Code Files

**Modify:**
- `src/main/java/com/military/payload/request/MilitaryPersonnelRequest.java` — add nested `vehicle` field
- `src/main/java/com/military/payload/response/MilitaryPersonnelResponse.java` — add nested `vehicle` field
- `src/main/java/com/military/service/impl/MilitaryPersonnelServiceImpl.java` — inject `VehicleService` + `VehicleRepository`, wire create/toResponse/delete

## Implementation Steps

1. **`MilitaryPersonnelRequest.java`** — add after `imagePath`:
   ```java
   @Schema(description = "Thong tin phuong tien cua quan nhan (khong bat buoc; co the gan sau qua POST /api/vehicles)")
   @Valid
   private VehicleRequest vehicle;
   ```
   Not `@NotNull` — genuinely optional, and this DTO is reused by `PUT /api/personnel/{id}` (see `MilitaryPersonnelController.update()`), where `vehicle` is ignored regardless. `@Valid` still cascades field-level validation (e.g. `@NotBlank brand`) whenever a `vehicle` object IS present, but allows the object itself to be absent without a bean-validation error. Imports needed: `com.military.payload.request.VehicleRequest` is already in the same package (no import needed) — but confirm `jakarta.validation.Valid` is imported (it already is, for `SignupRequest`'s nested validation pattern — check `MilitaryPersonnelRequest` doesn't already import it; if not, add `import jakarta.validation.Valid;`).

2. **`MilitaryPersonnelResponse.java`** — add:
   ```java
   private VehicleResponse vehicle;
   ```
   `BeanUtils.copyProperties(militaryPersonnel, this)` in the constructor will NOT populate this (no `vehicle` property on the `MilitaryPersonnel` model — intentionally not stored there, since `Vehicle` lives in its own table per the SCOPE EXPANSION decision). It's set explicitly in `MilitaryPersonnelServiceImpl.toResponse()` — see Step 4.

3. **`MilitaryPersonnelServiceImpl.java`** constructor — add `VehicleService vehicleService` and `VehicleRepository vehicleRepository` as new constructor params (service for create/delete-cascade which need authorization + S3 cleanup; repository for the plain `findByPersonnelId` lookup in `toResponse()`, since that read path should NOT throw/authorize-fail when embedding into a personnel response the caller is already authorized to see — using the repository directly avoids a redundant/conflicting authorization check inside `toResponse()`).

4. **`create()`** — after `MilitaryPersonnel saved = militaryPersonnelRepository.save(personnel);` and before `return toResponse(saved);`:
   ```java
   if (militaryPersonnelRequest.getVehicle() != null) {
     try {
       vehicleService.create(saved.getId(), militaryPersonnelRequest.getVehicle());
     } catch (RuntimeException ex) {
       militaryPersonnelRepository.delete(saved);
       throw ex;
     }
   }
   ```
   > **[Red Team applied — Finding 3, Critical]** If `vehicleService.create()` throws (e.g. a transient DynamoDB error) after the personnel row was already saved, `GlobalExceptionHandler`'s catch-all `@ExceptionHandler(Exception.class)` (`src/main/java/com/military/exception/GlobalExceptionHandler.java:58`) would otherwise return a generic 500 while silently leaving the personnel row persisted — a client retry would then create a **second** personnel record for the same person (no idempotency check exists in `create()`). The compensating `militaryPersonnelRepository.delete(saved)` on failure keeps the operation effectively all-or-nothing from the client's perspective, without needing a real DynamoDB transaction (none is used anywhere in this codebase).

   Note: `toResponse(saved)` is called after this, so the embedded vehicle in the create-response will reflect the just-created vehicle (Step 5's `toResponse` reads it back via `vehicleRepository.findByPersonnelId`) — or stays `null` if no `vehicle` was provided.

5. **`toResponse(MilitaryPersonnel personnel)`** — after building `response` and before setting `imageUrl`, add:
   ```java
   vehicleRepository.findByPersonnelId(personnel.getId())
       .ifPresent(vehicle -> response.setVehicle(toVehicleResponse(vehicle)));
   ```
   Add a small private helper `toVehicleResponse(Vehicle vehicle)` that builds a `VehicleResponse` with `imageUrls` populated (same `IMAGE_ENDPOINT`-prefix logic as `VehicleServiceImpl.toResponse()` — duplicated here deliberately rather than calling `vehicleService.getById()`, because that method authorizes against the *vehicle's* owner and would incorrectly reject e.g. an `adminUnit` viewing a personnel record they're allowed to see but whose vehicle-specific authorization check has slightly different tier semantics; reading directly from the repository sidesteps a double-authorization mismatch). Use endpoint constant `"/api/common/images/vehicle/"` matching Phase 2/3's `VehicleServiceImpl.IMAGE_ENDPOINT`.

6. **`delete()`** — add `vehicleService.deleteByPersonnelId(personnel.getId());` **before** the existing `militaryPersonnelRepository.delete(personnel);` line (`src/main/java/com/military/service/impl/MilitaryPersonnelServiceImpl.java:159`), not after.
   > **[Red Team applied — Finding 4, High]** The original plan placed this call after `repository.delete(personnel)`. If the vehicle-delete then throws (e.g. a transient S3 error deleting one of several images), the personnel row would already be gone while the vehicle row + remaining S3 images become permanently orphaned (unreachable — no personnel exists to look them up by). Deleting the vehicle first means a failure there aborts the whole `delete()` call cleanly, leaving the personnel record intact and nothing orphaned; the caller can safely retry.

7. **`update()` / `applyFullUpdate()`** — **no changes**. `MilitaryPersonnelRequest.vehicle` is simply never read on the update path, matching the "update ignores vehicle" design decision.

## Success Criteria

- [ ] `POST /api/personnel` without a `vehicle` payload succeeds and creates a personnel record with no vehicle (`GET` returns `vehicle: null`)
- [ ] `POST /api/personnel` with a valid `vehicle` payload creates both the personnel row and a linked `Vehicle` row (verify via `GET /api/vehicles/by-personnel/{id}` returning the newly created vehicle)
- [ ] `GET /api/personnel/{id}` response includes `vehicle.imageUrls` pointing at `/api/common/images/vehicle/{filename}` when a vehicle exists
- [ ] `DELETE /api/personnel/{id}` also removes the linked vehicle row and its S3 images (verify vehicle 404s afterward), and the delete happens vehicle-first (Finding 4)
- [ ] `PUT /api/personnel/{id}` with a `vehicle` payload in the body does NOT alter the existing vehicle (confirms update-path is a true no-op for this field)
- [ ] Signup flow (`AuthServiceImpl.registerUser()` → `SignupRequest.militaryPersonnel`) creates a vehicle only if one is supplied, same as admin-created personnel — no special-casing between the two callers
- [ ] If `vehicleService.create()` throws during `POST /api/personnel`, the personnel row is NOT left behind (compensating delete verified — Finding 3)

## Risk Assessment

- **Compensating delete is best-effort, not a real transaction**: if `vehicleService.create()` throws AND the subsequent `militaryPersonnelRepository.delete(saved)` ALSO throws (e.g. two consecutive transient DynamoDB errors), the personnel row could still be left orphaned. This is accepted as a bounded improvement over the original no-cleanup design, not a full guarantee — a genuinely atomic operation isn't achievable without DynamoDB's `TransactWriteItems` API, which isn't used anywhere in this codebase and is out of scope to introduce for this feature alone.
- **`toResponse()` reads the vehicle on every single personnel fetch/list call** (one extra `findByPersonnelId` scan per personnel row, per Phase 1's scan-based repository). For `list()`, which already iterates `findAllList()` and filters in-memory, this multiplies to one vehicle-table scan per *visible* personnel row per page — consistent with this codebase's existing N+1-via-scan patterns (e.g. `MilitaryPersonnelServiceImpl.list()` already does a full scan per request), not a new class of inefficiency, but worth flagging if personnel list pages ever get slow — a candidate follow-up (batch-fetch all vehicles once via `findAllList()` and match in-memory) if this becomes a measured problem.
