import { buildApp } from "./app.js";
import { loadConfig } from "./config.js";
import { createDatabase } from "./db.js";
import { safeErrorSummary } from "./logging.js";

const config = loadConfig();
const pool = createDatabase(config.databaseUrl, config.databaseStatementTimeoutMs);
const app = await buildApp(config, pool);

const shutdown = async (signal: string) => {
  app.log.info({ signal }, "Shutting down");
  await app.close();
  await pool.end();
  process.exit(0);
};

process.on("SIGTERM", () => void shutdown("SIGTERM"));
process.on("SIGINT", () => void shutdown("SIGINT"));

try {
  await app.listen({ host: "0.0.0.0", port: config.port });
} catch (error) {
  app.log.error({ error: safeErrorSummary(error) }, "API startup failed");
  await pool.end();
  process.exit(1);
}
