---
phase: 2
title: Vehicle Service Layer
status: completed
priority: P1
effort: 4h
dependencies:
  - 1
---

# Phase 2: Vehicle Service Layer

## Overview

Implement `VehicleService`/`VehicleServiceImpl`: CRUD operations, S3-backed multi-image storage (store/load/delete-one), image-list diffing on update (clean up removed S3 objects), and access control that mirrors the existing `AccessScope` pattern already used in `MilitaryPersonnelServiceImpl`.

## Requirements

- Functional:
  - `create(personnelId, request)` — **internal, unauthenticated-safe**. Called only by `MilitaryPersonnelServiceImpl.create()` when the personnel-creation request includes a `vehicle` payload. No caller-authorization check inside this method — the personnel-creation operation itself is already authorized by `MilitaryPersonnelServiceImpl.create()` before this is called, and that flow can run with a **null** `AccessScope` (the self-registration/signup path has no authenticated principal yet — see `resolveAccessScopeIfPresent()` in `MilitaryPersonnelServiceImpl`). Still enforces the 1:1 invariant via a `findByPersonnelId` guard (throws `VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL`).
  - `attachToPersonnel(personnelId, request)` — **public-facing**, backs `POST /api/vehicles?personnelId={id}` (Phase 3), used to attach a vehicle later to a personnel who doesn't have one yet. Same uniqueness guard as `create`, PLUS a caller-authorization check (systemAdmin / adminUnit-matching-target-personnel's-unit / user-only-for-their-own-personnelId — a regular user can self-attach a vehicle to their own record but not someone else's). Delegates to `create()` internally after authorization passes.
  - `update(id, request)` — updates fields; if `imagePaths` shrinks, delete the removed S3 objects.
  - `getById(id)` / `getByPersonnelId(personnelId)` — throw `VEHICLE_NOT_FOUND` if missing.
  - `list(keyword, pageable)` — paginated, scoped by caller's `AccessScope`, searchable by plate/brand/model.
  - `delete(id)` — deletes the vehicle row + all its S3 images.
  - `deleteByPersonnelId(personnelId)` — cascade helper, no-op (not an error) if the personnel has no vehicle.
  - `deleteImage(id, imagePath)` — removes one path from the list + deletes that one S3 object; throws `VEHICLE_IMAGE_NOT_FOUND` if the path isn't in the vehicle's list.
  - `storeImage(MultipartFile)` / `loadImage(filename)` — same S3 store/load contract as `MilitaryUnitServiceImpl.storeLogo/loadLogo`, called by `CommonServiceImpl` for the `category=vehicle` upload/download path (wired in Phase 3).
- Non-functional: mirror `MilitaryUnitServiceImpl`'s S3 key-building / extension-extraction / prefix-normalization helpers verbatim (already proven, no need to reinvent).

## Architecture

```
VehicleController (Phase 3) --> VehicleService --> VehicleRepository (Phase 1)
                                      |
                                      +--> S3Client (store/load/delete image bytes)
                                      |
                                      +--> MilitaryPersonnelRepository (resolve unitCode for AccessScope)
                                      +--> UserRepository (resolve current user's personnelId for AccessScope)

CommonServiceImpl (Phase 3) --> VehicleService.storeImage() / loadImage()
MilitaryPersonnelServiceImpl (Phase 4) --> VehicleService.create() / deleteByPersonnelId() / getByPersonnelId()
```

### Access control (mirrors `MilitaryPersonnelServiceImpl.AccessScope`)

Same three-tier scope, resolved the same way (`SecurityContextHolder` → `UserDetailsImpl` → `UserRepository` → `MilitaryPersonnelRepository`):
- `systemAdmin` — full access to all vehicles.
- `adminUnit` — access to vehicles whose owning personnel's `unitCode` matches the admin's own personnel's `unitCode` (requires a `militaryPersonnelRepository.findById(vehicle.getPersonnelId())` lookup to get that personnel's `unitCode`).
- `user` — access only to their own vehicle (`vehicle.getPersonnelId().equals(currentPersonnelId)`).

