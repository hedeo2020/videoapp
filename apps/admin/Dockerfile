FROM node:22-alpine AS build
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-workspace.yaml ./
COPY apps/admin/package.json apps/admin/package.json
RUN pnpm install --filter @securestream/admin... --frozen-lockfile=false
COPY apps/admin apps/admin
RUN pnpm --filter @securestream/admin build
EXPOSE 3000
CMD ["pnpm","--filter","@securestream/admin","start"]
