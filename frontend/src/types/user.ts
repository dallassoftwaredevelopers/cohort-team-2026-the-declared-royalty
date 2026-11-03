import type { Property } from "@/types/property.ts";

export type User = {
    userId: string,
    userName: string,
    email: string,
    role: string,
    properties: Property[],
    createdAt: string,
    updatedAt: string,
}

export type UserResponse = {
    userId: string,
    createdAt: string,
    updatedAt: string,
}