This duplicates the `AccessScope` resolution logic already present in `MilitaryPersonnelServiceImpl`/`AuthServiceImpl`/`CommonServiceImpl` rather than extracting a shared utility — matches this codebase's existing convention (each service resolves its own scope independently; there is no shared `AccessScopeResolver` anywhere in the current code, so introducing one here would be an unplanned refactor of unrelated services). Flagged as a known duplication, not a defect.

> **[Red Team applied — Finding 1, Critical]** Every method below MUST call a concrete `canAccessVehicle(AccessScope scope, Vehicle vehicle)` helper, not a vague "authorize via AccessScope." Without a named, explicit boolean check mirroring `MilitaryPersonnelServiceImpl.canAccessPersonnel()` (`src/main/java/com/military/service/impl/MilitaryPersonnelServiceImpl.java:393`), an under-specified implementation risks an IDOR: a `user`-tier caller hitting `GET /api/vehicles/{id}` with someone else's ID could read another person's vehicle + license plate. Add this exact helper:
> ```java
> private boolean canAccessVehicle(AccessScope scope, Vehicle vehicle) {
>   if (scope.systemAdmin()) {
>     return true;
>   }
>   if (scope.adminUnit()) {
>     MilitaryPersonnel owner = militaryPersonnelRepository.findById(vehicle.getPersonnelId()).orElse(null);
>     return owner != null && scope.unitCode() != null && scope.unitCode().equalsIgnoreCase(owner.getUnitCode());
>   }
>   return scope.currentPersonnelId() != null && scope.currentPersonnelId().equals(vehicle.getPersonnelId());
> }
> ```
> Call it (throwing `ErrorCode.UNAUTHORIZED` on `false`) from `getById`, `getByPersonnelId`, `update`, `deleteImage`, and `delete` — every method that takes a vehicle ID or personnel ID from the caller.

## Related Code Files

**Create:**
- `src/main/java/com/military/service/dto/VehicleImage.java` — `record VehicleImage(String filename, String contentType, byte[] content)`, mirrors `PersonnelImage`/`UnitLogo`.
- `src/main/java/com/military/service/VehicleService.java`
- `src/main/java/com/military/service/impl/VehicleServiceImpl.java`

**Modify:** none in this phase (wiring into `CommonServiceImpl`/`MilitaryPersonnelServiceImpl` happens in Phases 3-4).

## Implementation Steps

1. **`VehicleImage.java`**:
   ```java
   package com.military.service.dto;

   public record VehicleImage(String filename, String contentType, byte[] content) {
   }
   ```

2. **`VehicleService.java`** (interface):
   ```java
   package com.military.service;

   import com.military.payload.request.VehicleRequest;
   import com.military.payload.response.VehicleResponse;
   import com.military.service.dto.VehicleImage;
   import org.springframework.data.domain.Page;
   import org.springframework.data.domain.Pageable;
   import org.springframework.web.multipart.MultipartFile;

   public interface VehicleService {
     VehicleResponse create(Long personnelId, VehicleRequest request);
     VehicleResponse attachToPersonnel(Long personnelId, VehicleRequest request);
     VehicleResponse update(Long id, VehicleRequest request);
     VehicleResponse getById(Long id);
     VehicleResponse getByPersonnelId(Long personnelId);
     Page<VehicleResponse> list(String keyword, Pageable pageable);
     void delete(Long id);
     void deleteByPersonnelId(Long personnelId);
     void deleteImage(Long id, String imagePath);
     String storeImage(MultipartFile imageFile);
     VehicleImage loadImage(String filename);
   }
   ```

