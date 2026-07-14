FROM node:22-alpine AS build
RUN apk add --no-cache ffmpeg
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-workspace.yaml ./
COPY apps/api/package.json apps/api/package.json
COPY apps/worker/package.json apps/worker/package.json
RUN pnpm install --filter @securestream/api... --filter @securestream/worker... --frozen-lockfile=false
COPY apps/api apps/api
COPY apps/worker apps/worker
RUN pnpm --filter @securestream/api build
RUN pnpm --filter @securestream/worker build

FROM node:22-alpine
RUN apk add --no-cache ffmpeg
RUN corepack enable
WORKDIR /app
COPY --from=build /app /app
COPY apps/api/start-coolify.sh /app/apps/api/start-coolify.sh
RUN chmod +x /app/apps/api/start-coolify.sh && mkdir -p /data/media /data/tmp && chown -R node:node /data
USER node
EXPOSE 4000
CMD ["/app/apps/api/start-coolify.sh"]
