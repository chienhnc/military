# Red Team Review — Personnel Vehicle Management

**Note on methodology:** the 3 parallel hostile-reviewer subagents (Security Adversary, Failure Mode Analyst, Assumption Destroyer) all failed mid-review due to the platform session limit being hit, before writing any report. No partial reports were produced. Rather than re-spawn (same limit would likely block again), the planner performed the same 3-lens adversarial review directly, with the same evidence-citation discipline (grep-verified file:line citations, no finding without codebase evidence).

## Findings

### Finding 1: Under-specified per-tier authorization on VehicleController — CRITICAL
**Lens:** Security Adversary
**Location:** Phase 2, "Implementation Steps" for `getById`/`getByPersonnelId`/`update`/`deleteImage`
**Flaw:** The plan says "authorize via AccessScope" without specifying the exact boolean check, unlike the existing codebase's precise pattern.
**Failure scenario:** A regular `user`-tier caller hits `GET /api/vehicles/{id}` with an ID that isn't theirs. If the implementer doesn't write an exact `canAccessVehicle(scope, vehicle)`-style check (mirroring the existing pattern), this becomes an IDOR: any authenticated user can enumerate `/api/vehicles/1..N` and read other personnel's vehicle + license plate data.
**Evidence:** Existing precise pattern: `src/main/java/com/military/service/impl/MilitaryPersonnelServiceImpl.java:393` (`private boolean canAccessPersonnel(AccessScope scope, MilitaryPersonnel personnel)`), used at lines 122, 137, 155, 346. Phase 2's plan text has no equivalent named-method spec — just prose "authorize via AccessScope."
**Suggested fix:** Add an explicit `canAccessVehicle(AccessScope scope, Vehicle vehicle)` helper to Phase 2's Implementation Steps, spelled out with the exact same three-tier boolean logic as `canAccessPersonnel`, and require every read/write method to call it before returning data.
**Disposition:** ACCEPT — add explicit helper spec to Phase 2.

### Finding 2: Vehicle images are publicly readable (no auth) — MEDIUM
**Lens:** Security Adversary
**Location:** Phase 3, `/api/common/images/vehicle/{filename}`
**Flaw:** `/api/common/images/**` is `permitAll()` in Spring Security — anyone with a filename (UUID) can fetch any image, including vehicle photos that may reveal a serviceman's license plate.
**Evidence:** `src/main/java/com/military/security/WebSecurityConfig.java:108` — `.requestMatchers("/api/common/images/**").permitAll()`.
**Failure scenario:** Not novel — personnel/unit images are already public under the same rule. The plan doesn't flag that vehicle images inherit this same public-by-default exposure for a new, arguably more sensitive data category (a personal vehicle + plate tied to a specific service member).
**Suggested fix:** Not a required code change (fixing this would mean redesigning image auth for ALL 3 categories, out of scope for this plan) — add an explicit note to Phase 3's Risk Assessment acknowledging the inherited exposure, so it's a documented decision, not a silent gap.
**Disposition:** ACCEPT (documentation-only) — add risk note to Phase 3.

### Finding 3: Non-atomic personnel+vehicle creation risks orphaned/duplicate personnel — CRITICAL
**Lens:** Failure Mode Analyst
**Location:** Phase 4, `create()` implementation step
**Flaw:** Personnel is saved, THEN vehicle is created. If vehicle creation throws, the generic exception handler returns a 500 with no indication the personnel row already exists. The plan's own Risk Assessment acknowledges this but defers it as "YAGNI unless it proves a real-world problem."
**Failure scenario:** DynamoDB has a transient error while creating the vehicle. Client sees "500 Internal server error," assumes creation failed, and retries the exact same `POST /api/personnel` call — creating a SECOND personnel row (no idempotency check anywhere in `create()`), now with two personnel records for one real person, one of which has no vehicle.
**Evidence:** `src/main/java/com/military/service/impl/MilitaryPersonnelServiceImpl.java:106` (`MilitaryPersonnel saved = militaryPersonnelRepository.save(personnel);`) followed immediately by vehicle creation per the plan; `src/main/java/com/military/exception/GlobalExceptionHandler.java:58` (`@ExceptionHandler(Exception.class)` catch-all → generic 500, no distinction between "nothing happened" and "partially happened").
**Suggested fix:** Do not defer as YAGNI. At minimum: if `vehicleService.create()` throws inside `MilitaryPersonnelServiceImpl.create()`, catch it and compensate by deleting the just-created personnel row before re-throwing, so the operation is effectively all-or-nothing from the client's perspective (a real DynamoDB transaction isn't used anywhere in this codebase, so full ACID isn't achievable without much larger scope — but a manual compensating delete is cheap and bounds the damage to "one more failed row deleted" instead of "silent duplicate risk on retry").
**Disposition:** ACCEPT — add compensating cleanup to Phase 4, upgrade Risk Assessment language from "deferred as YAGNI" to "handled via compensating delete."

### Finding 4: Cascade-delete ordering can orphan the vehicle — HIGH
**Lens:** Failure Mode Analyst
**Location:** Phase 4, `delete()` implementation step
**Flaw:** `vehicleService.deleteByPersonnelId()` is called AFTER `militaryPersonnelRepository.delete(personnel)`. If the vehicle-delete throws (e.g. one of several S3 image deletes fails transiently), the personnel is already gone but the vehicle row + remaining S3 images become permanently unreachable via any personnel-based lookup.
**Evidence:** `src/main/java/com/military/service/impl/MilitaryPersonnelServiceImpl.java:159` (`militaryPersonnelRepository.delete(personnel);`) — Phase 4's plan places the vehicle cascade-delete call immediately after this line.
**Suggested fix:** Reverse the order — delete the vehicle (and its S3 images) FIRST, then delete the personnel row. If vehicle-delete fails, the whole `delete()` call fails cleanly with the personnel record still intact (nothing orphaned); the caller can retry safely.
**Disposition:** ACCEPT — reorder in Phase 4.

