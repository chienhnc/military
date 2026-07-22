# Military Region Removal, CORS Fix, and Vehicle Feature

**Date**: 2026-07-21 15:45
**Severity**: High (region removal), High (CORS), Medium (feature)
**Component**: Security layer, RBAC model, API architecture, personnel management
**Status**: Resolved

## What Happened

Three distinct deployment cycles completed this session:

1. **Military Region Feature Removed** (commit fa6810a): Deleted the entire `MilitaryRegion` entity, controller, service, repository, DTOs, and DynamoDB table. Removed `regionCode` field from `MilitaryUnit` and `MilitaryPersonnel`. Stripped `ROLE_ADMIN_REGION` from RBAC enum and all region-scoped access control. Personnel code generation format changed from `REGION|UNIT|RANK|POSITION` to `UNIT|RANK|POSITION`.

2. **CORS Bug Fixed** (commit 9b80259): Root cause identified and resolved — Spring Security's `AuthEntryPointJwt` was writing 401 responses directly to the servlet response from inside the security filter chain, completely bypassing Spring MVC's `@CrossOrigin` CORS header injection (which only applies post-DispatcherServlet). Centralized CORS via `CorsConfigurationSource` bean in `WebSecurityConfig`, covering all responses including security-layer rejections. Removed redundant `@CrossOrigin` annotations from 9 controllers.

3. **Personnel Vehicle Management Added** (new feature): Implemented `Vehicle` entity (1:1 owned by `MilitaryPersonnel`, 1:N images). New DynamoDB table `military_vehicles`, REST API at `/api/vehicles` (CRUD + search + image management). Vehicle optional at personnel-creation time; attachable later via `POST /api/vehicles?personnelId={id}`. Personnel deletion cascades. Used atomic DynamoDB conditional `attribute_not_exists(id)` to prevent TOCTOU races on concurrent create/attach operations.

## The Brutal Truth

**Region removal was irreversible data destruction.** The feature delete included a live DynamoDB table (`military_regions`) with production data. There is no soft-delete pattern, no backup/export workflow, no "are you absolutely sure?" gate in the deployment pipeline. Once SAM redeploy ran, that table was gone forever. This happened with explicit user acknowledgment beforehand, but the **architectural fragility is real** — a future accidental deletion (typo in template.yaml, forgotten --no-capabilities flag) would silently vaporize production data with zero recovery path.

**CORS bug was a painful architectural failure.** Spring Security's filter chain executes before DispatcherServlet, so auth-layer failures (401/403) never see CORS headers. Clients receive a cryptic "CORS error" instead of the real 401, making the actual problem (expired JWT, missing token) invisible. This bug persisted through deployment to production Lambda before discovery.

**No automated tests meant all verification fell to code review and live curl.** Every change relied on human inspection and manual testing against the deployed API. The vehicle feature was reviewed via subagent code-reviewer (scored 8/10, no blockers), but a regression in personnel-deletion cascade logic could ship undetected.

## Technical Details

### Region Removal
- Deleted entities: `MilitaryRegion`, `RegionDto`, `RegionResponse`, `RegionRequest`, `MilitaryRegionController`, `MilitaryRegionService`, `MilitaryRegionRepository`
- Removed enums: `ROLE_ADMIN_REGION` from `RoleType`
- Updated `MilitaryUnit` and `MilitaryPersonnel`: deleted `regionCode` field, removed region-based access checks
- Updated `ErrorCode` and `application.properties`: cleaned all region-related messages
- Personnel code format: was `{regionCode}|{unitCode}|{rank}|{position}` → now `{unitCode}|{rank}|{position}`

### CORS Fix
- **Before**: `AuthEntryPointJwt` called `response.sendError(401)` directly in filter chain → no CORS headers
- **After**: Implemented `CorsConfigurationSource` bean with allowed origins, methods, headers. Registered via `http.cors(corsConfigurationSource())` in `SecurityFilterChain`. CORS filter now runs before security filters, covering all 4xx/5xx responses.
- **Verification**: curl tests on deployed Lambda confirmed Authorization headers now receive `Access-Control-Allow-Origin` on both 401 and 200 responses

### Vehicle Feature
- **Entity**: `Vehicle` (DynamoDB item, partition key = `id` = `personnelId` for O(1) lookup)
- **1:N images**: `VehicleImage` table with GSI on `vehicleId`
- **Optional binding**: Vehicle creation deferred until first attach via API; personnel CRUD works without vehicle
- **Race condition mitigation**: DynamoDB `PutItem` with `attribute_not_exists(id)` condition key ensures atomic "create if not exists" for vehicle attach. Code review caught a TOCTOU race on concurrent create/attach calls; conditional write closes it.
- **Cascade delete**: `MilitaryPersonnelService.deletePersonnel()` also triggers `vehicleService.deleteVehicle(personnelId)`

