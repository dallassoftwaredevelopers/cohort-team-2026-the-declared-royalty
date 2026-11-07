import { type FormEvent, type ReactNode, useEffect, useId, useRef, useState } from "react";
import { FiAlertCircle, FiAlertTriangle, FiTrash2, FiX } from "react-icons/fi";

// Offered in edit forms: a Delete button that needs two confirmations
export type FormModalDeletion = {
  // Singular, e.g. "vendor"
  noun: string;
  // What the user must type to confirm, e.g. the vendor's name
  name: string;
  // Extra guidance in the first step, e.g. a less destructive alternative
  note?: string;
  // Deletes; throwing shows the error
  confirm: () => Promise<void>;
  describeError: (error: unknown) => string;
};

type FormModalProps = {
  title: string;
  subtitle: ReactNode;
  // e.g. "font-mono text-xs" to show an ID
  subtitleClassName?: string;
  submitLabel: string;
  savingLabel: string;
  submitIcon: ReactNode;
  // `name` of the field to focus when the form opens
  initialFocusName: string;
  // Runs on submit; return false to stay open (e.g. to show validation errors)
  validate: () => boolean;
  // Saves; return null when there's nothing to save. Throwing shows the error.
  save: () => Promise<void> | null;
  describeError: (error: unknown) => string;
  // Starts the error message, e.g. "Couldn't save changes."
  errorPrefix: string;
  deletion?: FormModalDeletion;
  onClose: () => void;
  // The fields, plus any extra messages (e.g. a duplicate warning)
  children: ReactNode;
};

// "form": editing. "confirm": first confirmation. "type": second confirmation (type the name).
type Step = "form" | "confirm" | "type";

const secondaryButton =
  "font-inter-medium h-9 rounded-[7px] border border-[#e6ebe8] bg-white px-4 text-sm text-[#25362f] hover:bg-[#f8faf9] disabled:opacity-50";
const dangerButton =
  "font-inter-semibold flex h-9 items-center gap-2 rounded-[7px] bg-[#b42318] px-4 text-sm text-white hover:bg-[#912018] disabled:cursor-not-allowed disabled:opacity-50";

