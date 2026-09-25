/**
 * Matches the backend's GroupEventMessage (dto/event). This is only ever
 * used as a "something changed, go refetch" signal — never as the source
 * of truth for balances or anything else. See useGroupChannel.
 */
export type GroupEventType = "EXPENSE_CREATED" | "EXPENSE_DELETED" | "MEMBER_ADDED";

export interface GroupEvent {
  type: GroupEventType;
  groupId: number;
  entityId?: number;
  timestamp?: string;
}
