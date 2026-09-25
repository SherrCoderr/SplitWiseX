import Modal from "./Modal";

interface ConfirmDialogProps {
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  /** True while the confirmed action is in flight — disables both buttons and swaps the confirm label. */
  isConfirming?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
}

/**
 * Stage 7: on-brand replacement for window.confirm(), used for destructive
 * actions like deleting an expense. Built on the shared Modal so it gets
 * the same Escape-to-close and focus behavior for free.
 */
export default function ConfirmDialog({
  title,
  description,
  confirmLabel = "Delete",
  cancelLabel = "Cancel",
  isConfirming = false,
  onCancel,
  onConfirm,
}: ConfirmDialogProps) {
  return (
    <Modal title={title} onClose={onCancel}>
      {description && <p className="text-[14px] text-muted mb-6">{description}</p>}
      <div className="flex items-center justify-end gap-3">
        <button
          type="button"
          onClick={onCancel}
          disabled={isConfirming}
          className="text-[14px] font-medium text-ink/80 border border-line rounded-full px-4 py-2 hover:bg-paper hover:border-ink/20 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        >
          {cancelLabel}
        </button>
        <button
          type="button"
          onClick={onConfirm}
          disabled={isConfirming}
          className="text-[14px] font-medium text-white bg-rose-600 rounded-full px-4 py-2 hover:bg-rose-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        >
          {isConfirming ? "Deleting…" : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
