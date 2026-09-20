import pg from "pg";

export type Database = pg.Pool;

export function createDatabase(databaseUrl: string, statementTimeoutMs: number): Database {
  return new pg.Pool({
    connectionString: databaseUrl,
    max: 8,
    idleTimeoutMillis: 30_000,
    connectionTimeoutMillis: 5_000,
    statement_timeout: statementTimeoutMs
  });
}
