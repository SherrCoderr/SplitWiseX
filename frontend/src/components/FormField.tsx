import type { InputHTMLAttributes } from "react";

interface FormFieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
}

export default function FormField({ label, error, id, ...inputProps }: FormFieldProps) {
  return (
    <div>
      <label htmlFor={id} className="block text-[13px] font-medium text-ink/80 mb-1.5">
        {label}
      </label>
      <input
        id={id}
        {...inputProps}
        className={`w-full rounded-lg border px-3.5 py-2.5 text-[15px] text-ink placeholder:text-muted/70 outline-none transition-colors focus:ring-2 focus:ring-mint-500/30 ${
          error ? "border-rose-400 focus:border-rose-400" : "border-line focus:border-mint-500"
        }`}
      />
      {error && <p className="mt-1.5 text-[13px] text-rose-600">{error}</p>}
    </div>
  );
}
