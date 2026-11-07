import { useCallback, useEffect, useState } from "react";
import type { UserResponse } from "@/types/user";
import { getUserByUserId } from "./userService";

// Seeded by backend migration V4 in every database ("stand-in actor until real
// authentication exists"), so it's the default signed-in user for now.
// TODO(auth): stop using this as "you"; keep it only if the UI needs to label
// automated (System) actions, e.g. in the audit log feed
export const SYSTEM_USER_ID = "00000000-0000-0000-0000-000000000001";

// Where "Switch user" remembers its choice (development builds only)
// TODO(auth): remove with Switch user
const STORAGE_KEY = "dev.currentUserId";

export type CurrentUserState = {
  user: UserResponse | null;
  loading: boolean;
  error: string | null;
};

export type CurrentUser = CurrentUserState & {
  // Dev-only "Switch user"; remembered across reloads
  switchUser: (user: UserResponse) => void;
};

// Users offered by "Switch user"
export function isSelectableUser(user: UserResponse): boolean {
  return user.status === "ACTIVE";
}

// Storage can be missing or blocked (private mode, tests); treat that as "nothing saved"
export function readStoredUserId(): string | null {
  if (!import.meta.env.DEV) return null;
  try {
    return localStorage.getItem(STORAGE_KEY);
  } catch {
    return null;
  }
}

export function storeUserId(userId: string | null): void {
  if (!import.meta.env.DEV) return;
  try {
    if (userId) localStorage.setItem(STORAGE_KEY, userId);
    else localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Not saved; the choice still applies until the page reloads
  }
}

/**
 * TEMPORARY stand-in until login exists: the backend has no notion of who is
 * signed in, so "you" is
 *   1. the user picked with "Switch user" (development builds only), or
 *   2. the user whose UUID is in VITE_CURRENT_USER_ID (frontend/.env.local), or
 *   3. the System user seeded by migration V4.
 * A saved or configured user who no longer exists or is INACTIVE is skipped.
 *
 * TODO(auth): replace this whole function with "who does the session belong to":
 *   - fetch the signed-in user from the backend (e.g. a new GET /api/v1/users/me
 *     that reads the token), or read it from the auth provider's session
 *   - return null when there's no session, so the UI shows "Sign in"
 *   - delete readStoredUserId/storeUserId, switchUser, VITE_CURRENT_USER_ID and
 *     the System fallback; System stays a backend-only actor for automated actions
 *   - also refresh it after sign-in and clear it on sign-out (expose those from this hook)
 */
export async function loadCurrentUser(): Promise<UserResponse> {
  const storedId = readStoredUserId();
  if (storedId) {
    const stored = await getUserByUserId(storedId).catch(() => null);
    if (stored && isSelectableUser(stored)) return stored;
    // Deleted (e.g. the database was reset) or deactivated since it was picked; forget it
    storeUserId(null);
  }

  const configuredId = import.meta.env.VITE_CURRENT_USER_ID;
  if (configuredId && configuredId !== SYSTEM_USER_ID) {
    const configured = await getUserByUserId(configuredId).catch(() => null);
    if (configured && isSelectableUser(configured)) return configured;
  }

  return getUserByUserId(SYSTEM_USER_ID);
}

export function useCurrentUser(): CurrentUser {
  const [state, setState] = useState<CurrentUserState>({
    user: null,
    loading: true,
    error: null,
  });

  useEffect(() => {
    let active = true;

    loadCurrentUser()
      .then((user) => {
        if (active) setState({ user, loading: false, error: null });
      })
      .catch((requestError: unknown) => {
        if (active) {
          setState({
            user: null,
            loading: false,
            error:
              requestError instanceof Error
                ? requestError.message
                : "Unable to load user.",
          });
        }
      });

    return () => {
      active = false;
    };
  }, []);

  const switchUser = useCallback((user: UserResponse) => {
    storeUserId(user.userId);
    setState({ user, loading: false, error: null });
  }, []);

  return { ...state, switchUser };
}