### Finding 5: Orphaned S3 uploads amplified by multi-image design — MEDIUM
**Lens:** Failure Mode Analyst
**Location:** Phase 1 (`VehicleRequest.imagePaths` `@Size(max=10)`) / Phase 3 (upload reuse)
**Flaw:** The 10-image cap only applies to what's included in a submitted request — nothing stops unbounded independent calls to `/api/common/upload-image?category=vehicle` whose results are never referenced.
**Evidence:** Same pre-existing weak pattern as personnel/unit single-image upload (abandoned uploads already orphan today), but the vehicle feature's multi-image design multiplies the potential per-entity volume ~10x.
**Suggested fix:** Not a blocking change — add a note to Phase 3's Risk Assessment naming this as an accepted, pre-existing-pattern limitation rather than leaving it unmentioned.
**Disposition:** ACCEPT (documentation-only) — add risk note to Phase 3.

### Finding 6: Making `vehicle` required-on-create silently forces it onto the self-service signup flow — CRITICAL
**Lens:** Assumption Destroyer
**Location:** Phase 4, `MilitaryPersonnelRequest.vehicle` + Success Criteria ("Signup flow... also requires and creates a vehicle")
**Flaw:** `AuthServiceImpl.registerUser()` calls the exact same `MilitaryPersonnelService.create()`. Making vehicle effectively-required there means every self-registering user must now declare a vehicle at signup — a business rule never confirmed with the user, and the original feature request was scoped to admin-driven "quản lý quân nhân" (personnel management), not the self-registration UX.
**Evidence:** `src/main/java/com/military/service/impl/AuthServiceImpl.java:101` (`militaryPersonnelService.create(signUpRequest.getMilitaryPersonnel())`) — confirmed single shared `create()` entry point for both admin-created and self-registered personnel.
**Suggested fix:** Ask the user directly whether self-registering personnel must also declare a vehicle at signup, or whether the requirement should be scoped to admin-driven creation only (in which case `create()` would need to distinguish caller context, or `AuthServiceImpl` would need its own path — added complexity either way, worth confirming before committing to one).
**Disposition:** ACCEPT — surface as an open question to the user (not something the planner should silently decide).

### Finding 7: No remediation path for personnel left without a vehicle — HIGH
**Lens:** Assumption Destroyer
**Location:** Phase 3 ("No `POST /api/vehicles`... deliberate") / Phase 4 (`vehicle: null` framed as acceptable for legacy records)
**Flaw:** Combined with Finding 3 (partial-failure risk) and any pre-existing personnel record, there is currently zero path — no endpoint, no admin tool — to ever attach a vehicle to a personnel record after the fact.
**Evidence:** Phase 3 explicitly states creation is "exclusively internal... never a public `POST /api/vehicles`" with no carve-out.
**Suggested fix:** Depends on the resolution of Finding 3 and the plan's open question #2 in `plan.md` (legacy personnel behavior). If compensating-delete (Finding 3's fix) makes create-failure self-healing, this narrows to only the legacy-record case — which the user may simply accept (per `plan.md`'s existing Open Question #2) or may want a one-time admin backfill capability for.
**Disposition:** ACCEPT — link to existing Open Question #2 in `plan.md`, no separate code change required beyond Finding 3's fix.

### Finding 8: `GET /api/vehicles/by-personnel/{personnelId}` lacks a validated use case — MEDIUM
**Lens:** Assumption Destroyer
**Location:** Phase 3, endpoint list
**Flaw:** Justified only as "useful for direct deep-link/refresh scenarios" — no concrete consumer was identified; `MilitaryPersonnelResponse.vehicle` (Phase 4) already serves the same data on every personnel fetch.
**Evidence:** Phase 3's own text: "though personnel response already embeds it — still useful for direct deep-link/refresh scenarios" (hedged, not evidenced).
**Suggested fix:** Keep it (cost is low, one extra controller method) but reword Phase 3 to stop overstating its necessity — it's a convenience endpoint, not a validated requirement.
**Disposition:** ACCEPT (documentation-only) — reword justification in Phase 3.

### Finding 9: "Every personnel has exactly one vehicle" is asserted as a universal business rule, unconfirmed — MEDIUM
**Lens:** Assumption Destroyer
**Location:** `plan.md` Overview / Phase 4 (`PERSONNEL_VEHICLE_REQUIRED`)
**Flaw:** The original request states "1 quân nhân có 1 phương tiện" as a given, and the plan operationalizes it as hard-mandatory at creation. In reality, not every service member may own a vehicle.
**Evidence:** This is a requirements question, not a code defect — flagging because the plan commits to a hard `@NotNull`-equivalent enforcement (`PERSONNEL_VEHICLE_REQUIRED`) based on a literal reading of the request without confirming whether "no vehicle" is a valid real-world case.
**Suggested fix:** Confirm directly with the user (ties into Finding 6 and `plan.md`'s existing Open Question #1).
**Disposition:** ACCEPT — surface as an open question, not a plan defect to silently fix.

## Summary

- **Total findings:** 9 (0 rejected — all passed the evidence filter)
- **Severity breakdown:** 3 Critical, 2 High, 4 Medium
- **Disposition:** all 9 Accept. 6 (F1-F5, F8) are direct plan-text edits with no user input needed. 3 (F6, F7, F9) require the user's decision before the plan can be finalized — they change the fundamental "vehicle required at creation, for every caller" business rule.
