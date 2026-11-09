import type { AuditLogCreateRequest, AuditLogFilters, AuditLogsResponse } from "@/types/auditlogs.ts";
import { apiClient } from "@/app/services/apiClient.ts";

const AUDIT_LOGS_URL = "/api/v1/audit-logs";

export function getAuditLogs(filters: AuditLogFilters = {}): Promise<AuditLogsResponse[]> {
    const params = new URLSearchParams();
    for (const [key, value] of Object.entries(filters)) {
        if (value && value.trim() !== "") params.append(key, value);
    }
    const query = params.toString();
    const url = query ? `${AUDIT_LOGS_URL}?${query}` : AUDIT_LOGS_URL;
    return apiClient.get<AuditLogsResponse[]>(url, undefined, false);
}

export function getAuditLogById(auditUuid: string): Promise<AuditLogsResponse> {
    return apiClient.get<AuditLogsResponse>(`${AUDIT_LOGS_URL}/${auditUuid}`, undefined, false);
}

export function createAuditLog(request: AuditLogCreateRequest): Promise<AuditLogsResponse> {
    return apiClient.post<AuditLogsResponse, AuditLogCreateRequest>(AUDIT_LOGS_URL, request, undefined, false);
}