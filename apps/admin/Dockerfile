FROM node:22-alpine AS build
RUN corepack enable
WORKDIR /app
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
COPY package.json pnpm-workspace.yaml ./
COPY apps/admin/package.json apps/admin/package.json
RUN pnpm install --filter @securestream/admin... --frozen-lockfile=false
COPY apps/admin apps/admin
RUN pnpm --filter @securestream/admin build

FROM node:22-alpine
RUN corepack enable
WORKDIR /app
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
COPY --from=build /app /app
EXPOSE 3000
CMD ["pnpm","--filter","@securestream/admin","start","--","-H","0.0.0.0","-p","3000"]
