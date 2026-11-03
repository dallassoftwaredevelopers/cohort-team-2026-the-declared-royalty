import { useEffect, useState } from "react";
import { apiClient } from "../app/services/apiClient";

type User = {
  userId: string;
  username?: string;
  email?: string;
  role?: string;
  status?: string;
  createdAt: string;
  updatedAt: string;
};

function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let active = true;

    apiClient
      .get<User[]>("/api/v1/users", undefined, false)
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