## What We Tried

1. **Region removal**: Direct entity/table deletion. No alternative explored; requirement was removal, not refactoring.
2. **CORS fix**: Initially added `@CrossOrigin` to `AuthEntryPointJwt` — realized this doesn't work because the filter chain response is raw servlet, not MVC. Pivoted to `CorsConfigurationSource` + filter-chain registration.
3. **Vehicle management**: Verified atomic DynamoDB conditional write prevents TOCTOU via code inspection. Planner performed adversarial red-team review (3 hostile subagents hit platform limit mid-session; planner completed the review directly). No regression testing — relied on code-reviewer subagent final pass.

## Root Cause Analysis

**Region removal brittleness**: No soft-delete pattern exists in the codebase. Table definitions in template.yaml are treated as source-of-truth; deletion is implicit in a redeploy. This is a design flaw: production tables need explicit lifecycle management (archival, versioning, or irreversible backup before deletion).

**CORS bypass**: Spring Security filter chain executes before DispatcherServlet, and `AuthEntryPointJwt` writes responses at the servlet level. Annotation-based CORS (`@CrossOrigin`) only works on MVC handlers. This is a well-known Spring architecture pattern, but the project did not account for it during security layer implementation. No integration test caught this (test suite is empty).

**Vehicle TOCTOU race**: Initial design passed `personnelId` to vehicle create endpoint without ensuring atomicity. A second concurrent request could create a second `Vehicle` item for the same personnel. Code review caught it; fix was one-line addition of conditional `attribute_not_exists(id)`.

## Lessons Learned

1. **Destructive schema changes need explicit gates**: Before deleting any live DynamoDB table, export data to S3 backup and implement soft-delete patterns. Template.yaml should require explicit `--capability CAPABILITY_IAM` style confirmation for table deletions.

2. **Security filters need CORS awareness**: In any Spring app with centralized auth, configure CORS at the filter chain level (`CorsConfigurationSource`), not on individual handlers. Auth failures must return CORS headers or clients see misleading "CORS error" instead of real 401.

3. **Concurrent DynamoDB operations need conditionals**: When attaching/creating resources with external identifiers, use conditional writes (`attribute_not_exists`, `attribute_exists`) to prevent TOCTOU races. Atomic operations beat retry logic.

4. **Test-driven verification is not optional**: This project has zero unit tests. Every deploy relied on code inspection + live curl. Regression in vehicle-deletion cascade, missing image cleanup on vehicle delete, or auth scope issues on vehicle endpoints could ship undetected. Priority: set up a basic test suite (JUnit 5 + Mockito).

5. **JDK version lock matters**: This project requires JDK 17 specifically to build (Lombok annotation processing breaks silently on JDK 25, the system default). Document this in README.md and consider adding a `java.toolchain.version=17` property to pom.xml.

## Next Steps

1. **Add soft-delete pattern to DynamoDB tables**: Create a `deletedAt` timestamp field on `MilitaryUnit`, `MilitaryPersonnel`, `Vehicle`, `VehicleImage`. Soft-delete by setting `deletedAt`, query with `attribute_not_exists(deletedAt)` filter. Hard-delete via separate admin endpoint with explicit confirmation.

2. **Implement data export before destructive schema changes**: On any table deletion, trigger automated S3 backup export in the SAM deployment template. Link to this in deployment confirmation prompts.

3. **Add basic unit test suite**: Start with `MilitaryPersonnelServiceTest` (personnel CRUD, vehicle cascade delete), `VehicleServiceTest` (concurrent attach via mock DynamoDB), `AuthEntryPointJwtTest` (CORS headers on 401). Use JUnit 5 + Mockito. Target ≥ 60% coverage on core services.

4. **Document JDK 17 requirement**: Add to README.md: "Requires JDK 17 due to Lombok + Spring Boot 2.x compatibility. JDK 25+ breaks annotation processing silently."

5. **Add CORS integration test**: Verify that `/api/personnel` with invalid JWT returns both `401` and `Access-Control-Allow-Origin` header. Catch CORS bypass regressions before deployment.

6. **Code review the cascading delete logic**: Confirm `MilitaryPersonnelService.deletePersonnel()` cleans up all dependent `Vehicle` and `VehicleImage` records. Add a unit test for this path specifically.

---

**Deployed**: Three times (region removal, CORS fix, vehicle feature) to AWS Lambda + API Gateway + DynamoDB + S3 via SAM. Each deployment gated by explicit user confirmation beforehand. All changes compiled cleanly; live curl verification confirmed CORS headers and vehicle endpoints operational.
