import { TbUser } from "react-icons/tb";
import Icon from "@/components/ui/Icon";
import type { UserResponse } from "@/types/user";
import { getDisplayName, getInitials } from "./userDisplay";

type UserAvatarProps = {
  user: UserResponse | null;
  // Tailwind size class, e.g. "size-8"
  sizeClassName: string;
};

// Initials in a green circle, or a generic person icon when there's no user
export default function UserAvatar({ user, sizeClassName }: UserAvatarProps) {
  if (!user) {
    return (
      <span
        className={`flex shrink-0 items-center justify-center rounded-full bg-[#f1f4f2] text-[#68766f] ${sizeClassName}`}
      >
        <Icon icon={TbUser} size={17} />
      </span>
    );
  }

  return (
    <span
      aria-hidden="true"
      className={`font-inter-semibold flex shrink-0 items-center justify-center rounded-full bg-[#e8f7ed] text-xs text-[#16834a] ${sizeClassName}`}
    >
      {getInitials(getDisplayName(user))}
    </span>
  );
}
