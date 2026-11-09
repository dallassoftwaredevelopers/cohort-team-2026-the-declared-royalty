import type { CurrentUserState } from "@/features/users/useCurrentUser";
import UserAvatar from "@/features/users/UserAvatar";
import { getDisplayName } from "@/features/users/userDisplay";

type SidebarProfileProps = {
  currentUser: CurrentUserState;
};

function profileText({ user, loading }: CurrentUserState) {
  if (loading) return { name: "Loading…", detail: "" };
  if (user) return { name: getDisplayName(user), detail: user.email ?? user.role ?? "" };
  // TODO(auth): with login, "no user" means signed out rather than an error;
  // show "Not signed in" with a link to the sign-in page
  return { name: "Not signed in", detail: "Couldn't load user" };
}

// Shows who you are; the account menu itself lives on the top bar avatar
export default function SidebarProfile({ currentUser }: SidebarProfileProps) {
  const { name, detail } = profileText(currentUser);

  return (
    <div
      className="flex h-[84px] shrink-0 items-center gap-3 border-t border-[#e6ebe8] px-6"
      title={currentUser.error ?? undefined}
    >
      <UserAvatar sizeClassName="size-8" user={currentUser.user} />
      <div className="flex min-w-0 flex-1 flex-col gap-1">
        <span className="font-inter-medium truncate text-xs text-[#68766f]">
          {name}
        </span>
        {detail && (
          <span className="font-inter-regular truncate text-[11px] text-[#98a29c]">
            {detail}
          </span>
        )}
      </div>
    </div>
  );
}
