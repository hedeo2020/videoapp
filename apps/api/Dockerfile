FROM node:22-alpine AS build
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-workspace.yaml ./
COPY apps/api/package.json apps/api/package.json
RUN pnpm install --filter @securestream/api... --frozen-lockfile=false
COPY apps/api apps/api
RUN pnpm --filter @securestream/api build
FROM node:22-alpine
RUN corepack enable
WORKDIR /app
COPY --from=build /app /app
USER node
EXPOSE 4000
CMD ["pnpm","--filter","@securestream/api","start"]
