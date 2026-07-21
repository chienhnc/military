---
phase: 3
title: Vehicle Controller & API
status: completed
priority: P1
effort: 2h
dependencies:
  - 2
---

# Phase 3: Vehicle Controller & API

## Overview

Expose `VehicleService` via a new `VehicleController` (`/api/vehicles`) for list/search/get/update/delete/delete-one-image, and wire `category=vehicle` into the existing generic `CommonController`/`CommonServiceImpl` upload/download endpoints so the frontend can upload vehicle images through the same mechanism it already uses for personnel/unit images.

## Requirements

- Functional:
  - `POST /api/vehicles?personnelId={id}` — attach a vehicle to a personnel who doesn't have one yet. **[Red Team applied — decision revised]** Originally omitted deliberately (vehicle was required-at-creation, so no standalone create path was needed). Per the user's decision (see `plan.md` Decisions Log), vehicle is now optional at personnel-creation time, so this endpoint is required to let a personnel attach a vehicle later. Backed by `VehicleService.attachToPersonnel()` (Phase 2), which enforces the 1:1 invariant (`VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL` if one already exists) and caller authorization.
  - `GET /api/vehicles` — paginated list + keyword search (plate/brand/model).
  - `GET /api/vehicles/{id}` — detail.
  - `GET /api/vehicles/by-personnel/{personnelId}` — convenience lookup by owning personnel; this data is also embedded in `MilitaryPersonnelResponse.vehicle` (Phase 4). Kept as a low-cost convenience method, not because a specific consumer was validated — don't over-justify its necessity.
  - `PUT /api/vehicles/{id}` — update fields and/or replace the image list.
  - `DELETE /api/vehicles/{id}/images` — remove one image (query param `imagePath`).
  - `DELETE /api/vehicles/{id}` — delete the vehicle entirely.
  - `POST /api/common/upload-image?category=vehicle` and `GET /api/common/images/vehicle/{filename}` — reuse the existing generic endpoints.
- Non-functional: controller style matches `MilitaryUnitController.java` exactly (Swagger annotations, `BaseResponse.of(...)` wrapping, `HttpServletRequest` param for `servletPath`).

## Architecture

```
VehicleController  ──┬── POST   /api/vehicles?personnelId={id}     (attach to existing personnel)
                      ├── GET    /api/vehicles                    (list)
                      ├── GET    /api/vehicles/{id}                (detail)
                      ├── GET    /api/vehicles/by-personnel/{id}    (by owner)
                      ├── PUT    /api/vehicles/{id}                 (update)
                      ├── DELETE /api/vehicles/{id}/images          (remove one image)
                      └── DELETE /api/vehicles/{id}                 (delete)

CommonController      ── POST /api/common/upload-image?category=vehicle  → CommonServiceImpl → VehicleService.storeImage()
                       ── GET  /api/common/images/vehicle/{filename}     → CommonServiceImpl → VehicleService.loadImage()
```

No new Spring Security rules needed — `WebSecurityConfig`'s existing `anyRequest().authenticated()` (plus the already-fixed global CORS bean from the earlier CORS fix) covers `/api/vehicles/**` automatically; `/api/common/upload-image` and `/api/common/images/**` are already permitted/authenticated per the existing rules (no path-specific change required since `category` is a query/path param, not a route).

## Related Code Files

**Create:**
- `src/main/java/com/military/controllers/VehicleController.java`

**Modify:**
- `src/main/java/com/military/service/CommonService.java` — no interface signature change needed (category is already a runtime string param on `uploadImage`/`loadImage`)
- `src/main/java/com/military/service/impl/CommonServiceImpl.java` — add `CATEGORY_VEHICLE`, inject `VehicleService`, wire into `uploadImage`/`loadImage`/`normalizeCategory`
- `src/main/java/com/military/controllers/CommonController.java` — update Swagger doc strings ("personnel, unit" → "personnel, unit, vehicle")

## Implementation Steps

