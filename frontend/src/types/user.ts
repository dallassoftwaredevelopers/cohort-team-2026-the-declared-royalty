import type { Property } from "@/types/property.ts";

export type User = {
    userId: string,
    userName: string,
    email: string,
    role: string,
    status: string,
    properties: Property[],
    createdAt: string,
    updatedAt: string,
}

export type UserResponse = {
    userId: string,
    username?: string,
    email?: string,
    role?: string,
    status?: string,
    createdAt: string,
    updatedAt: string,
}

// role defaults to ADMIN and status to ACTIVE when omitted
export type UserCreateRequest = {
    username: string,
    email: string,
    role?: string,
    status?: string,
}

// Partial update: omitted fields are unchanged
export type UserUpdateRequest = Partial<UserCreateRequest>;
