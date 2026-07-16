import type { Role } from "@prisma/client";
export const permissions = {
  SUPER_ADMIN: new Set(["admin:manage", "users:manage", "catalog:write", "settings:security", "audit:read"]),
  ADMIN: new Set(["users:manage", "catalog:write", "audit:read"]),
  EDITOR: new Set(["catalog:write"]),
  VIEWER: new Set<string>(),
} satisfies Record<Role, Set<string>>;
export function hasPermission(role: Role, permission: string) {
  return permissions[role].has(permission);
}
