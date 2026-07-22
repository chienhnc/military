---
phase: 1
title: Implement API
status: completed
priority: P2
effort: 1h
dependencies: []
---

# Phase 1: Implement API

## Overview

Add `GET /api/common/combobox/users`: returns `ComboboxOptionResponse` list of Users
that have a linked `MilitaryPersonnel` (inner join). Users without a linked personnel
are excluded from the result.

## Requirements

- Functional:
  - New repository method `UserRepository.findAllList()` (mirrors
    `MilitaryPersonnelRepository.findAllList()`), scans the DynamoDB `users` table.
  - New service method `CommonService.getUserCombobox()`: loads all personnel into a
    `Map<Long, MilitaryPersonnel>` keyed by id, then filters users where
    `militaryPersonnelId != null && personnelById.containsKey(militaryPersonnelId)`.
  - `ComboboxOptionResponse.code` = `String.valueOf(user.getId())` (Users have no
    natural short code like rank/position/unit do).
  - `ComboboxOptionResponse.name` = `"{personnel.fullName} ({user.username})"`.
  - New controller endpoint `GET /api/common/combobox/users`, same shape as
    `getRankCombobox()` / `getPositionCombobox()` (no extra role/scope check —
    `WebSecurityConfig` already requires authentication for all of `/api/common/**`
    except `/api/common/images/**`, matching ranks/positions, not the units endpoint's
    extra `AccessScope` restriction).
- Non-functional: no pagination (existing combobox endpoints are unpaginated lists);
  DynamoDB full-table scan is consistent with existing `findAllList()` usage elsewhere.

## Architecture

```
GET /api/common/combobox/users
  -> CommonController.getUserCombobox()
  -> CommonService.getUserCombobox()
       -> militaryPersonnelRepository.findAllList()  -> Map<Long, MilitaryPersonnel>
       -> userRepository.findAllList()
            .filter(user has militaryPersonnelId AND id exists in map)
            .map(user -> ComboboxOptionResponse(userId, "fullName (username)"))
  -> BaseResponse.of(200, data, path)
```

## Related Code Files

- Modify: `src/main/java/com/military/repository/UserRepository.java` — add
  `List<User> findAllList();`
- Modify: `src/main/java/com/military/repository/dynamodb/UserRepositoryImpl.java` —
  implement `findAllList()` via `table.scan().items().stream().map(this::toModel).toList()`
  (same style as `MilitaryPersonnelRepositoryImpl.findAllList()`).
- Modify: `src/main/java/com/military/service/CommonService.java` — add
  `List<ComboboxOptionResponse> getUserCombobox();`
- Modify: `src/main/java/com/military/service/impl/CommonServiceImpl.java` — implement
  `getUserCombobox()` per Architecture above.
- Modify: `src/main/java/com/military/controllers/CommonController.java` — add
  `@GetMapping("/combobox/users")` handler mirroring `getRankCombobox()`.

## Implementation Steps

1. `UserRepository`: add `List<User> findAllList();` to the interface (import `java.util.List`).
2. `UserRepositoryImpl`: implement `findAllList()` — scan the table, map each `UserItem`
   to `User` via the existing private `toModel()` helper, return as `List`.
3. `CommonService`: add `List<ComboboxOptionResponse> getUserCombobox();` to the interface.
4. `CommonServiceImpl`: implement `getUserCombobox()`:
   - `militaryPersonnelRepository.findAllList()` → collect to
     `Map<Long, MilitaryPersonnel>` via `Collectors.toMap(MilitaryPersonnel::getId, p -> p, (a, b) -> a)`.
   - `userRepository.findAllList()` → filter users with non-null `militaryPersonnelId`
     present in the map → map to `new ComboboxOptionResponse(String.valueOf(user.getId()), personnel.getFullName() + " (" + user.getUsername() + ")")`.
   - Return as `List` (`.toList()`).
5. `CommonController`: add
   ```java
   @GetMapping("/combobox/users")
   @Operation(summary = "Combobox user", description = "Tra danh sach user co lien ket MilitaryPersonnel (inner join)")
   @ApiResponse(responseCode = "200", description = "Lay du lieu thanh cong")
   public ResponseEntity<BaseResponse<List<ComboboxOptionResponse>>> getUserCombobox(HttpServletRequest request) {
     List<ComboboxOptionResponse> data = commonService.getUserCombobox();
     return ResponseEntity.ok(BaseResponse.of(200, data, request.getServletPath()));
   }
   ```
6. Compile the project (`mvn -q compile` or project's build command) and fix any errors.

## Success Criteria

- [x] `GET /api/common/combobox/users` returns 200 with a JSON array of
      `{code, name}` objects.
- [x] Users with `militaryPersonnelId == null`, or with a `militaryPersonnelId` that
      does not resolve to an existing `MilitaryPersonnel`, are excluded from the result
      (verifies inner-join semantics, not left-join). Verified via 7 unit tests in
      `CommonServiceImplUserComboboxTest`.
- [x] Unauthenticated requests are rejected (401), consistent with other
      `/api/common/combobox/**` endpoints (existing `WebSecurityConfig` catch-all,
      unchanged).
- [x] Project compiles with no errors (JDK 17 required — see project note on Lombok/JDK 25).

## Risk Assessment

- **Full table scans on every call**: both `userRepository.findAllList()` and
  `militaryPersonnelRepository.findAllList()` scan their entire DynamoDB tables. This
  matches the existing `getUnitComboboxByCurrentUser()` / `findAllList()` pattern
  already in use — acceptable at current data volume, not a new risk class introduced
  by this endpoint. No pagination added, to stay consistent with sibling combobox
  endpoints.
- **No role restriction**: unlike `/combobox/units`, this endpoint has no `AccessScope`
  check — any authenticated user can list all linked users. This mirrors
  `/combobox/ranks` and `/combobox/positions` (static reference data), but
  `/combobox/users` exposes real user/personnel names. If this needs to be restricted
  to admins, that's a scope decision for the user to confirm before implementation —
  not assumed here.
