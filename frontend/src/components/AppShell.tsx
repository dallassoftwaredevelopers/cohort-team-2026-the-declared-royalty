import { type ReactNode, useState } from "react";

const assetPath = (name: string) => `/assets/${name}`;

export type AppRoute = {
  label: string;
  path: string;
  section: "WORKSPACE" | "OPERATIONS" | "OVERSIGHT";
  icon: string | ReactNode;
};

type AppShellProps = {
  children: ReactNode;
  currentRoute: AppRoute;
  onNavigate: (path: string) => void;
  routes: AppRoute[];
};

const sections: AppRoute["section"][] = [
  "WORKSPACE",
  "OPERATIONS",
  "OVERSIGHT",
];

function Icon({
  file,
  size = 18,
  className = "",
}: {
  file: string;
  size?: number;
  className?: string;
}) {
  return (
    <img
      alt=""
      aria-hidden="true"
      className={`shrink-0 ${className}`}
      height={size}
      src={assetPath(file)}
      width={size}
    />
  );
}

export default function AppShell({
  children,
  currentRoute,
  onNavigate,
  routes,
}: AppShellProps) {
  const [sidebarOpen, setSidebarOpen] = useState(true);
  const [activityOpen, setActivityOpen] = useState(true);

  return (
    <div className="flex h-dvh min-h-[600px] w-full overflow-hidden bg-[#f8faf9] text-[#25362f]">
      <aside
        className={`hidden h-full shrink-0 flex-col overflow-hidden border-r border-[#e6ebe8] bg-white transition-[width] duration-200 md:flex ${
          sidebarOpen ? "w-[264px]" : "w-[76px]"
        }`}
      >
        <div
          className={`flex h-[100px] shrink-0 items-center gap-3 ${
            sidebarOpen ? "px-6" : "px-[22px]"
          }`}
        >
          <div className="flex size-8 shrink-0 items-center justify-center rounded-[9px] bg-[#16834a]">
            <Icon file="af1f8.svg" size={19} />
          </div>
          {sidebarOpen && (
            <p className="font-inter-semibold text-[15px] leading-[21px]">
              Property Bill Automation
            </p>
          )}
        </div>

        <nav
          aria-label="Primary navigation"
          className="flex flex-col gap-[26px] px-4 pt-3"
        >
          {sections.map((section) => {
            const sectionRoutes = routes.filter(
              (route) => route.section === section,
            );
            return (
              <div className="flex flex-col gap-2" key={section}>
                {sidebarOpen && (
                  <p className="font-inter-semibold text-[10px] text-[#98a29c]">
                    {section}
                  </p>
                )}
                <div className="flex flex-col gap-1">
                  {sectionRoutes.map((route) => {
                    const active = route.path === currentRoute.path;
                    return (
                      <button
                        aria-current={active ? "page" : undefined}
                        aria-label={route.label}
                        className={`flex h-11 w-full items-center gap-3 rounded-[7px] px-3 text-left transition-colors ${
                          active
                            ? "bg-[#e8f7ed] text-[#14532d]"
                            : "bg-white text-[#68766f] hover:bg-[#f8faf9]"
                        } ${sidebarOpen ? "" : "justify-center"}`}
                        key={route.path}
                        onClick={() => onNavigate(route.path)}
                        title={!sidebarOpen ? route.label : undefined}
                        type="button"
                      >
                        {typeof route.icon === "string" ? (
                          <Icon file={route.icon} />
                        ) : (
                          route.icon
                        )}
                        {sidebarOpen && (
                          <span
                            className={
                              active
                                ? "font-inter-semibold text-sm"
                                : "font-inter-regular text-sm"
                            }
                          >
                            {route.label}
                          </span>
                        )}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </nav>

        <div className="flex-1" />

        <div className="flex flex-col gap-2 px-4 pb-4">
          <button
            aria-label={sidebarOpen ? "Collapse sidebar" : "Expand sidebar"}
            className={`flex h-9 items-center gap-3 px-3 text-[#98a29c] ${
              sidebarOpen ? "" : "justify-center"
            }`}
            onClick={() => setSidebarOpen((open) => !open)}
            type="button"
          >
            <Icon
              file="ff806.svg"
              size={17}
              className={sidebarOpen ? "" : "rotate-180"}
            />
            {sidebarOpen && (
              <span className="font-inter-regular text-xs">
                Collapse sidebar
              </span>
            )}
          </button>
          <button
            className={`flex h-11 items-center gap-3 rounded-[7px] px-3 text-[#68766f] hover:bg-[#f8faf9] ${
              sidebarOpen ? "" : "justify-center"
            }`}
            type="button"
          >
            <Icon file="481b4.svg" />
            {sidebarOpen && (
              <span className="font-inter-regular text-sm">Settings</span>
            )}
          </button>
        </div>

        <button
          className={`flex h-[84px] shrink-0 items-center gap-3 border-t border-[#e6ebe8] text-left ${
            sidebarOpen ? "px-6" : "justify-center px-[22px]"
          }`}
          type="button"
        >
          <span className="flex size-8 shrink-0 items-center justify-center rounded-full bg-[#f1f4f2]">
            <Icon file="3a987.svg" size={17} />
          </span>
          {sidebarOpen && (
            <>
              <span className="flex min-w-0 flex-1 flex-col gap-1">
                <span className="font-inter-medium text-xs text-[#68766f]">
                  User profile
                </span>
                <span className="font-inter-regular text-[11px] text-[#98a29c]">
                  Your account
                </span>
              </span>
              <Icon file="76ed3.svg" size={14} />
            </>
          )}
        </button>
      </aside>

      <main className="flex min-w-0 flex-1 flex-col bg-[#f8faf9]">
        <header className="shrink-0">
          <div className="flex h-[60px] items-center justify-between border-b border-[#e6ebe8] bg-white px-4 sm:pl-8 sm:pr-6">
            <label className="flex h-[34px] w-[180px] items-center gap-2 rounded-[7px] border border-[#e6ebe8] bg-[#f8faf9] px-3 sm:w-[220px]">
              <Icon file="97f77.svg" size={14} />
              <input
                aria-label="Search"
                className="font-inter-regular min-w-0 flex-1 bg-transparent text-[13px] text-[#68766f] outline-none placeholder:text-[#b0bab5]"
                placeholder="Search anything…"
                type="search"
              />
            </label>
            <div className="flex items-center gap-2">
              <button
                aria-label="Notifications"
                className="flex size-[34px] items-center justify-center rounded-[7px] border border-[#e6ebe8] bg-white"
                type="button"
              >
                <Icon file="f9f2a.svg" size={16} />
              </button>
              <button
                aria-label="Open user menu"
                className="font-inter-semibold flex size-[34px] items-center justify-center rounded-full bg-[#e8f7ed] text-xs text-[#16834a]"
                type="button"
              >
                U
              </button>
            </div>
          </div>
          <div className="flex flex-col gap-2.5 px-5 pb-6 pt-7 sm:px-8">
            <div className="flex items-center gap-2 text-[13px]">
              <span className="font-inter-regular text-[#98a29c]">
                Workspace
              </span>
              <Icon file="a359d.svg" size={12} />
              <span className="font-inter-regular text-[#68766f]">
                {currentRoute.label}
              </span>
            </div>
            <h1 className="font-inter-semibold text-[28px] leading-[1.2] text-[#14532d]">
              {currentRoute.label}
            </h1>
          </div>
        </header>

        <section
          aria-label={`${currentRoute.label} page content`}
          className="min-h-0 flex-1 overflow-y-auto px-5 pb-8 sm:px-8"
        >
          {children}
        </section>
      </main>

      {activityOpen ? (
        <aside className="hidden h-full w-[288px] shrink-0 flex-col border-l border-[#e6ebe8] bg-white xl:flex">
          <div className="flex h-[60px] shrink-0 items-center gap-3 border-b border-[#e6ebe8] px-5">
            <p className="font-inter-semibold min-w-0 flex-1 text-[13px]">
              Activity &amp; Notifications
            </p>
            <button
              aria-label="Collapse activity panel"
              className="flex size-7 items-center justify-center rounded-md border border-[#e6ebe8]"
              onClick={() => setActivityOpen(false)}
              type="button"
            >
              <Icon file="2a42c.svg" size={14} />
            </button>
          </div>
          <div className="px-5">
            {["Recent activity", "Alerts", "Quick actions"].map(
              (label, index) => (
                <section
                  className={`h-[164px] pt-6 ${
                    index < 2 ? "border-b border-[#e6ebe8]" : ""
                  }`}
                  key={label}
                >
                  <h2 className="font-inter-semibold text-xs text-[#68766f]">
                    {label}
                  </h2>
                </section>
              ),
            )}
          </div>
        </aside>
      ) : (
        <button
          aria-label="Expand activity panel"
          className="absolute right-3 top-4 z-10 hidden size-7 items-center justify-center rounded-md border border-[#e6ebe8] bg-white xl:flex"
          onClick={() => setActivityOpen(true)}
          type="button"
        >
          <Icon file="2a42c.svg" size={14} className="rotate-180" />
        </button>
      )}
    </div>
  );
}
