import { useEffect, useRef, useState } from "react";
import { Client, type StompSubscription } from "@stomp/stompjs";
import { getToken } from "./authStorage";
import type { GroupEvent } from "../types/events";

export type GroupChannelStatus = "connecting" | "live" | "reconnecting" | "offline";

/**
 * Same-origin WebSocket URL for the STOMP endpoint. In dev this goes
 * through the Vite proxy's "/ws" entry (see vite.config.ts, ws: true) the
 * same way api/client.ts's relative "/api" baseURL goes through its proxy
 * entry, so nothing here needs to know the backend's actual host/port.
 */
function brokerUrl(): string {
  const apiUrl = import.meta.env.VITE_API_URL;

  if (apiUrl) {
    const protocol = apiUrl.startsWith("https") ? "wss" : "ws";
    return `${protocol}://${apiUrl.replace(/^https?:\/\//, "")}/ws`;
  }

  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}/ws`;
}

/**
 * Subscribes to /topic/groups/{groupId} for as long as the component using
 * it is mounted and groupId is set, and calls onEvent for each message.
 *
 * REST stays the source of truth — this hook only delivers the raw
 * GroupEvent; callers decide what to refetch. Handles connect/reconnect/
 * unmount/group-id-change cleanly: at most one connection and one
 * subscription exist at a time, and both are torn down on unmount or when
 * groupId changes.
 *
 * If the socket never connects (WebSocket unsupported, auth rejected,
 * network down), REST-driven pages keep working as normal — this hook
 * only ever reports a status, it never throws into the caller.
 */
export function useGroupChannel(
  groupId: string | number | undefined,
  onEvent: (event: GroupEvent) => void
): GroupChannelStatus {
  const [status, setStatus] = useState<GroupChannelStatus>("connecting");
  const onEventRef = useRef(onEvent);
  onEventRef.current = onEvent;

  useEffect(() => {
    if (!groupId) return;

    const token = getToken();
    if (!token) {
      setStatus("offline");
      return;
    }

    setStatus("connecting");
    let subscription: StompSubscription | null = null;

    const client = new Client({
      brokerURL: brokerUrl(),
      connectHeaders: { Authorization: `Bearer ${token}` },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        setStatus("live");
        subscription = client.subscribe(`/topic/groups/${groupId}`, (message) => {
          try {
            const event = JSON.parse(message.body) as GroupEvent;
            onEventRef.current(event);
          } catch {
            // Malformed/unexpected payload - ignore rather than crash the page.
          }
        });
      },
      onWebSocketClose: () => setStatus("reconnecting"),
      onStompError: () => setStatus("reconnecting"),
      onWebSocketError: () => setStatus("reconnecting"),
    });

    client.activate();

    return () => {
      subscription?.unsubscribe();
      client.deactivate();
    };
  }, [groupId]);

  return status;
}