1. **`VehicleController.java`** — mirror `MilitaryUnitController.java` structure:
   ```java
   package com.military.controllers;

   import com.military.payload.request.VehicleRequest;
   import com.military.payload.response.BaseResponse;
   import com.military.payload.response.VehicleResponse;
   import com.military.service.VehicleService;
   import io.swagger.v3.oas.annotations.Operation;
   import io.swagger.v3.oas.annotations.Parameter;
   import io.swagger.v3.oas.annotations.tags.Tag;
   import jakarta.servlet.http.HttpServletRequest;
   import jakarta.validation.Valid;
   import jakarta.validation.constraints.Min;
   import jakarta.validation.constraints.NotBlank;
   import org.springframework.data.domain.Page;
   import org.springframework.data.domain.PageRequest;
   import org.springframework.data.domain.Sort;
   import org.springframework.http.ResponseEntity;
   import org.springframework.validation.annotation.Validated;
   import org.springframework.web.bind.annotation.*;

   @Validated
   @RestController
   @RequestMapping("/api/vehicles")
   @Tag(name = "Vehicle", description = "API quan ly phuong tien cua quan nhan: danh sach, chi tiet, sua, xoa anh, xoa")
   public class VehicleController {
     private final VehicleService vehicleService;

     public VehicleController(VehicleService vehicleService) {
       this.vehicleService = vehicleService;
     }

     @PostMapping
     @Operation(summary = "Gan phuong tien cho quan nhan chua co phuong tien")
     public ResponseEntity<BaseResponse<VehicleResponse>> attach(@RequestParam Long personnelId,
                                                                  @Valid @RequestBody VehicleRequest request,
                                                                  HttpServletRequest httpRequest) {
       VehicleResponse response = vehicleService.attachToPersonnel(personnelId, request);
       return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
     }

     @GetMapping
     @Operation(summary = "Danh sach phuong tien co phan trang")
     public ResponseEntity<BaseResponse<Page<VehicleResponse>>> list(
         @RequestParam(defaultValue = "0") @Min(0) int page,
         @RequestParam(defaultValue = "10") @Min(1) int size,
         @RequestParam(required = false) String keyword,
         HttpServletRequest request) {
       PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
       Page<VehicleResponse> response = vehicleService.list(keyword, pageRequest);
       return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
     }

     @GetMapping("/{id}")
     @Operation(summary = "Chi tiet phuong tien")
     public ResponseEntity<BaseResponse<VehicleResponse>> detail(@PathVariable Long id, HttpServletRequest request) {
       VehicleResponse response = vehicleService.getById(id);
       return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
     }

     @GetMapping("/by-personnel/{personnelId}")
     @Operation(summary = "Lay phuong tien theo id quan nhan")
     public ResponseEntity<BaseResponse<VehicleResponse>> byPersonnel(@PathVariable Long personnelId,
                                                                       HttpServletRequest request) {
       VehicleResponse response = vehicleService.getByPersonnelId(personnelId);
       return ResponseEntity.ok(BaseResponse.of(200, response, request.getServletPath()));
     }

     @PutMapping("/{id}")
     @Operation(summary = "Cap nhat phuong tien")
     public ResponseEntity<BaseResponse<VehicleResponse>> update(@PathVariable Long id,
                                                                  @Valid @RequestBody VehicleRequest request,
                                                                  HttpServletRequest httpRequest) {
       VehicleResponse response = vehicleService.update(id, request);
       return ResponseEntity.ok(BaseResponse.of(200, response, httpRequest.getServletPath()));
     }

     @DeleteMapping("/{id}/images")
     @Operation(summary = "Xoa mot anh phuong tien")
     public ResponseEntity<BaseResponse<String>> deleteImage(@PathVariable Long id,
                                                              @RequestParam @NotBlank String imagePath,
                                                              HttpServletRequest request) {
       vehicleService.deleteImage(id, imagePath);
       return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
     }

     @DeleteMapping("/{id}")
     @Operation(summary = "Xoa phuong tien")
     public ResponseEntity<BaseResponse<String>> delete(@PathVariable Long id, HttpServletRequest request) {
       vehicleService.delete(id);
       return ResponseEntity.ok(BaseResponse.of(200, "Deleted successfully", request.getServletPath()));
     }
   }
   ```
   (No `@CrossOrigin` — matches the codebase-wide removal already done for the CORS fix; CORS is centralized in `WebSecurityConfig`.)

