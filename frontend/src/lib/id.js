/**
 * Generates a unique 9-digit numeric identifier, mirroring the backend's
 * UniqueIdGenerator. Used in place of UUIDs for client-generated ids
 * (e.g. idempotency keys, account ids) so the format is consistent
 * across the whole application.
 */
const MIN_VALUE = 100_000_000;
const MAX_VALUE = 999_999_999;

export function generateUniqueId() {
  const value = Math.floor(MIN_VALUE + Math.random() * (MAX_VALUE - MIN_VALUE + 1));
  return String(value);
}
