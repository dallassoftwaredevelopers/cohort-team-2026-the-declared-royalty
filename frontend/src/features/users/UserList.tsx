import { useEffect, useState } from "react";
import { getUsers } from "./userService";
import type { UserResponse } from "@/types/user";

function UserList() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    getUsers()
      .then((result) => {
        if (active) setUsers(result);
      })
      .catch((requestError: unknown) => {
        if (active) {
          setError(
            requestError instanceof Error
              ? requestError.message
              : "Unable to load users.",
          );
        }
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, []);

  return (
    <main className="mx-auto max-w-3xl p-6">
      <h1 className="mb-4 text-2xl font-semibold">Users</h1>

      {loading && <p>Loading users...</p>}
      {error && <p role="alert">Could not load users: {error}</p>}
      {!loading && !error && users.length === 0 && <p>No users found.</p>}

      {!loading && !error && users.length > 0 && (
        <ul className="divide-y divide-gray-200">
          {users.map((user) => (
            <li className="py-4" key={user.userId}>
              <h2 className="font-medium">{user.username || "Unnamed user"}</h2>
              <p>{user.email || "No email"}</p>
              <p>
                {user.role || "No role"} · {user.status || "No status"}
              </p>
            </li>
          ))}
        </ul>
      )}
    </main>
  );
}

export default UserList;