2. **`CommonServiceImpl.java`** changes:
   - Add `private static final String CATEGORY_VEHICLE = "vehicle";`
   - Add `private final VehicleService vehicleService;` + constructor param + assignment.
   - `uploadImage()` switch: add `case CATEGORY_VEHICLE -> vehicleService.storeImage(multipartFile);`
   - `loadImage()`: add a branch `if (CATEGORY_VEHICLE.equals(normalizedCategory)) { VehicleImage image = vehicleService.loadImage(filename); return new CommonImage(image.filename(), image.contentType(), image.content()); }`
   - `normalizeCategory()`: add `|| CATEGORY_VEHICLE.equals(normalized)` to the allowed-set check.

3. **`CommonController.java`** — update the two `@Operation`/`@Parameter` description strings that currently say "category ho tro: personnel, unit" to "category ho tro: personnel, unit, vehicle".

4. **`ErrorCode.java`** message for `COMMON_INVALID_FILE_CATEGORY` — update its message text in `messages.properties`/`messages_vi.properties` from "Supported values: personnel, unit." to "Supported values: personnel, unit, vehicle." (both locales).

## Success Criteria

- [ ] `VehicleController` compiles and exposes exactly the 7 endpoints listed above (including `POST /api/vehicles?personnelId={id}`)
- [ ] `POST /api/vehicles?personnelId={id}` returns `400 VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL` if called twice for the same personnel
- [ ] `POST /api/common/upload-image?category=vehicle` compiles/wires to `VehicleService.storeImage`
- [ ] `GET /api/common/images/vehicle/{filename}` compiles/wires to `VehicleService.loadImage`
- [ ] No `@CrossOrigin` annotation added to `VehicleController` (consistency with the centralized-CORS fix already shipped)

## Risk Assessment

- **`CommonServiceImpl` now depends on 3 services** (`MilitaryPersonnelService`, `MilitaryUnitService`, `VehicleService`) — same pattern as before, just one more constructor param. Low risk, mechanical change.
- **No dedicated batch-upload endpoint** — per the confirmed scope decision, the frontend uploads vehicle images one at a time via the existing `/api/common/upload-image` (same as it already does for personnel/unit images) and collects the returned filenames client-side into the `imagePaths` array it sends to `PUT /api/vehicles/{id}` (or embeds in `MilitaryPersonnelRequest.vehicle.imagePaths` at creation time, per Phase 4). This means a partial upload failure (e.g. 3 of 5 images succeed) is a frontend concern, not something this backend phase needs to handle transactionally.
- **[Red Team applied — Finding 2, Medium]** `/api/common/images/**` is `permitAll()` in `WebSecurityConfig.java:108` — vehicle photos (and any visible license plate) are publicly readable by anyone who obtains a filename, same as personnel/unit images today. Not a new regression (inherited from the existing generic image-serving design), but flagged here as a known, accepted exposure for a newly-added, arguably more sensitive data category. Not fixed in this plan — would require redesigning image auth for all 3 categories, out of scope.
- **[Red Team applied — Finding 5, Medium]** Nothing stops a client from calling `/api/common/upload-image?category=vehicle` more times than images it ultimately references (the `@Size(max=10)` cap in Phase 1 only bounds what's included in a submitted `VehicleRequest`). Abandoned uploads become orphaned S3 objects — the same weak pattern personnel/unit single-image upload already has today, but the multi-image design multiplies the potential volume roughly 10x per vehicle. Accepted as a pre-existing pattern, not fixed in this plan.
