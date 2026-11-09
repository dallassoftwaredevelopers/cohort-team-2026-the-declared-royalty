import type { IconType } from "react-icons";

type IconProps = {
  icon: IconType;
  size?: number;
  className?: string;
};

// Decorative icon, hidden from screen readers. It takes its color from the
// surrounding text (currentColor), so set a text color on it or its parent.
export default function Icon({
  icon: IconComponent,
  size = 18,
  className = "",
}: IconProps) {
  return (
    <IconComponent
      aria-hidden="true"
      className={`shrink-0 ${className}`}
      size={size}
    />
  );
}
