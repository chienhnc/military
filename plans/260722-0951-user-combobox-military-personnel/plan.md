---
title: User Combobox Inner Join MilitaryPersonnel
description: >-
  New GET /api/common/combobox/users endpoint returning only Users that have a
  matching MilitaryPersonnel record (inner join semantics), for combobox UI.
status: completed
priority: P2
branch: master
tags:
  - backend
  - dynamodb
  - combobox
blockedBy: []
blocks: []
created: '2026-07-22T02:51:04.763Z'
createdBy: 'ck:plan'
source: skill
---

# User Combobox Inner Join MilitaryPersonnel

## Overview

Add a combobox API that returns all `User` records inner-joined to `MilitaryPersonnel`
(i.e. only users whose `militaryPersonnelId` points to an existing `MilitaryPersonnel`
row are included — users without a linked personnel are excluded, matching SQL INNER
JOIN semantics). Follows the exact existing pattern of `/api/common/combobox/ranks`,
`/positions`, and `/units` in `CommonController` / `CommonService` / `CommonServiceImpl`.

Data layer is DynamoDB (no relational JOIN available), so the "join" is done in the
service layer: load all personnel into an id-keyed map, then filter+map users whose
`militaryPersonnelId` exists in that map.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Implement API](./phase-01-implement-api.md) | Completed |

## Dependencies

None. No overlap with the completed `260721-1139-personnel-vehicle-management` plan.
