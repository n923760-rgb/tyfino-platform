import pg from "pg";

export type Database = pg.Pool;

type DatabaseRoleBoundary = {
  superuser: boolean;
  createDatabase: boolean;
  createRole: boolean;
  replication: boolean;
  bypassRowSecurity: boolean;
  databaseCreate: boolean;
  schemaCreate: boolean;
  ownsDatabase: boolean;
  ownsPublicSchema: boolean;
  ownsPublicObjects: boolean;
  hasRoleMembership: boolean;
};

export class UnsafeDatabaseRoleError extends Error {
  readonly code = "UNSAFE_DATABASE_ROLE";

  constructor() {
    super("Database role violates the restricted runtime boundary");
    this.name = "UnsafeDatabaseRoleError";
  }
}

export async function assertRestrictedDatabaseRole(db: Pick<Database, "query">): Promise<void> {
  const result = await db.query<DatabaseRoleBoundary>(
    `SELECT role.rolsuper AS superuser,
            role.rolcreatedb AS "createDatabase",
            role.rolcreaterole AS "createRole",
            role.rolreplication AS replication,
            role.rolbypassrls AS "bypassRowSecurity",
            has_database_privilege(current_user, current_database(), 'CREATE') AS "databaseCreate",
            has_schema_privilege(current_user, 'public', 'CREATE') AS "schemaCreate",
            database.datdba = role.oid AS "ownsDatabase",
            namespace.nspowner = role.oid AS "ownsPublicSchema",
            EXISTS (
              SELECT 1 FROM pg_class object
               WHERE object.relnamespace = namespace.oid AND object.relowner = role.oid
            ) AS "ownsPublicObjects",
            EXISTS (
              SELECT 1 FROM pg_auth_members membership WHERE membership.member = role.oid
            ) AS "hasRoleMembership"
       FROM pg_roles role
       JOIN pg_database database ON database.datname = current_database()
       JOIN pg_namespace namespace ON namespace.nspname = 'public'
      WHERE role.rolname = current_user`
  );
  const boundary = result.rows[0];
  if (!boundary || Object.values(boundary).some((value) => value)) {
    throw new UnsafeDatabaseRoleError();
  }
}

export function createDatabase(databaseUrl: string, statementTimeoutMs: number): Database {
  return new pg.Pool({
    connectionString: databaseUrl,
    max: 8,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 5_000,
    statement_timeout: statementTimeoutMs
  });
}
