import { type KeyboardEvent, useEffect, useId, useRef, useState } from "react";
import { TbCheck, TbLogout } from "react-icons/tb";
import Icon from "@/components/ui/Icon";
import type { UserResponse } from "@/types/user";
import type { CurrentUser } from "./useCurrentUser";
import { isSelectableUser } from "./useCurrentUser";
import UserAvatar from "./UserAvatar";
import { getDisplayName } from "./userDisplay";
import { getUsers } from "./userService";

type UserMenuProps = {
  currentUser: CurrentUser;
};

type UserList =
  | { status: "idle" | "loading" }
  | { status: "loaded"; users: UserResponse[] }
  | { status: "error" };

const itemClass =
  "flex w-full items-center gap-3 rounded-md px-3 py-2 text-left text-[13px] outline-none";

// Account menu behind the top bar avatar (ARIA menu-button pattern)
export default function UserMenu({ currentUser }: UserMenuProps) {
  const { user, switchUser } = currentUser;
  const [open, setOpen] = useState(false);
  const [userList, setUserList] = useState<UserList>({ status: "idle" });
  const containerRef = useRef<HTMLDivElement>(null);
  const buttonRef = useRef<HTMLButtonElement>(null);
  const menuRef = useRef<HTMLDivElement>(null);
  const menuId = useId();
  const userName = user ? getDisplayName(user) : null;

  const items = () =>
    Array.from(menuRef.current?.querySelectorAll<HTMLElement>("[role^='menuitem']") ?? []);

  const close = (returnFocus: boolean) => {
    setOpen(false);
    if (returnFocus) buttonRef.current?.focus();
  };

  const openMenu = () => {
    setOpen(true);
    if (import.meta.env.DEV) {
      setUserList({ status: "loading" });
      getUsers()
        .then((users) => setUserList({ status: "loaded", users: users.filter(isSelectableUser) }))
        .catch(() => setUserList({ status: "error" }));
    }
  };

  // Focus the checked user, or else the first item, whenever the menu opens or the list arrives
  useEffect(() => {
    if (!open) return;
    const all = items();
    (all.find((item) => item.getAttribute("aria-checked") === "true") ?? all[0])?.focus();
  }, [open, userList]);

  // Clicks outside close the menu without stealing focus back
  useEffect(() => {
    if (!open) return;
    const onPointerDown = (event: PointerEvent) => {
      if (!containerRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener("pointerdown", onPointerDown);
    return () => document.removeEventListener("pointerdown", onPointerDown);
  }, [open]);

  const onButtonKeyDown = (event: KeyboardEvent) => {
    if (event.key === "ArrowDown" && !open) {
      event.preventDefault();
      openMenu();
    }
  };

  const onMenuKeyDown = (event: KeyboardEvent) => {
    const all = items();
    const index = all.indexOf(document.activeElement as HTMLElement);
    const focusAt = (i: number) => all[(i + all.length) % all.length]?.focus();

    if (event.key === "ArrowDown") focusAt(index + 1);
    else if (event.key === "ArrowUp") focusAt(index - 1);
    else if (event.key === "Home") focusAt(0);
    else if (event.key === "End") focusAt(all.length - 1);
    else if (event.key === "Escape") close(true);
    else if (event.key === "Tab") close(false);
    else return;
    if (event.key !== "Tab") event.preventDefault();
  };

  const chooseUser = (picked: UserResponse) => {
    switchUser(picked);
    close(true);
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        aria-controls={open ? menuId : undefined}
        aria-expanded={open}
        aria-haspopup="menu"
        aria-label={userName ? `Open user menu for ${userName}` : "Open user menu"}
        className="flex size-[34px] items-center justify-center rounded-full"
        onClick={() => (open ? close(false) : openMenu())}
        onKeyDown={onButtonKeyDown}
        ref={buttonRef}
        title={userName ?? "Not signed in"}
        type="button"
      >
        <UserAvatar sizeClassName="size-[34px]" user={user} />
      </button>

      {open && (
        <div
          aria-label="Account"
          className="absolute right-0 top-[calc(100%+8px)] z-30 w-[288px] rounded-[10px] border border-[#e6ebe8] bg-white p-1.5 text-[#25362f] shadow-[0_8px_24px_rgba(37,54,47,0.14)]"
          id={menuId}
          onKeyDown={onMenuKeyDown}
          ref={menuRef}
          role="menu"
        >
          {/* Who you are; not an item, so arrow keys skip it */}
          <div className="flex items-center gap-3 px-3 pb-3 pt-2.5" role="none">
            <UserAvatar sizeClassName="size-9" user={user} />
            <div className="min-w-0 flex-1">
              <p className="font-inter-semibold truncate text-[13px]">
                {userName ?? "Not signed in"}
              </p>
              {user?.email && (
                <p className="font-inter-regular truncate text-xs text-[#68766f]">
                  {user.email}
                </p>
              )}
            </div>
            {user?.role && (
              <span className="font-inter-semibold shrink-0 rounded bg-[#e8f7ed] px-1.5 py-0.5 text-[10px] text-[#16834a]">
                {user.role}
              </span>
            )}
          </div>

          {/* TODO(auth): remove Switch user (or keep it as admin-only impersonation,
              if the backend adds support for that); it only exists because there's no login */}
          {import.meta.env.DEV && (
            <div
              aria-labelledby={`${menuId}-switch`}
              className="border-t border-[#e6ebe8] pt-1.5"
              role="group"
            >
              <p
                className="font-inter-semibold px-3 pb-1 pt-1 text-[10px] text-[#98a29c]"
                id={`${menuId}-switch`}
              >
                SWITCH USER · DEV ONLY
              </p>
              <div className="max-h-[240px] overflow-y-auto">
                {userList.status === "loading" && (
                  <p className="font-inter-regular px-3 py-2 text-[13px] text-[#98a29c]" role="none">
                    Loading users…
                  </p>
                )}
                {userList.status === "error" && (
                  <p className="font-inter-regular px-3 py-2 text-[13px] text-[#98a29c]" role="none">
                    Couldn't load users.
                  </p>
                )}
                {userList.status === "loaded" && userList.users.length === 0 && (
                  <p className="font-inter-regular px-3 py-2 text-[13px] text-[#98a29c]" role="none">
                    No users yet. Create one with POST /api/v1/users.
                  </p>
                )}
                {userList.status === "loaded" &&
                  userList.users.map((option) => {
                    const checked = option.userId === user?.userId;
                    return (
                      <button
                        aria-checked={checked}
                        className={`${itemClass} hover:bg-[#f8faf9] focus:bg-[#f1f4f2]`}
                        key={option.userId}
                        onClick={() => chooseUser(option)}
                        role="menuitemradio"
                        tabIndex={-1}
                        type="button"
                      >
                        <UserAvatar sizeClassName="size-6" user={option} />
                        <span className="min-w-0 flex-1">
                          <span className="font-inter-medium block truncate">
                            {getDisplayName(option)}
                          </span>
                          <span className="font-inter-regular block truncate text-[11px] text-[#98a29c]">
                            {option.role}
                          </span>
                        </span>
                        {checked && (
                          <Icon className="text-[#16834a]" icon={TbCheck} size={16} />
                        )}
                      </button>
                    );
                  })}
              </div>
            </div>
          )}

          {/* TODO(auth): enable Sign out: drop aria-disabled and the "Needs login" label,
              call the auth provider's sign-out, clear the current user, and go to
              the sign-in page. When signed out, show a "Sign in" item instead. */}
          <div className="mt-1.5 border-t border-[#e6ebe8] pt-1.5">
            <button
              aria-disabled="true"
              className={`${itemClass} cursor-not-allowed text-[#b0bab5] focus:bg-[#f8faf9]`}
              role="menuitem"
              tabIndex={-1}
              title="Available once login is added"
              type="button"
            >
              <Icon icon={TbLogout} size={16} />
              <span className="font-inter-regular flex-1">Sign out</span>
              <span className="font-inter-regular text-[11px]">Needs login</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
