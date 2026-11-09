import { TbX } from "react-icons/tb";
import Drawer from "@/components/ui/Drawer";
import Icon from "@/components/ui/Icon";
import ActivitySection from "./ActivitySection";

type ActivityDrawerProps = {
  open: boolean;
  onClose: () => void;
};

export const ACTIVITY_DRAWER_ID = "activity-drawer";

// Opened from the bell in the top bar
export default function ActivityDrawer({ open, onClose }: ActivityDrawerProps) {
  return (
    <Drawer
      aria-labelledby={`${ACTIVITY_DRAWER_ID}-title`}
      id={ACTIVITY_DRAWER_ID}
      onClose={onClose}
      open={open}
      side="right"
      widthClassName="w-[320px]"
    >
      <div className="flex h-[60px] shrink-0 items-center gap-3 border-b border-[#e6ebe8] px-5">
        <h2
          className="font-inter-semibold min-w-0 flex-1 text-[13px]"
          id={`${ACTIVITY_DRAWER_ID}-title`}
        >
          Activity &amp; Notifications
        </h2>
        <button
          aria-label="Close activity and notifications"
          className="flex size-7 shrink-0 items-center justify-center rounded-md border border-[#e6ebe8] text-[#68766f]"
          onClick={onClose}
          type="button"
        >
          <Icon icon={TbX} size={14} />
        </button>
      </div>
      <div className="min-h-0 flex-1 overflow-y-auto px-5">
        {/* Filled from invoice exceptions once the invoice endpoints exist */}
        <ActivitySection divider title="Alerts">
          <p className="font-inter-regular text-[13px] text-[#98a29c]">
            No alerts right now.
          </p>
        </ActivitySection>
        {/* Filled from GET /api/v1/audit-logs once there is an auditLogService */}
        <ActivitySection title="Recent activity">
          <p className="font-inter-regular text-[13px] text-[#98a29c]">
            No recent activity yet.
          </p>
        </ActivitySection>
      </div>
    </Drawer>
  );
}
