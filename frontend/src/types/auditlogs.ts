import type { User } from "@/types/user.ts";
import type { Invoice } from "@/types/invoice.ts";

export type AuditActionType =
    | "SYSTEM_INGESTED" | "USER_INGESTED"
    | "SYSTEM_IDENTIFIED" | "USER_IDENTIFIED"
    | "SYSTEM_MATCHED" | "USER_MATCHED"
    | "SYSTEM_VALIDATED" | "USER_VALIDATED"
    | "SYSTEM_APPROVED" | "USER_APPROVED"
    | "SYSTEM_PAID" | "USER_PAID";

export type AuditLogs = {
    auditUuid: string,
    invoice: Invoice,
    user: User,
    actionType: AuditActionType,
    actionTaken: string,
    fromState: string,
    toState: string,
    notes: string,
    createdAt: string,
    updatedAt: string,
}

export type AuditLogsResponse = {
    auditLogId: string,
    invoice: string,
    user: string,
    actionType: AuditActionType,
    actionTaken: string,
    fromState: string,
    toState: string,
    notes: string | null,
    createdAt: string,
    updatedAt: string,
}

export type AuditLogCreateRequest = {
    invoice: string,
    user: string,
    actionType: AuditActionType,
    actionTaken: string,
    fromState: string,
    toState: string,
    notes?: string,
}

// Optional query filters for GET /api/v1/audit-logs
export type AuditLogFilters = {
    invoice?: string,
    actionType?: AuditActionType,
    fromState?: string,
    toState?: string,
}
