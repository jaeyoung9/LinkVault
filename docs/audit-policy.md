# LinkVault Audit Policy

## 1. Overview

All admin mutations are recorded in the `audit_logs` table.
Audit logging runs in an isolated `REQUIRES_NEW` transaction so a persistence failure never rolls back the business operation.

## 2. Detail-Field Standards

Each audit entry contains a `details` column (VARCHAR 500).
Details use a `key=value` pair format produced by `AuditDetailFormatter.format()`.

| Action Code | Entity Type | Detail Fields |
|---|---|---|
| `ADMIN_USER_CREATE` | User | `username`, `role` |
| `ADMIN_USER_UPDATE` | User | `role` |
| `ADMIN_USER_DELETE` | User | _(none)_ |
| `ADMIN_USER_TOGGLE` | User | `enabled` |
| `ADMIN_USER_BULK_DEACTIVATE` | User | `deactivated` (count) |
| `ADMIN_TAG_MERGE` | Tag | `target`, `sourceCount` |
| `ADMIN_TAG_CLEANUP` | Tag | `deleted` (count) |
| `ADMIN_TAG_DELETE` | Tag | _(none)_ |
| `BACKUP_CREATE` | System | `filename` |
| `BACKUP_RESTORE` | System | `filename` |
| `ADMIN_INVITATION_CREATE` | Invitation | `role`, `maxUses` |
| `ADMIN_INVITATION_TOGGLE` | Invitation | _(none)_ |
| `ADMIN_INVITATION_DELETE` | Invitation | _(none)_ |
| `ADMIN_MENU_CREATE` | MenuItem | `label` |
| `ADMIN_MENU_UPDATE` | MenuItem | `label` |
| `ADMIN_MENU_DELETE` | MenuItem | _(none)_ |
| `ADMIN_MENU_TOGGLE` | MenuItem | _(none)_ |
| `ADMIN_MENU_REORDER` | MenuItem | `count` |
| `ADMIN_PERMISSION_TOGGLE` | Permission | `role`, `granted` |
| `ADMIN_QNA_CREATE` | QnaArticle | `category` |
| `ADMIN_QNA_UPDATE` | QnaArticle | `category` |
| `ADMIN_QNA_STATUS_CHANGE` | QnaArticle | `status` |
| `ADMIN_QNA_DELETE` | QnaArticle | _(none)_ |
| `ADMIN_ANNOUNCEMENT_CREATE` | Announcement | `priority` |
| `ADMIN_ANNOUNCEMENT_UPDATE` | Announcement | _(none)_ |
| `ADMIN_ANNOUNCEMENT_STATUS` | Announcement | `status` |
| `ADMIN_ANNOUNCEMENT_DELETE` | Announcement | _(none)_ |
| `ADMIN_REPORT_REVIEW` | Report | `status` |
| `AUTO_DISABLE_USER` | User | `username`, `actionedReports` |
| `BULK_SOFT_DELETE_CONTENT` | User | `username`, `deletedPosts`, `deletedComments` |
| `ADMIN_PRIVACY_POLICY_UPDATE` | PrivacyPolicy | `version` |
| `ADMIN_SETTINGS_UPDATE` | SystemSettings | `key` |
| `ADMIN_AUDIT_POLICY_UPDATE` | SystemSettings | `key` |
| `SOFT_DELETE_COMMENT` | Comment | `bookmarkId`, `owner`, `moderatorAction` |
| `RESTORE_COMMENT` | Comment | `bookmarkId`, `owner` |
| `PURGE_COMMENT` | Comment | `bookmarkId`, `owner` |
| `CREATE_BOOKMARK` | Bookmark | title (raw string) |
| `UPDATE_BOOKMARK` | Bookmark | title (raw string) |
| `SOFT_DELETE_BOOKMARK` | Bookmark | title (raw string) |
| `RESTORE_BOOKMARK` | Bookmark | title (raw string) |
| `PURGE_BOOKMARK` | Bookmark | title (raw string) |

## 3. Sensitive-Data Masking Rules

Masking is controlled by the `audit.masking.level` system setting.

| Level | Behaviour |
|---|---|
| `NONE` | No masking applied. Detail fields stored as-is. |
| `BASIC` (default) | Emails masked (`ab***@domain.com`). URLs kept. Tokens/passwords never logged. |
| `STRICT` | Emails masked. URLs truncated to host only. Usernames replaced with user IDs where possible. |

Implementation notes:
- `AuditDetailFormatter.maskEmail()` handles email masking.
- `AuditDetailFormatter.maskUrl()` handles URL masking (STRICT only).
- Passwords and auth tokens are **never** included in audit details regardless of level.
- Masking is applied at write time inside `AuditLogService.log()` before persistence.

## 4. Retention / Deletion Policy

Controlled by system settings:

| Setting Key | Type | Default | Description |
|---|---|---|---|
| `audit.retention.enabled` | boolean | `false` | Whether automatic retention cleanup is active |
| `audit.retention.days` | int | `365` | Days to retain audit entries (min 30, max 3650) |
| `audit.delete.mode` | enum | `SOFT` | `SOFT` = mark as archived; `HARD` = permanently delete |
| `audit.masking.level` | enum | `BASIC` | Masking level applied to audit detail fields |

When retention is enabled, a scheduled job (or manual admin trigger) can purge entries older than `retention.days`.

When `delete.mode = SOFT`, expired entries are flagged but remain in the database for compliance queries. When `HARD`, they are permanently removed.

## 5. Recommended Defaults

For most deployments:
- `audit.retention.enabled = false` (retain indefinitely)
- `audit.retention.days = 365`
- `audit.delete.mode = SOFT`
- `audit.masking.level = BASIC`

For high-sensitivity environments:
- `audit.masking.level = STRICT`
- `audit.retention.enabled = true`
- `audit.retention.days = 90`
- `audit.delete.mode = HARD`

## 6. Operational Impact

- **Storage**: Each audit entry is ~200 bytes. At 1000 admin actions/day, expect ~70 MB/year.
- **Performance**: The `REQUIRES_NEW` propagation isolates audit writes from the main transaction. The try-catch wrapper ensures audit failures never block business operations.
- **Indexes**: `audit_logs` has indexes on `user_id`, `timestamp`, and `action` for efficient querying.
- **User deletion**: When a user is deleted, audit log FK references are nullified (not deleted). The `actorUsername` column preserves the actor identity for historical audit trail.
- **H2 note**: In the default H2 in-memory configuration, all audit data resets on server restart. Use backup/restore to preserve data across restarts.

## 7. Changing Audit Policy

Changes to audit settings (`audit.*` keys) are logged under the `ADMIN_AUDIT_POLICY_UPDATE` action code. Only users with the `SYSTEM_SETTINGS` permission can modify these values.

Validation rules:
- `audit.retention.enabled`: must be `true` or `false`
- `audit.retention.days`: integer between 30 and 3650
- `audit.delete.mode`: must be `SOFT` or `HARD`
- `audit.masking.level`: must be `NONE`, `BASIC`, or `STRICT`

Invalid values are rejected with HTTP 400 and a descriptive error message.