3. **`VehicleServiceImpl.java`** — structure mirrors `MilitaryUnitServiceImpl` + adds the `AccessScope` resolution from `MilitaryPersonnelServiceImpl`:
   - Constructor deps: `VehicleRepository`, `MilitaryPersonnelRepository`, `UserRepository`, `S3Client`, `@Value("${military.app.s3.bucket}") bucketName`, `@Value("${military.app.s3.vehicle-prefix:military-vehicles}") objectPrefix`.
   - `IMAGE_ENDPOINT = "/api/common/images/vehicle/"` (matches `CommonController`'s `/api/common/images/{category}/{filename}` route).
   - `create(personnelId, request)` (internal, no auth check):
     ```java
     if (vehicleRepository.findByPersonnelId(personnelId).isPresent()) {
       throw new AppException(ErrorCode.VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL);
     }
     Vehicle vehicle = new Vehicle(request);
     vehicle.setPersonnelId(personnelId);
     Vehicle saved = vehicleRepository.save(vehicle);
     return toResponse(saved);
     ```
   - `attachToPersonnel(personnelId, request)` (public, authorized):
     ```java
     MilitaryPersonnel targetPersonnel = militaryPersonnelRepository.findById(personnelId)
         .orElseThrow(() -> new AppException(ErrorCode.PERSONNEL_NOT_FOUND));
     AccessScope scope = resolveAccessScope(); // throws UNAUTHORIZED if no authenticated principal
     if (!scope.systemAdmin()) {
       boolean sameUnit = scope.adminUnit() && scope.unitCode() != null
           && scope.unitCode().equalsIgnoreCase(targetPersonnel.getUnitCode());
       boolean isSelf = scope.currentPersonnelId() != null && scope.currentPersonnelId().equals(personnelId);
       if (!sameUnit && !isSelf) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
       }
     }
     return create(personnelId, request);
     ```
     > **[Red Team applied — decision revised]** Originally `create()` had no uniqueness guard, reasoning it would only ever be called once, internally, from `MilitaryPersonnelServiceImpl.create()`. That assumption no longer holds: per the user's decision (see `plan.md` Decisions Log), vehicle is now **optional** at personnel-creation time, and Phase 3 adds a public endpoint so a vehicle can be attached later to a personnel who didn't get one at creation. Splitting into an unauthenticated-safe `create()` (for the internal/signup call site, which may run with no `AccessScope`) and an authorized `attachToPersonnel()` (for the new public endpoint) avoids breaking the signup flow while still enforcing the 1:1 invariant and caller authorization on the public path.
   - `update(id, request)`:
     - Load existing vehicle (`findEntityById`, throws `VEHICLE_NOT_FOUND`).
     - Authorize via `AccessScope` (same shape as `MilitaryUnitServiceImpl` has NO access check today — but `Vehicle` carries personal data tied to a specific user, so add one; systemAdmin/adminUnit-matching-owner-unit/user-owns-vehicle, else `UNAUTHORIZED`).
     - Compute `List<String> oldPaths = vehicle.getImagePaths()`; apply new fields (`vehicleType`, `brand`, `model`, `licensePlate`, `imagePaths`) from `request`; save.
     - Diff: for each path in `oldPaths` not present in `request.getImagePaths()`, call `deleteImageObjectIfExists(path)` (S3 cleanup) — same pattern as `MilitaryUnitServiceImpl.update()`'s single-`oldLogoPath` diff, generalized to a list.
   - `getById(id)` — `findEntityById` (throws `VEHICLE_NOT_FOUND`), authorize via `AccessScope`, `toResponse`.
   - `getByPersonnelId(personnelId)` — `vehicleRepository.findByPersonnelId(personnelId).orElseThrow(() -> new AppException(ErrorCode.VEHICLE_NOT_FOUND))`, authorize, `toResponse`. (Phase 4's embed-in-personnel-response call site will catch this exception and treat it as "no vehicle" rather than propagating — see Phase 4.)
   - `list(keyword, pageable)` — resolve `AccessScope`; if `systemAdmin`, unrestricted `findAll`/keyword-search; if `adminUnit`, filter `findAllList()` by personnel-unit match (scan-then-filter, consistent with `MilitaryPersonnelServiceImpl.list()`'s existing approach); if `user`, return only their own vehicle (0 or 1 result) — mirror `MilitaryPersonnelServiceImpl.list()`'s `canAccessPersonnel` filter pattern applied to vehicles instead.
   - `delete(id)` — load vehicle, authorize (systemAdmin or adminUnit-matching-owner-unit only — regular users should not be able to delete their own vehicle outright; note as an assumption to confirm), delete all S3 images via `imagePaths`, `vehicleRepository.delete(vehicle)`.
   - `deleteByPersonnelId(personnelId)` — `vehicleRepository.findByPersonnelId(personnelId)`; if present, delete its S3 images + row; if absent, no-op (do NOT throw — called from personnel-delete cascade in Phase 4, and a personnel record predating this feature may have no vehicle).
   - `deleteImage(id, imagePath)` — load vehicle, authorize (same tiers as `update`), verify `imagePath` is in `vehicle.getImagePaths()` (else `VEHICLE_IMAGE_NOT_FOUND`), remove from list, delete S3 object, save.
   - `storeImage`/`loadImage` — copy `MilitaryUnitServiceImpl.storeLogo`/`loadLogo` byte-for-byte, renaming error codes to the `VEHICLE_IMAGE_*` ones from Phase 1, and returning `VehicleImage` instead of `UnitLogo`.
   - Private helpers: `extractExtension`, `buildObjectKey`, `normalizePrefix`, `deleteObjectIfExists(String path)` — copy from `MilitaryUnitServiceImpl` verbatim (same S3 delete-with-404-swallow logic), throwing `VEHICLE_IMAGE_DELETE_FAILED` on unexpected S3 errors.
   - `toResponse(Vehicle vehicle)` — `new VehicleResponse(vehicle)` then `response.setImageUrls(vehicle.getImagePaths() == null ? List.of() : vehicle.getImagePaths().stream().map(p -> IMAGE_ENDPOINT + p).toList())`.

## Success Criteria

- [ ] `VehicleServiceImpl` compiles, implements every `VehicleService` method
- [ ] `update()` correctly deletes S3 objects for images removed from the list (verified by code review — no automated test suite in this repo)
- [ ] `deleteByPersonnelId()` is a safe no-op when no vehicle exists (verified by code review — this is exercised by Phase 4's cascade-delete call site)
- [ ] Access control tiers (systemAdmin/adminUnit/user) match the shape already established in `MilitaryPersonnelServiceImpl.AccessScope` — no new authorization model invented

## Risk Assessment

- **[Post-implementation code review applied]** The original `create()` design (check `findByPersonnelId()` then `save()`) had a TOCTOU race: two concurrent calls could both pass the existence check before either saved, producing two vehicles for one personnel. Fixed by making `Vehicle.id` always equal `Vehicle.personnelId` and adding `VehicleRepository.createForPersonnel()`, which does an atomic DynamoDB conditional `PutItem` (`attribute_not_exists(id)`), translated to `VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL` on `ConditionalCheckFailedException`. As a side effect, `findByPersonnelId()` became an O(1) direct `getItem` instead of a table scan (previously flagged as a code-review finding on the N+1 scan cost in `MilitaryPersonnelServiceImpl.toResponse()` — this narrows that cost from "N full table scans" to "N direct key lookups," though `VehicleServiceImpl.list()`'s `adminUnit`-scope filtering still scans, left as accepted pre-existing-pattern debt per user decision).
- **Duplicated `AccessScope` resolution logic** across `VehicleServiceImpl`, `MilitaryPersonnelServiceImpl`, `AuthServiceImpl`, `CommonServiceImpl` — pre-existing codebase pattern, not introduced by this phase. Extracting a shared resolver is out of scope (would touch 3 unrelated existing services).
- **`delete(id)` restricted to systemAdmin/adminUnit** (regular users can't delete their own vehicle) is an assumption, not explicitly specified by the user — confirm during Phase 4 review or adjust before shipping if users should be able to self-delete.
- **S3 cleanup on `update()` diff** — if the S3 delete call fails mid-diff (partial failure across multiple removed images), the vehicle row is still saved with the new list; a stray S3 object may remain (orphaned but harmless — same risk profile as the existing single-image `oldLogoPath`/`oldImagePath` pattern elsewhere in the codebase, just multiplied across N images instead of 1).
