import type { HTMLInputTypeAttribute } from "react";

type BaseProps = {
  // Used for the input's id (with useId) and name; also how forms focus a field
  id: string;
  name: string;
  label: string;
  error?: string;
  optional?: boolean;
  // e.g. "sm:col-span-2" in a grid
  className?: string;
};

const controlClass = (error?: string) =>
  `font-inter-regular h-9 rounded-[7px] border bg-white px-3 text-sm outline-none placeholder:text-[#b0bab5] focus:border-[#16834a] focus:ring-2 focus:ring-[#16834a]/20 disabled:bg-[#f8faf9] ${
    error ? "border-[#b42318]" : "border-[#e6ebe8]"
  }`;

function Label({ id, label, optional }: Pick<BaseProps, "id" | "label" | "optional">) {
  return (
    <label className="font-inter-medium text-[13px]" htmlFor={id}>
      {label}
      {optional && <span className="font-inter-regular text-[#98a29c]"> (optional)</span>}
    </label>
  );
}

function ErrorText({ id, error }: { id: string; error?: string }) {
  if (!error) return null;
  return (
    <p className="font-inter-regular text-xs text-[#b42318]" id={`${id}-error`}>
      {error}
    </p>
  );
}

type TextFieldProps = BaseProps & {
  value: string;
  onChange: (value: string) => void;
  placeholder?: string;
  maxLength?: number;
  autoComplete?: string;
  type?: HTMLInputTypeAttribute;
};

export function TextField({
  id,
  name,
  label,
  error,
  optional,
  className = "",
  value,
  onChange,
  placeholder,
  maxLength,
  autoComplete = "off",
  type = "text",
}: TextFieldProps) {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <Label id={id} label={label} optional={optional} />
      <input
        aria-describedby={error ? `${id}-error` : undefined}
        aria-invalid={error ? true : undefined}
        autoComplete={autoComplete}
        className={controlClass(error)}
        id={id}
        maxLength={maxLength}
        name={name}
        onChange={(event) => onChange(event.target.value)}
        placeholder={placeholder}
        required={!optional}
        type={type}
        value={value}
      />
      <ErrorText error={error} id={id} />
    </div>
  );
}

type SelectFieldProps<T extends string> = BaseProps & {
  value: T;
  onChange: (value: T) => void;
  options: { value: T; label: string }[];
};

export function SelectField<T extends string>({
  id,
  name,
  label,
  error,
  className = "",
  value,
  onChange,
  options,
}: SelectFieldProps<T>) {
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <Label id={id} label={label} />
      <select
        aria-describedby={error ? `${id}-error` : undefined}
        aria-invalid={error ? true : undefined}
        className={controlClass(error)}
        id={id}
        name={name}
        onChange={(event) => onChange(event.target.value as T)}
        value={value}
      >
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <ErrorText error={error} id={id} />
    </div>
  );
}
