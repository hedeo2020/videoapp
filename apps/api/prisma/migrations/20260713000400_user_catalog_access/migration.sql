-- AlterTable
ALTER TABLE "User" ADD COLUMN "accessRestricted" BOOLEAN NOT NULL DEFAULT false;

-- CreateTable
CREATE TABLE "UserMovieAccess" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "movieId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "UserMovieAccess_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "UserCollectionAccess" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "collectionId" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "UserCollectionAccess_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "UserMovieAccess_userId_movieId_key" ON "UserMovieAccess"("userId", "movieId");

-- CreateIndex
CREATE INDEX "UserMovieAccess_movieId_idx" ON "UserMovieAccess"("movieId");

-- CreateIndex
CREATE UNIQUE INDEX "UserCollectionAccess_userId_collectionId_key" ON "UserCollectionAccess"("userId", "collectionId");

-- CreateIndex
CREATE INDEX "UserCollectionAccess_collectionId_idx" ON "UserCollectionAccess"("collectionId");

-- AddForeignKey
ALTER TABLE "UserMovieAccess" ADD CONSTRAINT "UserMovieAccess_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "UserMovieAccess" ADD CONSTRAINT "UserMovieAccess_movieId_fkey" FOREIGN KEY ("movieId") REFERENCES "Movie"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "UserCollectionAccess" ADD CONSTRAINT "UserCollectionAccess_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "UserCollectionAccess" ADD CONSTRAINT "UserCollectionAccess_collectionId_fkey" FOREIGN KEY ("collectionId") REFERENCES "Collection"("id") ON DELETE CASCADE ON UPDATE CASCADE;
