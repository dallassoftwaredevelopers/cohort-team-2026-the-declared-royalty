import { useEffect, useRef, useState } from "react";
import {
  FiAlertCircle,
  FiCheckCircle,
  FiFileText,
  FiImage,
  FiLoader,
  FiUpload,
  FiUploadCloud,
  FiX,
} from "react-icons/fi";
import {
  ApiError,
  checkExistingHashes,
  uploadInvoice,
} from "./invoiceUploadService";
import { sha256Hex } from "./fileHash";

const MAX_FILE_SIZE = 5 * 1024 * 1024;
const ACCEPTED_TYPES = ["application/pdf", "image/png", "image/jpeg"];

type Phase = "pending" | "invalid" | "uploading" | "uploaded" | "failed";

type SelectedFile = {
  id: string;
  file: File;
  phase: Phase;
  hash?: string;
  checked?: boolean; // server duplicate check finished (or was attempted)
  message?: string;
};

type DisplayStatus =
  | "checking"
  | "unique"
  | "duplicate-existing"
  | "duplicate-batch"
  | Exclude<Phase, "pending">;

const ERROR_STATUSES: DisplayStatus[] = [
  "duplicate-existing",
  "duplicate-batch",
  "invalid",
  "failed",
];

const STATUS_TEXT: Record<DisplayStatus, string> = {
  checking: "Checking file…",
  unique: "Hash check: unique file",
  "duplicate-existing": "Hash check: exact match found",
  "duplicate-batch": "Same file selected twice",
  invalid: "Invalid file",
  uploading: "Uploading…",
  uploaded: "Uploaded",
  failed: "Upload failed",
};

// Status is derived, not stored, so removing a file re-evaluates the others
function getStatus(
  entry: SelectedFile,
  index: number,
  files: SelectedFile[],
  existingHashes: Set<string>,
): DisplayStatus {
  if (entry.phase !== "pending") return entry.phase;
  if (!entry.hash || !entry.checked) return "checking";
  if (existingHashes.has(entry.hash)) return "duplicate-existing";
  const selectedEarlier = files
    .slice(0, index)
    .some((other) => other.hash === entry.hash && other.phase !== "invalid");
  return selectedEarlier ? "duplicate-batch" : "unique";
}

// Quick client-side checks; the server re-checks the real file contents
function validateFile(file: File): string | undefined {
  if (!ACCEPTED_TYPES.includes(file.type)) return "File type invalid: only PDF, PNG and JPEG";
  if (file.size === 0) return "File is empty";
  if (file.size > MAX_FILE_SIZE) return "File too large: maximum size is 5MB";
  return undefined;
}

function formatSize(bytes: number): string {
  return `${Math.max(bytes / (1024 * 1024), 0.1).toFixed(1)}MB`;
}

const plural = (n: number, word: string) => `${n} ${word}${n === 1 ? "" : "s"}`;

type UploadInvoicesModalProps = {
  onClose: () => void;
  onUploaded?: () => void;
};

