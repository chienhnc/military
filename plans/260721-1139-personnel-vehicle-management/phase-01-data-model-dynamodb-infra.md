---
phase: 1
title: Data Model & DynamoDB Infra
status: completed
priority: P1
effort: 3h
dependencies: []
---

# Phase 1: Data Model & DynamoDB Infra

## Overview

Create the `Vehicle` domain model, its DynamoDB item bean, repository (interface + impl), request/response DTOs, the `EVehicleType` enum, and the supporting infra (new DynamoDB table in `template.yaml`, new config in `application.properties`, new `ErrorCode` entries + messages). This phase produces no wired behavior yet — it's the foundation Phases 2-4 build on.

## Requirements

- Functional: `Vehicle` has `id`, `personnelId` (FK), `vehicleType`, `brand`, `model`, `licensePlate`, `imagePaths` (list of S3 filenames).
- Non-functional: follow existing codebase conventions exactly — mirror `MilitaryUnit`/`MilitaryUnitItem`/`MilitaryUnitRepository(Impl)` file-for-file, since that's the closest existing precedent (simple CRUD entity + S3-backed image, DynamoDB scan-based repository, no GSI — same as every other repository in this codebase).

## Architecture

```
Vehicle (model)  <---- VehicleRequest (payload/request)
   |                        ^
   v                        |
VehicleItem (DynamoDB bean) |
   ^                        |
   |                  VehicleResponse (payload/response)
VehicleRepository (interface) -- VehicleRepositoryImpl (DynamoDbTable<VehicleItem>)
```

- Table: `military_vehicles` (partition key `id`, type N) — same shape as `military_units`/`military_personnel`.
- `imagePaths` is a plain `List<String>` field on `VehicleItem` — DynamoDB Enhanced Client maps `List<String>` natively via getter/setter, no extra annotation needed (verified against how `MilitaryUnitItem`/`MilitaryPersonnelItem` map their scalar fields).
- `vehicleType` stored as `String` (enum `.name()`) in `VehicleItem`, parsed back via `EVehicleType.valueOf(...)` in `toModel()` — mirrors `MilitaryPersonnelRepositoryImpl.parseRank()`/`parsePosition()` (swallow `IllegalArgumentException`, return `null` on bad data rather than throwing).

## Related Code Files

**Create:**
- `src/main/java/com/military/models/EVehicleType.java`
- `src/main/java/com/military/models/Vehicle.java`
- `src/main/java/com/military/repository/dynamodb/item/VehicleItem.java`
- `src/main/java/com/military/repository/VehicleRepository.java`
- `src/main/java/com/military/repository/dynamodb/VehicleRepositoryImpl.java`
- `src/main/java/com/military/payload/request/VehicleRequest.java`
- `src/main/java/com/military/payload/response/VehicleResponse.java`

**Modify:**
- `src/main/java/com/military/exception/ErrorCode.java` — add 5 new codes (see below)
- `src/main/resources/messages.properties` / `messages_vi.properties` — matching message keys
- `template.yaml` — new `VehiclesTable` resource + IAM policy + env var
- `src/main/resources/application.properties` — new table + S3 prefix config

## Implementation Steps

1. **`EVehicleType.java`** — mirror `EMilitaryRank.java` exactly:
   ```java
   package com.military.models;

   public enum EVehicleType {
     CAR("Ô tô"),
     MOTORBIKE("Xe máy"),
     OTHER("Khác");

     private final String name;

     EVehicleType(String name) {
       this.name = name;
     }

     public String getCode() {
       return name();
     }

     public String getName() {
       return name;
     }
   }
   ```

