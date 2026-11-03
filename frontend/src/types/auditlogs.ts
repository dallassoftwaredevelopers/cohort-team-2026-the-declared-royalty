import type { User } from "@/types/user.ts";
import type { Invoice } from "@/types/invoice.ts";

export type AuditLogs = {
    auditUuid: string,
    invoice: Invoice,
    user: User,
    actionType: string,
    actionTaken: string,
    fromState: string,
    toState: string,
    notes: string,
    createdAt: string,
    updatedAt: string,
}

export type AuditLogsResponse = {
    auditUuid: string,
    invoice: Invoice,
    user: User,
    actionType: string,
    actionTaken: string,
    fromState: string,
    toState: string,
    notes: string,
    createdAt: string,
    updatedAt: string,
}