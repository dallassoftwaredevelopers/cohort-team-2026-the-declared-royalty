import { apiClient } from "./apiClient";
import type {
  UserCreateRequest,
  UserResponse,
  UserUpdateRequest,
} from "../../types/user";

const USERS_URL = "/api/v1/users";

/**
 * List all users
 * @returns Every user, active and inactive
 */
export function getUsers(): Promise<UserResponse[]> {
  return apiClient.get<UserResponse[]>(USERS_URL);
}

/**
 * Get a single user
 * @param userId The UUID of the user
 * @returns The user; rejects with a 404 error when not found
 */
export function getUserByUserId(userId: string): Promise<UserResponse> {
  return apiClient.get<UserResponse>(`${USERS_URL}/${userId}`);
}

/**
 * Create a user. role defaults to ADMIN and status to ACTIVE when omitted.
 * @param request The new user's details
 * @returns The created user
 */
export function createUser(request: UserCreateRequest): Promise<UserResponse> {
  return apiClient.post<UserResponse, UserCreateRequest>(USERS_URL, request);
}

/**
 * Update a user. Only the fields present in the request are changed.
 * @param userId The UUID of the user
 * @param request The fields to change
 * @returns The updated user; rejects with a 404 error when not found
 */
export function updateUserByUserId(
  userId: string,
  request: UserUpdateRequest,
): Promise<UserResponse> {
  return apiClient.put<UserResponse, UserUpdateRequest>(
    `${USERS_URL}/${userId}`,
    request,
  );
}

/**
 * Deactivate a user. This is a soft delete: the backend sets status to
 * INACTIVE and keeps the record.
 * @param userId The UUID of the user
 */
export function deactivateUserByUserId(userId: string): Promise<void> {
  return apiClient.delete<void>(`${USERS_URL}/${userId}`);
}