2. **`Vehicle.java`** (model) — mirror `MilitaryUnit.java` structure:
   ```java
   package com.military.models;

   import com.military.payload.request.VehicleRequest;
   import lombok.Data;
   import lombok.NoArgsConstructor;
   import org.springframework.beans.BeanUtils;

   import java.util.List;

   @Data
   @NoArgsConstructor
   public class Vehicle {
     private Long id;
     private Long personnelId;
     private EVehicleType vehicleType;
     private String brand;
     private String model;
     private String licensePlate;
     private List<String> imagePaths;

     public Vehicle(VehicleRequest request) {
       BeanUtils.copyProperties(request, this);
     }
   }
   ```
   Note: `BeanUtils.copyProperties` will NOT copy `personnelId` (not present on `VehicleRequest` — it's set explicitly by the service after construction, since personnelId comes from the URL/creation-context, not client input).

3. **`VehicleItem.java`** — mirror `MilitaryUnitItem.java`'s explicit getter/setter style (this codebase does NOT use Lombok on `@DynamoDbBean` classes):
   ```java
   package com.military.repository.dynamodb.item;

   import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
   import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

   import java.util.List;

   @DynamoDbBean
   public class VehicleItem {
     private Long id;
     private Long personnelId;
     private String vehicleType;
     private String brand;
     private String model;
     private String licensePlate;
     private List<String> imagePaths;

     @DynamoDbPartitionKey
     public Long getId() { return id; }
     public void setId(Long id) { this.id = id; }

     public Long getPersonnelId() { return personnelId; }
     public void setPersonnelId(Long personnelId) { this.personnelId = personnelId; }

     public String getVehicleType() { return vehicleType; }
     public void setVehicleType(String vehicleType) { this.vehicleType = vehicleType; }

     public String getBrand() { return brand; }
     public void setBrand(String brand) { this.brand = brand; }

     public String getModel() { return model; }
     public void setModel(String model) { this.model = model; }

     public String getLicensePlate() { return licensePlate; }
     public void setLicensePlate(String licensePlate) { this.licensePlate = licensePlate; }

     public List<String> getImagePaths() { return imagePaths; }
     public void setImagePaths(List<String> imagePaths) { this.imagePaths = imagePaths; }
   }
   ```

4. **`VehicleRepository.java`** (interface):
   ```java
   package com.military.repository;

   import com.military.models.Vehicle;
   import org.springframework.data.domain.Page;
   import org.springframework.data.domain.Pageable;
   import org.springframework.stereotype.Repository;

   import java.util.List;
   import java.util.Optional;

   @Repository
   public interface VehicleRepository {
     Vehicle save(Vehicle vehicle);
     Optional<Vehicle> findById(Long id);
     Optional<Vehicle> findByPersonnelId(Long personnelId);
     Page<Vehicle> findAll(Pageable pageable);
     void delete(Vehicle vehicle);
     List<Vehicle> findAllList();
     Page<Vehicle> findByLicensePlateContainingIgnoreCaseOrBrandContainingIgnoreCaseOrModelContainingIgnoreCase(
         String plateKeyword, String brandKeyword, String modelKeyword, Pageable pageable);
   }
   ```

5. **`VehicleRepositoryImpl.java`** — mirror `MilitaryUnitRepositoryImpl.java` verbatim structure: constructor takes `DynamoDbEnhancedClient` + `@Value("${military.app.dynamodb.tables.vehicles:military_vehicles}")` table name; `save()` generates ID via `DynamoIdGenerator.nextId()` when `item.getId() == null` (same collision-retry loop as `MilitaryUnitRepositoryImpl.generateUniqueId()`); `findByPersonnelId` and the 3-field keyword search both use `table.scan().items().stream().filter(...)` — same scan-based approach as every other repository in this codebase (no GSI; consistent with existing tech debt, not a regression).
   - `toItem()`/`toModel()` handle the `vehicleType` enum↔string conversion, swallowing `IllegalArgumentException` on parse (return `null`) exactly like `MilitaryPersonnelRepositoryImpl.parseRank()`.

6. **`VehicleRequest.java`**:
   ```java
   package com.military.payload.request;

   import com.military.models.EVehicleType;
   import io.swagger.v3.oas.annotations.media.Schema;
   import jakarta.validation.constraints.NotBlank;
   import jakarta.validation.constraints.NotNull;
   import jakarta.validation.constraints.Size;
   import lombok.Data;

   import java.util.List;

   @Data
   @Schema(description = "Thong tin phuong tien cua quan nhan")
   public class VehicleRequest {
     @Schema(description = "Loai xe", example = "CAR")
     @NotNull
     private EVehicleType vehicleType;

     @Schema(description = "Hang xe", example = "Toyota")
     @NotBlank
     @Size(max = 100)
     private String brand;

     @Schema(description = "Hieu xe", example = "Corolla")
     @NotBlank
     @Size(max = 100)
     private String model;

     @Schema(description = "Bien so xe", example = "51H-123.45")
     @NotBlank
     @Size(max = 20)
     private String licensePlate;

     @Schema(description = "Danh sach ten file anh da upload qua /api/common/upload-image?category=vehicle (toi da 10 anh)")
     @Size(max = 10)
     private List<@Size(max = 255) String> imagePaths;
   }
   ```
   Cap of 10 images is a reasonable default limit to prevent abuse — flagged as an assumption; adjust the `@Size(max = 10)` constant if the user wants a different cap.

7. **`VehicleResponse.java`**:
   ```java
   package com.military.payload.response;

   import com.military.models.EVehicleType;
   import com.military.models.Vehicle;
   import lombok.Data;
   import org.springframework.beans.BeanUtils;

   import java.util.List;

   @Data
   public class VehicleResponse {
     private Long id;
     private Long personnelId;
     private EVehicleType vehicleType;
     private String brand;
     private String model;
     private String licensePlate;
     private List<String> imageUrls;

     public VehicleResponse(Vehicle vehicle) {
       BeanUtils.copyProperties(vehicle, this);
     }
   }
   ```
   `imageUrls` is populated in the service layer (`VehicleServiceImpl.toResponse()`), NOT by the constructor — `BeanUtils.copyProperties` will not touch it since `Vehicle` has `imagePaths` not `imageUrls` (property name mismatch is intentional, matching how `MilitaryUnitResponse.logoUrl` is set post-construction in `MilitaryUnitServiceImpl.toResponse()`).

8. **`ErrorCode.java`** — append after `QR_SCAN_CITIZEN_ACTION_ONLY("MIL00049", ...)`:
   ```java
   VEHICLE_NOT_FOUND("MIL00050", "error.vehicle.not_found", HttpStatus.NOT_FOUND),
   VEHICLE_IMAGE_NOT_FOUND("MIL00051", "error.vehicle.image_not_found", HttpStatus.NOT_FOUND),
   VEHICLE_IMAGE_SAVE_FAILED("MIL00052", "error.vehicle.image_save_failed", HttpStatus.INTERNAL_SERVER_ERROR),
   VEHICLE_IMAGE_DELETE_FAILED("MIL00053", "error.vehicle.image_delete_failed", HttpStatus.INTERNAL_SERVER_ERROR),
   VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL("MIL00054", "error.vehicle.already_exists_for_personnel", HttpStatus.BAD_REQUEST);
   ```
   (Remember to move the terminating `;` from the previous last entry to this new last entry.)

   > **[Red Team applied]** Dropped `PERSONNEL_VEHICLE_REQUIRED` — user confirmed (see `plan.md` Decisions Log) that vehicle is **optional** at personnel-creation time, for both admin-created and self-registered (signup) personnel; no distinct caller-context branching. Added `VEHICLE_ALREADY_EXISTS_FOR_PERSONNEL` instead, needed now that Phase 3 exposes a public `POST /api/vehicles` to attach a vehicle to an existing personnel who doesn't have one yet — this code guards the 1:1 invariant on that new path (and on the internal creation-time path too).

9. **`messages.properties`** — append:
   ```properties
   error.vehicle.not_found=Vehicle is not found.
   error.vehicle.image_not_found=Vehicle image is not found.
   error.vehicle.image_save_failed=Cannot save vehicle image.
   error.vehicle.image_delete_failed=Cannot delete vehicle image.
   error.vehicle.already_exists_for_personnel=This military personnel already has a vehicle.
   ```
   **`messages_vi.properties`** — append:
   ```properties
   error.vehicle.not_found=Không tìm thấy phương tiện.
   error.vehicle.image_not_found=Không tìm thấy ảnh phương tiện.
   error.vehicle.image_save_failed=Không thể lưu ảnh phương tiện.
   error.vehicle.image_delete_failed=Không thể xóa ảnh phương tiện.
   error.vehicle.already_exists_for_personnel=Quân nhân này đã có phương tiện.
   ```

10. **`template.yaml`** — add a new table resource (place it near `UnitsTable`):
    ```yaml
    VehiclesTable:
      Type: AWS::DynamoDB::Table
      Properties:
        TableName: military_vehicles
        BillingMode: PAY_PER_REQUEST
        AttributeDefinitions:
          - AttributeName: id
            AttributeType: N
        KeySchema:
          - AttributeName: id
            KeyType: HASH
    ```
    Add `- !GetAtt VehiclesTable.Arn` to the `MilitaryApiFunction` IAM policy's DynamoDB `Resource` list (alongside `PersonnelTable.Arn`, `UnitsTable.Arn`, etc.).
    Add `DYNAMODB_VEHICLES_TABLE: !Ref VehiclesTable` to the `Environment.Variables` block.
    Update `DEPLOY_LAMBDA.md`'s auto-created-tables list to include `military_vehicles`.

11. **`application.properties`** — add:
    ```properties
    military.app.dynamodb.tables.vehicles=${DYNAMODB_VEHICLES_TABLE:military_vehicles}
    military.app.s3.vehicle-prefix=${S3_VEHICLE_PREFIX:military-vehicles}
    ```

## Success Criteria

- [ ] `EVehicleType`, `Vehicle`, `VehicleItem`, `VehicleRequest`, `VehicleResponse` compile with no errors
- [ ] `VehicleRepository`/`VehicleRepositoryImpl` compile and follow the exact same structure as `MilitaryUnitRepository`/`MilitaryUnitRepositoryImpl`
- [ ] `template.yaml` changeset (dry-run via `sam deploy` in Phase 5) shows only an *addition* of `VehiclesTable`, no unintended changes to other resources
- [ ] `mvn compile` succeeds with JDK 17 (see Phase 5)

## Risk Assessment

- **Scan-based queries don't scale** (`findByPersonnelId`, keyword search) — matches every other repository in this codebase already (`MilitaryUnitRepositoryImpl`, `MilitaryPersonnelRepositoryImpl` are all `table.scan()`-based). Not a new regression, just consistent with existing tech debt. Not worth introducing a GSI for a single new table when the rest of the codebase doesn't use one.
- **`imagePaths` as a bare `List<String>`** — DynamoDB Enhanced Client handles this natively for simple types; verified this is the same mechanism used for scalar fields elsewhere. Low risk, but worth a quick compile-time sanity check in Phase 5 since this is the first `List<String>` field in a `@DynamoDbBean` in this codebase (all existing beans only have scalar fields).
