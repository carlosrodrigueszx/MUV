import type { TripCriteria } from "./criteria";

const drafts = new Map<number, TripCriteria>();
const storageKey = (accountId: number) => `muv.trip-draft.v1.${accountId}`;

export function readTripDraft(accountId: number): TripCriteria | undefined {
  try {
    const cached = drafts.get(accountId);
    const value = cached ? null : sessionStorage.getItem(storageKey(accountId));
    return value ? JSON.parse(value) as TripCriteria : cached;
  } catch {
    return drafts.get(accountId);
  }
}

export function saveTripDraft(accountId: number, trip: TripCriteria): void {
  drafts.set(accountId, trip);
  try {
    sessionStorage.setItem(storageKey(accountId), JSON.stringify(trip));
  } catch {
    // The in-memory draft remains available when the browser blocks storage.
  }
}
