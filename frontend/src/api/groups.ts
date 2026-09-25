import { api } from "./client";
import type {
  AddMemberPayload,
  CreateGroupPayload,
  GroupDetail,
  GroupMember,
  GroupSummary,
} from "../types/group";

export async function createGroupRequest(payload: CreateGroupPayload): Promise<GroupDetail> {
  const { data } = await api.post<GroupDetail>("/groups", payload);
  return data;
}

export async function getMyGroupsRequest(): Promise<GroupSummary[]> {
  const { data } = await api.get<GroupSummary[]>("/groups");
  return data;
}

export async function getGroupRequest(groupId: number | string): Promise<GroupDetail> {
  const { data } = await api.get<GroupDetail>(`/groups/${groupId}`);
  return data;
}

export async function addGroupMemberRequest(
  groupId: number | string,
  payload: AddMemberPayload
): Promise<GroupMember> {
  const { data } = await api.post<GroupMember>(`/groups/${groupId}/members`, payload);
  return data;
}

export async function getGroupMembersRequest(groupId: number | string): Promise<GroupMember[]> {
  const { data } = await api.get<GroupMember[]>(`/groups/${groupId}/members`);
  return data;
}