// A form in a native modal <dialog>: the browser provides the focus trap, Esc,
// and returning focus to the button that opened it. While saving or deleting,
// the fields are disabled and the form can't be closed.
export default function FormModal({
  title,
  subtitle,
  subtitleClassName = "font-inter-regular text-sm",
  submitLabel,
  savingLabel,
  submitIcon,
  initialFocusName,
  validate,
  save,
  describeError,
  errorPrefix,
  deletion,
  onClose,
  children,
}: FormModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const formRef = useRef<HTMLFormElement>(null);
  const keepRef = useRef<HTMLButtonElement>(null);
  const typedRef = useRef<HTMLInputElement>(null);
  const titleId = useId();
  const typedId = useId();
  const [step, setStep] = useState<Step>("form");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [typed, setTyped] = useState("");

  const nameMatches = deletion ? typed.trim() === deletion.name.trim() : false;

  useEffect(() => {
    dialogRef.current?.showModal();
    // showModal() focuses the first button (Close); start in the first field instead.
    // React's autoFocus doesn't help here: it runs before showModal() moves focus.
    formRef.current?.querySelector<HTMLElement>(`[name="${initialFocusName}"]`)?.focus();
  }, [initialFocusName]);

  // Each delete step starts on its safest control: Keep, then the name box
  useEffect(() => {
    if (step === "confirm") keepRef.current?.focus();
    if (step === "type") typedRef.current?.focus();
  }, [step]);

  const close = () => dialogRef.current?.close();

  const goTo = (next: Step) => {
    setError(null);
    setTyped("");
    setStep(next);
    if (next === "form") {
      // Back to editing: put focus back where the delete flow started
      requestAnimationFrame(() => formRef.current?.querySelector<HTMLElement>("[data-delete-start]")?.focus());
    }
  };

  const run = async (action: Promise<void>, describe: (error: unknown) => string) => {
    setBusy(true);
    try {
      await action;
      close();
    } catch (actionError) {
      setError(describe(actionError));
      setBusy(false);
    }
  };

  const onSubmit = (event: FormEvent) => {
    event.preventDefault();
    // Enter while confirming a delete must never save the edit
    if (step === "type") {
      if (deletion && nameMatches) void run(deletion.confirm(), deletion.describeError);
      return;
    }
    if (step !== "form" || !validate()) return;

    const pending = save();
    if (!pending) close();
    else void run(pending, describeError);
  };

  return (
    <dialog
      aria-labelledby={titleId}
      className="m-auto max-h-[90dvh] w-[calc(100vw-2rem)] max-w-[520px] overflow-hidden rounded-xl border-0 bg-white p-0 text-[#25362f] shadow-xl backdrop:bg-black/30"
      // Keep the form open while it's working, even on Esc
      onCancel={(event) => {
        if (busy) event.preventDefault();
      }}
      onClose={onClose}
      // Clicks on the backdrop land on the <dialog> itself
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !busy) close();
      }}
      ref={dialogRef}
    >
      <form
        className="flex max-h-[90dvh] flex-col"
        noValidate
        // Any edit clears a previous error
        onChange={() => setError(null)}
        onSubmit={onSubmit}
        ref={formRef}
      >
        {/* Header */}
        <div className="flex items-start justify-between gap-4 px-6 pt-6">
          <div className="min-w-0">
            <h2 className="font-inter-semibold text-xl text-[#14532d]" id={titleId}>
              {title}
            </h2>
            <p className={`mt-1 truncate text-[#68766f] ${subtitleClassName}`}>{subtitle}</p>
          </div>
          <button
            aria-label="Close"
            className="flex size-8 shrink-0 items-center justify-center rounded-md text-[#68766f] hover:bg-[#f8faf9] disabled:opacity-40"
            disabled={busy}
            onClick={close}
            type="button"
          >
            <FiX size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          {/* Hidden (not unmounted) while confirming, so edits aren't lost on "Keep" */}
          <fieldset className="m-0 min-w-0 border-0 p-0" disabled={busy} hidden={step !== "form"}>
            {children}
          </fieldset>

          {deletion && step !== "form" && (
            <div className="rounded-lg border border-[#fecdca] bg-[#fef3f2] p-4" role="group" aria-labelledby={`${titleId}-delete`}>
              <p className="font-inter-semibold flex items-center gap-2 text-sm text-[#b42318]" id={`${titleId}-delete`}>
                <FiAlertTriangle aria-hidden="true" size={16} />
                {step === "confirm" ? `Delete ${deletion.name}?` : `Confirm deleting ${deletion.name}`}
              </p>
              {step === "confirm" ? (
                <p className="font-inter-regular mt-2 text-sm text-[#25362f]">
                  This permanently removes the {deletion.noun} and can't be undone.
                  {deletion.note && <> {deletion.note}</>}
                </p>
              ) : (
                <div className="mt-3 flex flex-col gap-1.5">
                  <label className="font-inter-regular text-sm text-[#25362f]" htmlFor={typedId}>
                    Type <span className="font-inter-semibold">{deletion.name}</span> to confirm.
                  </label>
                  <input
                    autoComplete="off"
                    className="font-inter-regular h-9 rounded-[7px] border border-[#e6ebe8] bg-white px-3 text-sm outline-none focus:border-[#b42318] focus:ring-2 focus:ring-[#b42318]/20 disabled:bg-[#f8faf9]"
                    disabled={busy}
                    id={typedId}
                    onChange={(event) => setTyped(event.target.value)}
                    ref={typedRef}
                    spellCheck={false}
                    type="text"
                    value={typed}
                  />
                </div>
              )}
            </div>
          )}

          {error && (
            <p className="mt-4 flex items-start gap-2 text-sm text-[#b42318]" role="alert">
              <FiAlertCircle className="mt-0.5 shrink-0" size={16} />
              <span>
                {step === "form" ? errorPrefix : `Couldn't delete the ${deletion?.noun}.`} {error}
              </span>
            </p>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-2 border-t border-[#e6ebe8] px-6 py-4">
          {step === "form" ? (
            <>
              {deletion && (
                <button
                  className="font-inter-medium mr-auto flex h-9 items-center gap-1.5 rounded-[7px] px-2 text-sm text-[#b42318] hover:bg-[#fef3f2] disabled:opacity-50"
                  data-delete-start
                  disabled={busy}
                  onClick={() => goTo("confirm")}
                  type="button"
                >
                  <FiTrash2 aria-hidden="true" size={15} />
                  Delete {deletion.noun}
                </button>
              )}
              <button className={secondaryButton} disabled={busy} onClick={close} type="button">
                Cancel
              </button>
              <button
                className="font-inter-semibold flex h-9 items-center gap-2 rounded-[7px] bg-[#16834a] px-4 text-sm text-white hover:bg-[#13723f] disabled:cursor-not-allowed disabled:opacity-50"
                disabled={busy}
                type="submit"
              >
                {submitIcon}
                {busy ? savingLabel : submitLabel}
              </button>
            </>
          ) : (
            <>
              <button className={secondaryButton} disabled={busy} onClick={() => goTo("form")} ref={keepRef} type="button">
                Keep {deletion?.noun}
              </button>
              {step === "confirm" ? (
                <button className={dangerButton} onClick={() => goTo("type")} type="button">
                  Yes, delete
                </button>
              ) : (
                <button className={dangerButton} disabled={busy || !nameMatches} type="submit">
                  <FiTrash2 aria-hidden="true" size={15} />
                  {busy ? "Deleting…" : "Delete permanently"}
                </button>
              )}
            </>
          )}
        </div>
      </form>
    </dialog>
  );
}

// Shared look for warnings shown inside a FormModal (e.g. a duplicate)
export function FormAlert({ children }: { children: ReactNode }) {
  return (
    <p className="mt-4 flex items-start gap-2 text-sm text-[#b42318]" role="alert">
      <FiAlertCircle className="mt-0.5 shrink-0" size={16} />
      <span>{children}</span>
    </p>
  );
}
