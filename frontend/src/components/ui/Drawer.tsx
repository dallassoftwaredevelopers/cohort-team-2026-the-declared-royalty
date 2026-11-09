import { type ReactNode, useEffect, useRef } from "react";

type DrawerProps = {
  id: string;
  open: boolean;
  onClose: () => void;
  side: "left" | "right";
  // Tailwind width classes, e.g. "w-[264px]"
  widthClassName: string;
  "aria-label"?: string;
  "aria-labelledby"?: string;
  children: ReactNode;
};

// Full strings so Tailwind can find the classes
const sideClasses = {
  left: "left-0 right-auto border-r -translate-x-full starting:open:-translate-x-full",
  right: "left-auto right-0 border-l translate-x-full starting:open:translate-x-full",
};

// A panel that slides in over the page. It is a native modal <dialog>, so the
// browser provides Esc-to-close, the backdrop, the focus trap, and returning
// focus to the button that opened it.
export default function Drawer({
  id,
  open,
  onClose,
  side,
  widthClassName,
  children,
  ...labelProps
}: DrawerProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog) return;
    if (open && !dialog.open) dialog.showModal();
    if (!open && dialog.open) dialog.close();
  }, [open]);

  return (
    <dialog
      {...labelProps}
      className={`fixed inset-y-0 m-0 h-dvh max-h-none max-w-[85vw] border-0 border-[#e6ebe8] bg-white p-0 text-[#25362f] transition-[translate,overlay,display] transition-discrete duration-200 open:translate-x-0 backdrop:bg-[#25362f]/30 ${sideClasses[side]} ${widthClassName}`}
      id={id}
      // Clicks on the backdrop land on the <dialog> itself; clicks inside land on its children
      onClick={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
      // Fires for Esc as well as dialog.close()
      onClose={onClose}
      ref={dialogRef}
    >
      <div className="flex h-full flex-col">{children}</div>
    </dialog>
  );
}
