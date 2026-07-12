-- CreateSchema
CREATE SCHEMA IF NOT EXISTS "public";

-- CreateEnum
CREATE TYPE "Role" AS ENUM ('SUPER_ADMIN', 'ADMIN', 'EDITOR', 'VIEWER');

-- CreateEnum
CREATE TYPE "UserStatus" AS ENUM ('ACTIVE', 'SUSPENDED', 'DISABLED');

-- CreateEnum
CREATE TYPE "PublishStatus" AS ENUM ('DRAFT', 'PUBLISHED', 'UNPUBLISHED', 'ARCHIVED');

-- CreateEnum
CREATE TYPE "AssetState" AS ENUM ('UPLOADING', 'UPLOADED', 'QUEUED', 'PROCESSING', 'READY', 'FAILED', 'ARCHIVED');

-- CreateTable
CREATE TABLE "User" (
    "id" TEXT NOT NULL,
    "email" TEXT NOT NULL,
    "displayName" TEXT NOT NULL,
    "passwordHash" TEXT NOT NULL,
    "role" "Role" NOT NULL DEFAULT 'VIEWER',
    "status" "UserStatus" NOT NULL DEFAULT 'ACTIVE',
    "emailVerifiedAt" TIMESTAMP(3),
    "failedLoginCount" INTEGER NOT NULL DEFAULT 0,
    "lockedUntil" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "User_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "RefreshSession" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "deviceId" TEXT NOT NULL,
    "tokenHash" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "revokedAt" TIMESTAMP(3),
    "replacedById" TEXT,
    "tokenFamily" TEXT NOT NULL,
    "userAgent" TEXT,
    "ipHash" TEXT,
    "lastUsedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "RefreshSession_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "PasswordResetToken" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "tokenHash" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "usedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "PasswordResetToken_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "EmailVerificationToken" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "tokenHash" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "usedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "EmailVerificationToken_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Movie" (
    "id" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "synopsis" TEXT NOT NULL,
    "slug" TEXT NOT NULL,
    "status" "PublishStatus" NOT NULL DEFAULT 'DRAFT',
    "featured" BOOLEAN NOT NULL DEFAULT false,
    "releaseDate" TIMESTAMP(3),
    "maturityRating" TEXT,
    "availabilityStart" TIMESTAMP(3),
    "availabilityEnd" TIMESTAMP(3),
    "version" INTEGER NOT NULL DEFAULT 1,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Movie_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Series" (
    "id" TEXT NOT NULL,
    "title" TEXT NOT NULL,
    "synopsis" TEXT NOT NULL,
    "slug" TEXT NOT NULL,
    "status" "PublishStatus" NOT NULL DEFAULT 'DRAFT',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Series_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Season" (
    "id" TEXT NOT NULL,
    "seriesId" TEXT NOT NULL,
    "number" INTEGER NOT NULL,
    "title" TEXT,

    CONSTRAINT "Season_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Episode" (
    "id" TEXT NOT NULL,
    "seasonId" TEXT NOT NULL,
    "number" INTEGER NOT NULL,
    "title" TEXT NOT NULL,
    "synopsis" TEXT NOT NULL,
    "status" "PublishStatus" NOT NULL DEFAULT 'DRAFT',

    CONSTRAINT "Episode_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "VideoAsset" (
    "id" TEXT NOT NULL,
    "movieId" TEXT,
    "episodeId" TEXT,
    "state" "AssetState" NOT NULL DEFAULT 'UPLOADING',
    "durationSeconds" INTEGER,
    "sourceStorageKey" TEXT NOT NULL,
    "manifestStorageKey" TEXT,
    "drmScheme" TEXT,
    "failureReason" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "VideoAsset_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "VideoRendition" (
    "id" TEXT NOT NULL,
    "videoAssetId" TEXT NOT NULL,
    "height" INTEGER NOT NULL,
    "bitrateKbps" INTEGER NOT NULL,
    "codec" TEXT NOT NULL,
    "storageKey" TEXT NOT NULL,

    CONSTRAINT "VideoRendition_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Collection" (
    "id" TEXT NOT NULL,
    "name" TEXT NOT NULL,
    "slug" TEXT NOT NULL,
    "published" BOOLEAN NOT NULL DEFAULT false,
    "sortOrder" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "Collection_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "CollectionItem" (
    "id" TEXT NOT NULL,
    "collectionId" TEXT NOT NULL,
    "movieId" TEXT,
    "seriesId" TEXT,
    "sortOrder" INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT "CollectionItem_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "PlaybackSession" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "videoAssetId" TEXT NOT NULL,
    "deviceId" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "revokedAt" TIMESTAMP(3),
    "riskSignals" TEXT[],
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "PlaybackSession_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "WatchProgress" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "videoAssetId" TEXT NOT NULL,
    "positionSeconds" INTEGER NOT NULL,
    "completed" BOOLEAN NOT NULL DEFAULT false,
    "version" INTEGER NOT NULL DEFAULT 1,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "WatchProgress_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "WatchHistory" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "videoAssetId" TEXT NOT NULL,
    "watchedAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "removedAt" TIMESTAMP(3),

    CONSTRAINT "WatchHistory_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "MyListItem" (
    "id" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "catalogId" TEXT NOT NULL,
    "kind" TEXT NOT NULL,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "MyListItem_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "Upload" (
    "id" TEXT NOT NULL,
    "videoAssetId" TEXT NOT NULL,
    "storageKey" TEXT NOT NULL,
    "uploadId" TEXT,
    "bytesExpected" BIGINT NOT NULL,
    "bytesReceived" BIGINT NOT NULL DEFAULT 0,
    "status" TEXT NOT NULL DEFAULT 'CREATED',
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "Upload_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "AdminAuditLog" (
    "id" TEXT NOT NULL,
    "actorUserId" TEXT NOT NULL,
    "action" TEXT NOT NULL,
    "targetType" TEXT NOT NULL,
    "targetId" TEXT,
    "requestId" TEXT NOT NULL,
    "ipHash" TEXT,
    "metadata" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "AdminAuditLog_pkey" PRIMARY KEY ("id")
);

-- CreateTable
CREATE TABLE "PlatformSetting" (
    "key" TEXT NOT NULL,
    "value" JSONB NOT NULL,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "PlatformSetting_pkey" PRIMARY KEY ("key")
);

-- CreateTable
CREATE TABLE "SecurityEvent" (
    "id" TEXT NOT NULL,
    "userId" TEXT,
    "kind" TEXT NOT NULL,
    "severity" TEXT NOT NULL,
    "metadata" JSONB,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "SecurityEvent_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "User_email_key" ON "User"("email");

-- CreateIndex
CREATE UNIQUE INDEX "RefreshSession_tokenHash_key" ON "RefreshSession"("tokenHash");

-- CreateIndex
CREATE INDEX "RefreshSession_userId_expiresAt_idx" ON "RefreshSession"("userId", "expiresAt");

-- CreateIndex
CREATE INDEX "RefreshSession_tokenFamily_idx" ON "RefreshSession"("tokenFamily");

-- CreateIndex
CREATE UNIQUE INDEX "PasswordResetToken_tokenHash_key" ON "PasswordResetToken"("tokenHash");

-- CreateIndex
CREATE INDEX "PasswordResetToken_userId_expiresAt_idx" ON "PasswordResetToken"("userId", "expiresAt");

-- CreateIndex
CREATE UNIQUE INDEX "EmailVerificationToken_tokenHash_key" ON "EmailVerificationToken"("tokenHash");

-- CreateIndex
CREATE INDEX "EmailVerificationToken_userId_expiresAt_idx" ON "EmailVerificationToken"("userId", "expiresAt");

-- CreateIndex
CREATE UNIQUE INDEX "Movie_slug_key" ON "Movie"("slug");

-- CreateIndex
CREATE UNIQUE INDEX "Series_slug_key" ON "Series"("slug");

-- CreateIndex
CREATE UNIQUE INDEX "Season_seriesId_number_key" ON "Season"("seriesId", "number");

-- CreateIndex
CREATE UNIQUE INDEX "Episode_seasonId_number_key" ON "Episode"("seasonId", "number");

-- CreateIndex
CREATE INDEX "VideoAsset_state_idx" ON "VideoAsset"("state");

-- CreateIndex
CREATE UNIQUE INDEX "VideoRendition_videoAssetId_height_key" ON "VideoRendition"("videoAssetId", "height");

-- CreateIndex
CREATE UNIQUE INDEX "Collection_slug_key" ON "Collection"("slug");

-- CreateIndex
CREATE UNIQUE INDEX "CollectionItem_collectionId_sortOrder_key" ON "CollectionItem"("collectionId", "sortOrder");

-- CreateIndex
CREATE INDEX "PlaybackSession_userId_expiresAt_idx" ON "PlaybackSession"("userId", "expiresAt");

-- CreateIndex
CREATE UNIQUE INDEX "WatchProgress_userId_videoAssetId_key" ON "WatchProgress"("userId", "videoAssetId");

-- CreateIndex
CREATE INDEX "WatchHistory_userId_watchedAt_idx" ON "WatchHistory"("userId", "watchedAt");

-- CreateIndex
CREATE UNIQUE INDEX "MyListItem_userId_catalogId_key" ON "MyListItem"("userId", "catalogId");

-- CreateIndex
CREATE INDEX "AdminAuditLog_actorUserId_createdAt_idx" ON "AdminAuditLog"("actorUserId", "createdAt");

-- CreateIndex
CREATE INDEX "SecurityEvent_kind_createdAt_idx" ON "SecurityEvent"("kind", "createdAt");

-- AddForeignKey
ALTER TABLE "RefreshSession" ADD CONSTRAINT "RefreshSession_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "PasswordResetToken" ADD CONSTRAINT "PasswordResetToken_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "EmailVerificationToken" ADD CONSTRAINT "EmailVerificationToken_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Season" ADD CONSTRAINT "Season_seriesId_fkey" FOREIGN KEY ("seriesId") REFERENCES "Series"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Episode" ADD CONSTRAINT "Episode_seasonId_fkey" FOREIGN KEY ("seasonId") REFERENCES "Season"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "VideoAsset" ADD CONSTRAINT "VideoAsset_movieId_fkey" FOREIGN KEY ("movieId") REFERENCES "Movie"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "VideoAsset" ADD CONSTRAINT "VideoAsset_episodeId_fkey" FOREIGN KEY ("episodeId") REFERENCES "Episode"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "VideoRendition" ADD CONSTRAINT "VideoRendition_videoAssetId_fkey" FOREIGN KEY ("videoAssetId") REFERENCES "VideoAsset"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CollectionItem" ADD CONSTRAINT "CollectionItem_collectionId_fkey" FOREIGN KEY ("collectionId") REFERENCES "Collection"("id") ON DELETE CASCADE ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CollectionItem" ADD CONSTRAINT "CollectionItem_movieId_fkey" FOREIGN KEY ("movieId") REFERENCES "Movie"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "CollectionItem" ADD CONSTRAINT "CollectionItem_seriesId_fkey" FOREIGN KEY ("seriesId") REFERENCES "Series"("id") ON DELETE SET NULL ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "PlaybackSession" ADD CONSTRAINT "PlaybackSession_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "PlaybackSession" ADD CONSTRAINT "PlaybackSession_videoAssetId_fkey" FOREIGN KEY ("videoAssetId") REFERENCES "VideoAsset"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WatchProgress" ADD CONSTRAINT "WatchProgress_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WatchProgress" ADD CONSTRAINT "WatchProgress_videoAssetId_fkey" FOREIGN KEY ("videoAssetId") REFERENCES "VideoAsset"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WatchHistory" ADD CONSTRAINT "WatchHistory_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "WatchHistory" ADD CONSTRAINT "WatchHistory_videoAssetId_fkey" FOREIGN KEY ("videoAssetId") REFERENCES "VideoAsset"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "MyListItem" ADD CONSTRAINT "MyListItem_userId_fkey" FOREIGN KEY ("userId") REFERENCES "User"("id") ON DELETE RESTRICT ON UPDATE CASCADE;

-- AddForeignKey
ALTER TABLE "Upload" ADD CONSTRAINT "Upload_videoAssetId_fkey" FOREIGN KEY ("videoAssetId") REFERENCES "VideoAsset"("id") ON DELETE RESTRICT ON UPDATE CASCADE;
