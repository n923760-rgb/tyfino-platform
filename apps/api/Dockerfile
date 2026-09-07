FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY tsconfig.json ./
COPY src ./src
RUN npm run build && npm prune --omit=dev

FROM node:22-alpine AS runtime
ENV NODE_ENV=production
WORKDIR /app
RUN addgroup -S tyfino && adduser -S tyfino -G tyfino
COPY --from=build --chown=tyfino:tyfino /app/package.json ./
COPY --from=build --chown=tyfino:tyfino /app/node_modules ./node_modules
COPY --from=build --chown=tyfino:tyfino /app/dist ./dist
USER tyfino
EXPOSE 3000
CMD ["node", "dist/server.js"]
