ALTER TABLE "User" ADD COLUMN "deletedAt" TIMESTAMP(3);
ALTER TABLE "User" ADD COLUMN "deletedReason" TEXT;
ALTER TABLE "Movie" ADD COLUMN "deletedAt" TIMESTAMP(3);
ALTER TABLE "Movie" ADD COLUMN "deletedReason" TEXT;

CREATE INDEX "User_deletedAt_idx" ON "User"("deletedAt");
CREATE INDEX "Movie_deletedAt_idx" ON "Movie"("deletedAt");
