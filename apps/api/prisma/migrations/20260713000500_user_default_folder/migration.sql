ALTER TABLE "User" ADD COLUMN "defaultCollectionId" TEXT;

ALTER TABLE "User"
  ADD CONSTRAINT "User_defaultCollectionId_fkey"
  FOREIGN KEY ("defaultCollectionId") REFERENCES "Collection"("id")
  ON DELETE SET NULL ON UPDATE CASCADE;

CREATE INDEX "User_defaultCollectionId_idx" ON "User"("defaultCollectionId");
