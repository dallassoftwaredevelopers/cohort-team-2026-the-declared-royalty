import { TbBell } from "react-icons/tb";
import Icon from "@/components/ui/Icon";
import type { CurrentUser } from "@/features/users/useCurrentUser";
import UserMenu from "@/features/users/UserMenu";
import { ACTIVITY_DRAWER_ID } from "../activity/ActivityDrawer";
import SearchBox from "./SearchBox";

type TopBarProps = {
  activityOpen: boolean;
  currentUser: CurrentUser;
  onOpenActivity: () => void;
};

export default function TopBar({
  activityOpen,
  currentUser,
  onOpenActivity,
}: TopBarProps) {
  return (
    // pl-16 leaves room for the floating SidebarToggle at the top-left
    <div className="flex h-[60px] items-center justify-between border-b border-[#e6ebe8] bg-white pl-16 pr-4 sm:pr-6">
      <SearchBox />
      <div className="flex items-center gap-2">
        <button
          aria-controls={ACTIVITY_DRAWER_ID}
          aria-expanded={activityOpen}
          aria-haspopup="dialog"
          aria-label="Notifications"
          className="flex size-[34px] items-center justify-center rounded-[7px] border border-[#e6ebe8] bg-white text-[#68766f] hover:bg-[#f8faf9]"
          onClick={onOpenActivity}
          type="button"
        >
          <Icon icon={TbBell} size={16} />
        </button>
        <UserMenu currentUser={currentUser} />
      </div>
    </div>
  );
}
