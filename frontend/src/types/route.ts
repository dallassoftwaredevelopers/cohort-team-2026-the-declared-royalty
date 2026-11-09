import type { IconType } from "react-icons";

export type AppSection = "WORKSPACE" | "OPERATIONS" | "OVERSIGHT";

export type AppRoute = {
    label: string,
    path: string,
    section: AppSection,
    // A react-icons component, e.g. TbBlocks (the component, not <TbBlocks />)
    icon: IconType,
}
