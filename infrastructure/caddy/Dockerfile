FROM node:22-alpine AS admin-build
WORKDIR /build
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
COPY apps/admin/package.json apps/admin/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY apps/admin/tsconfig.json apps/admin/tsconfig.app.json apps/admin/tsconfig.node.json apps/admin/vite.config.ts apps/admin/index.html ./
COPY apps/admin/src ./src
RUN npm run build

FROM caddy:2-alpine
COPY infrastructure/caddy/Caddyfile /etc/caddy/Caddyfile
COPY sites/public /srv/public
COPY --from=admin-build /build/dist /srv/admin
