import type { UserResponse } from "@/types/user";

// The name to show for a user; username and email are optional in the API
export function getDisplayName(user: UserResponse): string {
  return user.username?.trim() || user.email || "Unnamed user";
}

// Up to two initials: "Jordan Lee" -> "JL", "jordan.lee" -> "JL", "jordan" -> "J"
export function getInitials(name: string): string {
  const parts = name.trim().split(/[\s._-]+/).filter(Boolean);
  if (parts.length === 0) return "?";
  const letters = parts.length === 1 ? parts[0][0] : parts[0][0] + parts[parts.length - 1][0];
  return letters.toUpperCase();
}
