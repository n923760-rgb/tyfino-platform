export type Admin = { id: string; email: string; role: "owner" | "admin" | "support" };

export type Activation = {
  id: string;
  codeSuffix: string;
  licenseKind: "one_year" | "lifetime";
  status: "unused" | "active" | "revoked";
  preActivationExpiresAt: string | null;
  activatedAt: string | null;
  grantStartsAt: string | null;
  grantExpiresAt: string | null;
  deviceBound: boolean;
  customerName: string | null;
  phoneNumber: string | null;
  externalReference: string | null;
  adminLabel: string | null;
  internalNote: string | null;
  createdAt: string;
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
