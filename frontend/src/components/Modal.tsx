import { useEffect, useId, useRef, type ReactNode } from "react";

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
}

/**
 * Shared modal shell used by CreateGroupModal, AddMemberModal and
 * ConfirmDialog. Stage 7: Escape closes the modal, and focus moves into
 * the dialog on open so keyboard users aren't left focused on whatever
 * was behind it. Visual markup/behavior is otherwise unchanged.
 */
export default function Modal({ title, onClose, children }: ModalProps) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    // Focus the dialog itself by default. Individual forms still contain
    // their own first input, which the browser/user can tab into
    // immediately after — this just guarantees focus starts somewhere
    // sensible inside the modal rather than staying on the page behind it.
    dialogRef.current?.focus();

    function handleKeyDown(e: KeyboardEvent) {
      if (e.key === "Escape") {
        e.stopPropagation();
        onClose();
      }
    }

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center px-4">
      <button
        aria-label="Close"
        onClick={onClose}
        className="absolute inset-0 bg-ink/40 backdrop-blur-[2px]"
      />
      <div
        ref={dialogRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        tabIndex={-1}
        className="relative w-full max-w-md bg-white rounded-xl2 border border-line shadow-card p-6 outline-none"
      >
        <div className="flex items-center justify-between mb-5">
          <h2 id={titleId} className="font-display text-xl text-ink">
            {title}
          </h2>
          <button
            onClick={onClose}
            aria-label="Close"
            className="text-ink/50 hover:text-ink transition-colors text-xl leading-none"
          >
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
