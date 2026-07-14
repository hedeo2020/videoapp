#!/bin/sh
set -eu

pnpm --filter @securestream/api prisma:migrate

pnpm --filter @securestream/worker start &

exec pnpm --filter @securestream/api start
