import type { UserSummary } from "./auth";

/**
 * Lightweight group shape for list views (GET /api/groups) — matches the
 * backend's GroupSummaryDto.
 */
export interface GroupSummary {
  id: number;
  name: string;
  description: string | null;
  memberCount: number;
  expenseCount: number;
  createdAt: string;
}

export interface GroupMember {
  userId: number;
  name: string;
  email: string;
  joinedAt: string;
}

/**
 * Full group shape for GET /api/groups/{id} — matches the backend's
 * GroupDetailDto.
 */
export interface GroupDetail {
  id: number;
  name: string;
  description: string | null;
  createdBy: UserSummary;
  createdAt: string;
  members: GroupMember[];
}

export interface CreateGroupPayload {
  name: string;
  description?: string;
}

export interface AddMemberPayload {
  email: string;
}