export default function UploadInvoicesModal({ onClose, onUploaded }: UploadInvoicesModalProps) {
  const [files, setFiles] = useState<SelectedFile[]>([]);
  const [existingHashes, setExistingHashes] = useState<Set<string>>(new Set());
  const [checkWarning, setCheckWarning] = useState<string | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [isUploading, setIsUploading] = useState(false);
  const [finished, setFinished] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const statuses = files.map((entry, i) => getStatus(entry, i, files, existingHashes));
  const count = (status: DisplayStatus) => statuses.filter((s) => s === status).length;

  const uniqueCount = count("unique");
  const invalidCount = count("invalid");
  const checkingCount = count("checking");
  const uploadedCount = count("uploaded");
  const failedCount = count("failed");
  const duplicateNames = files
    .filter((_, i) => statuses[i] === "duplicate-existing" || statuses[i] === "duplicate-batch")
    .map((entry) => entry.file.name);

  const canUpload =
    !isUploading &&
    uniqueCount > 0 &&
    duplicateNames.length === 0 &&
    invalidCount === 0 &&
    checkingCount === 0;

  useEffect(() => {
    const handleKey = (event: KeyboardEvent) => {
      if (event.key === "Escape" && !isUploading) onClose();
    };
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [isUploading, onClose]);

  const updateFile = (id: string, patch: Partial<SelectedFile>) =>
    setFiles((prev) =>
      prev.map((entry): SelectedFile => (entry.id === id ? { ...entry, ...patch } : entry)),
    );

  const addFiles = async (picked: File[]) => {
    if (picked.length === 0 || isUploading) return;
    setFinished(false);

    const entries = picked.map((file): SelectedFile => {
      const error = validateFile(file);
      return {
        id: crypto.randomUUID(),
        file,
        phase: error ? "invalid" : "pending",
        message: error,
      };
    });
    setFiles((prev) => [...prev, ...entries]);

    // 1. Hash in the browser
    const hashed = await Promise.all(
      entries
        .filter((entry) => entry.phase === "pending")
        .map(async (entry) => {
          try {
            return { id: entry.id, hash: await sha256Hex(entry.file) };
          } catch {
            return { id: entry.id, hash: undefined };
          }
        }),
    );

    setFiles((prev) =>
      prev.map((entry): SelectedFile => {
        const result = hashed.find((h) => h.id === entry.id);
        if (!result) return entry;
        return result.hash
          ? { ...entry, hash: result.hash }
          : { ...entry, phase: "invalid", message: "Could not read file" };
      }),
    );

    const hashes = hashed.flatMap((h) => (h.hash ? [h.hash] : []));
    if (hashes.length === 0) return;

    // 2. Ask the server which hashes already exist
    try {
      const duplicates = await checkExistingHashes(hashes);
      setExistingHashes((prev) => new Set([...prev, ...duplicates]));
      setCheckWarning(null);
    } catch (error) {
      console.error("Duplicate check failed", error);
      setCheckWarning(
        "Couldn't check for duplicates right now. Any duplicates will still be caught when you upload.",
      );
    }

    const checkedIds = new Set(hashed.map((h) => h.id));
    setFiles((prev) =>
      prev.map((entry): SelectedFile =>
        checkedIds.has(entry.id) ? { ...entry, checked: true } : entry,
      ),
    );
  };

  const removeFile = (id: string) => setFiles((prev) => prev.filter((entry) => entry.id !== id));

  const handleUpload = async () => {
    if (!canUpload) return;
    setIsUploading(true);

    // One at a time, so each file gets its own result
    const queue = files.filter((_, i) => statuses[i] === "unique");
    let succeeded = 0;

    for (const entry of queue) {
      updateFile(entry.id, { phase: "uploading" });
      try {
        await uploadInvoice(entry.file);
        updateFile(entry.id, { phase: "uploaded", message: undefined });
        const hash = entry.hash;
        if (hash) setExistingHashes((prev) => new Set(prev).add(hash));
        succeeded++;
      } catch (error) {
        updateFile(entry.id, {
          phase: "failed",
          message: error instanceof ApiError ? error.message : "Upload failed",
        });
      }
    }

    setIsUploading(false);
    setFinished(true);
    if (succeeded > 0) onUploaded?.();
  };

  const openPicker = () => {
    if (!isUploading) inputRef.current?.click();
  };

  const summary =
    files.length === 0
      ? "No files selected"
      : [
          uniqueCount > 0 && `${plural(uniqueCount, "unique file")}`,
          duplicateNames.length > 0 && plural(duplicateNames.length, "duplicate"),
          invalidCount > 0 && `${invalidCount} invalid`,
          uploadedCount > 0 && `${uploadedCount} uploaded`,
          failedCount > 0 && `${failedCount} failed`,
        ]
          .filter(Boolean)
          .join(" · ") || plural(files.length, "file");

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/30 p-4"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget && !isUploading) onClose();
      }}
    >
      <div
        aria-labelledby="upload-invoices-title"
        aria-modal="true"
        className="flex max-h-[90dvh] w-full max-w-[520px] flex-col overflow-hidden rounded-xl bg-white shadow-xl"
        role="dialog"
      >
        {/* Header */}
        <div className="flex items-start justify-between gap-4 px-6 pt-6">
          <div>
            <h2
              className="font-inter-semibold text-xl text-[#14532d]"
              id="upload-invoices-title"
            >
              Upload invoices
            </h2>
            <p className="font-inter-regular mt-1 text-sm text-[#68766f]">
              Add your bills to Property Bill Automation.
            </p>
          </div>
          <button
            aria-label="Close"
            className="flex size-8 items-center justify-center rounded-md text-[#68766f] hover:bg-[#f8faf9] disabled:opacity-40"
            disabled={isUploading}
            onClick={onClose}
            type="button"
          >
            <FiX size={18} />
          </button>
        </div>

        {/* Body */}
        <div className="min-h-0 flex-1 overflow-y-auto px-6 py-5">
          <div
            aria-disabled={isUploading}
            className={`flex flex-col items-center gap-1.5 rounded-lg border border-dashed px-6 py-8 text-center transition-colors ${
              isDragging ? "border-[#16834a] bg-[#e8f7ed]" : "border-[#b9d9c5] bg-[#f8faf9]"
            } ${isUploading ? "cursor-not-allowed opacity-60" : "cursor-pointer"}`}
            onClick={openPicker}
            onDragLeave={() => setIsDragging(false)}
            onDragOver={(event) => {
              event.preventDefault();
              if (!isUploading) setIsDragging(true);
            }}
            onDrop={(event) => {
              event.preventDefault();
              setIsDragging(false);
              void addFiles(Array.from(event.dataTransfer.files));
            }}
            onKeyDown={(event) => {
              if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                openPicker();
              }
            }}
            role="button"
            tabIndex={0}
          >
            <span className="mb-1 flex size-10 items-center justify-center rounded-lg bg-[#e8f7ed] text-[#16834a]">
              <FiUploadCloud size={20} />
            </span>
            <p className="font-inter-medium text-sm text-[#25362f]">Drag a PDF or image here</p>
            <p className="font-inter-regular text-sm text-[#68766f]">
              or <span className="font-inter-semibold text-[#16834a]">click to browse</span>
            </p>
            <p className="font-inter-regular text-xs text-[#98a29c]">
              PDF or image files · Max file size 5MB
            </p>
          </div>
          <input
            accept=".pdf,.png,.jpg,.jpeg"
            className="hidden"
            multiple
            onChange={(event) => {
              const picked = Array.from(event.target.files ?? []);
              event.target.value = ""; // allow picking the same file again
              void addFiles(picked);
            }}
            ref={inputRef}
            type="file"
          />

          {files.length > 0 && (
            <div className="mt-5">
              <div className="mb-2 flex items-center justify-between">
                <p className="font-inter-semibold text-sm text-[#25362f]">Selected files</p>
                <p className="font-inter-regular text-xs text-[#98a29c]">
                  {plural(files.length, "file")}
                </p>
              </div>

              <ul className="flex flex-col gap-2">
                {files.map((entry, i) => (
                  <FileRow
                    canRemove={!isUploading && statuses[i] !== "uploaded"}
                    entry={entry}
                    key={entry.id}
                    onRemove={() => removeFile(entry.id)}
                    status={statuses[i]}
                  />
                ))}
              </ul>

              {duplicateNames.length > 0 && (
                <p className="mt-3 flex items-start gap-2 text-sm text-[#b42318]" role="alert">
                  <FiAlertCircle className="mt-0.5 shrink-0" />
                  Duplicate file detected. Remove {duplicateNames.join(", ")} before uploading.
                </p>
              )}
              {invalidCount > 0 && (
                <p className="mt-3 flex items-start gap-2 text-sm text-[#b42318]" role="alert">
                  <FiAlertCircle className="mt-0.5 shrink-0" />
                  Remove invalid files before uploading.
                </p>
              )}
              {checkWarning && (
                <p className="mt-3 flex items-start gap-2 text-sm text-[#b45309]">
                  <FiAlertCircle className="mt-0.5 shrink-0" />
                  {checkWarning}
                </p>
              )}
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between gap-3 border-t border-[#e6ebe8] px-6 py-4">
          <p className="font-inter-regular text-xs text-[#68766f]">{summary}</p>
          <div className="flex gap-2">
            {finished && uniqueCount === 0 ? (
              <button
                className="font-inter-semibold flex h-9 items-center gap-2 rounded-[7px] bg-[#16834a] px-4 text-sm text-white hover:bg-[#13723f]"
                onClick={onClose}
                type="button"
              >
                Done
              </button>
            ) : (
              <>
                <button
                  className="font-inter-medium h-9 rounded-[7px] border border-[#e6ebe8] bg-white px-4 text-sm text-[#25362f] hover:bg-[#f8faf9] disabled:opacity-50"
                  disabled={isUploading}
                  onClick={onClose}
                  type="button"
                >
                  Cancel
                </button>
                <button
                  className="font-inter-semibold flex h-9 items-center gap-2 rounded-[7px] bg-[#16834a] px-4 text-sm text-white hover:bg-[#13723f] disabled:cursor-not-allowed disabled:opacity-50"
                  disabled={!canUpload}
                  onClick={() => void handleUpload()}
                  type="button"
                >
                  <FiUpload size={15} />
                  {isUploading ? "Uploading…" : "Upload"}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

type FileRowProps = {
  entry: SelectedFile;
  status: DisplayStatus;
  canRemove: boolean;
  onRemove: () => void;
};

function FileRow({ entry, status, canRemove, onRemove }: FileRowProps) {
  const isError = ERROR_STATUSES.includes(status);
  const isBusy = status === "checking" || status === "uploading";
  const isGood = status === "unique" || status === "uploaded";
  const isImage = entry.file.type.startsWith("image/");
  const text = entry.message ?? STATUS_TEXT[status];

  return (
    <li
      className={`flex items-center gap-3 rounded-lg border px-3 py-2.5 ${
        isError ? "border-[#f3c7c3] bg-[#fef3f2]" : "border-[#e6ebe8] bg-white"
      }`}
    >
      <span
        className={`flex size-9 shrink-0 items-center justify-center rounded-md ${
          isError ? "bg-[#fde2df] text-[#b42318]" : "bg-[#f1f4f2] text-[#68766f]"
        }`}
      >
        {isImage ? <FiImage size={17} /> : <FiFileText size={17} />}
      </span>

      <div className="min-w-0 flex-1">
        <p className="font-inter-medium truncate text-sm text-[#25362f]">
          {entry.file.name}{" "}
          <span className="font-inter-regular text-xs text-[#98a29c]">
            {formatSize(entry.file.size)}
          </span>
        </p>
        <p
          className={`mt-0.5 flex items-center gap-1.5 text-xs ${
            isError ? "text-[#b42318]" : isGood ? "text-[#16834a]" : "text-[#68766f]"
          }`}
        >
          {isBusy && <FiLoader className="animate-spin" size={12} />}
          {isGood && <FiCheckCircle size={12} />}
          {isError && <FiAlertCircle size={12} />}
          {text}
        </p>
      </div>

      {canRemove && (
        <button
          aria-label={`Remove ${entry.file.name}`}
          className="flex size-7 shrink-0 items-center justify-center rounded-md text-[#98a29c] hover:bg-[#f1f4f2] hover:text-[#25362f]"
          onClick={onRemove}
          type="button"
        >
          <FiX size={15} />
        </button>
      )}
    </li>
  );
}