FROM node:22-alpine AS build
RUN apk add --no-cache ffmpeg
RUN corepack enable
WORKDIR /app
COPY package.json pnpm-workspace.yaml ./
COPY apps/api/package.json apps/api/package.json
RUN pnpm install --filter @securestream/api... --frozen-lockfile=false
COPY apps/api apps/api
RUN pnpm --filter @securestream/api build
FROM node:22-alpine
RUN apk add --no-cache ffmpeg
RUN corepack enable
WORKDIR /app
COPY --from=build /app /app
RUN mkdir -p /data/media && chown -R node:node /data
USER node
EXPOSE 4000
CMD ["sh","-c","pnpm --filter @securestream/api prisma:migrate && pnpm --filter @securestream/api start"]
