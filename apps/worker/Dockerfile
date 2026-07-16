FROM node:22-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends ffmpeg ca-certificates && rm -rf /var/lib/apt/lists/* && corepack enable
WORKDIR /app
COPY package.json pnpm-workspace.yaml ./
COPY apps/worker/package.json apps/worker/package.json
RUN pnpm install --filter @securestream/worker...
COPY apps/worker apps/worker
RUN pnpm --filter @securestream/worker build
CMD ["pnpm","--filter","@securestream/worker","start"]
