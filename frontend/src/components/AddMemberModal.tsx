import { useState, type FormEvent } from "react";
import Modal from "./Modal";
import FormField from "./FormField";
import { addGroupMemberRequest } from "../api/groups";
import { extractErrorMessage } from "../api/auth";
import type { GroupMember } from "../types/group";

interface AddMemberModalProps {
  groupId: number;
  onClose: () => void;
  onAdded: (member: GroupMember) => void;
}

export default function AddMemberModal({ groupId, onClose, onAdded }: AddMemberModalProps) {
  const [email, setEmail] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);

    if (!email.trim()) {
      setError("Email is required");
      return;
    }

    setIsSubmitting(true);
    try {
      const member = await addGroupMemberRequest(groupId, { email: email.trim() });
      onAdded(member);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <Modal title="Add a member" onClose={onClose}>
      {error && (
        <div className="mb-4 rounded-lg bg-rose-50 border border-rose-200 px-3.5 py-2.5 text-[13px] text-rose-700">
          {error}
        </div>
      )}
      <form onSubmit={handleSubmit} noValidate className="space-y-4">
        <FormField
          id="member-email"
          label="Email address"
          type="email"
          placeholder="rahul@example.com"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <p className="text-[13px] text-muted -mt-2">
          They must already have a SplitWiseX account.
        </p>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-full bg-mint-600 text-white text-[15px] font-medium py-2.5 hover:bg-mint-700 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        >
          {isSubmitting ? "Adding…" : "Add member"}
        </button>
      </form>
    </Modal>
  );
}
