import { useState, type FormEvent } from "react";
import Modal from "./Modal";
import FormField from "./FormField";
import { createGroupRequest } from "../api/groups";
import { extractErrorMessage } from "../api/auth";
import type { GroupDetail } from "../types/group";

interface CreateGroupModalProps {
  onClose: () => void;
  onCreated: (group: GroupDetail) => void;
}

export default function CreateGroupModal({ onClose, onCreated }: CreateGroupModalProps) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!name.trim()) {
      setError("Group name is required");
      return;
    }

    setIsSubmitting(true);
    try {
      const group = await createGroupRequest({
        name: name.trim(),
        description: description.trim() || undefined,
      });
      onCreated(group);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal title="Create a group" onClose={onClose}>
      {error && (
        <div className="mb-4 rounded-lg bg-rose-50 border border-rose-200 px-3.5 py-2.5 text-[13px] text-rose-700">
          {error}
        </div>
      )}
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <FormField
          id="group-name"
          label="Group name"
          placeholder="Goa Trip"
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={120}
        />
        <div>
          <label htmlFor="group-description" className="block text-[13px] font-medium text-ink/80 mb-1.5">
            Description <span className="text-muted font-normal">(optional)</span>
          </label>
          <textarea
            id="group-description"
            rows={3}
            maxLength={500}
            placeholder="Weekend trip with the gang"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full rounded-lg border border-line px-3.5 py-2.5 text-[15px] text-ink placeholder:text-muted/70 outline-none transition-colors focus:ring-2 focus:ring-mint-500/30 focus:border-mint-500 resize-none"
          />
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-full bg-mint-600 text-white text-[15px] font-medium py-2.5 hover:bg-mint-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Creating…" : "Create group"}
        </button>
      </form>
    </Modal>
  );
}
