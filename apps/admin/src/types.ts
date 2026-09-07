export type Admin = { id: string; email: string; role: "owner" | "admin" | "support" };

export type Customer = {
  id: string;
  displayName: string;
  phone: string | null;
  whatsapp: string | null;
  notes: string | null;
  status: "active" | "suspended" | "archived";
  createdAt: string;
  updatedAt: string;
};

export type Host = {
  id: string;
  label: string;
  protocol: "xtream" | "m3u" | "stalker";
  baseUrl: string;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
};

export type ProviderAccount = {
  id: string;
  customerId: string;
  customerName: string;
  hostId: string;
  hostLabel: string;
  protocol: Host["protocol"];
  externalReference: string | null;
  expiresAt: string | null;
  maxConnections: number | null;
  isActive: boolean;
  hasUsername: boolean;
  hasPassword: boolean;
  hasPlaylistUrl: boolean;
  hasPortalData: boolean;
  createdAt: string;
  updatedAt: string;
};

export type Activation = {
  id: string;
  customerId: string;
  customerName: string;
  providerAccountId: string;
  codeSuffix: string;
  status: "unused" | "active" | "revoked" | "expired";
  deviceLimit: number;
  deviceCount: number;
  startsAt: string | null;
  expiresAt: string | null;
  activatedAt: string | null;
  createdAt: string;
};

export type Device = {
  id: string;
  activationCodeId: string;
  codeSuffix: string;
  customerName: string;
  platform: string;
  model: string | null;
  appVersion: string;
  status: "active" | "blocked";
  firstSeenAt: string;
  lastSeenAt: string;
};

export type AuditLog = {
  id: string;
  action: string;
  entityType: string;
  entityId: string | null;
  metadata: Record<string, unknown>;
  ipAddress: string | null;
  createdAt: string;
  adminEmail: string | null;
};
