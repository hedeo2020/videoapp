import type { Movie, Series, VideoAsset } from "@prisma/client";

export type CatalogMovie = Movie & { assets?: Pick<VideoAsset, "id" | "state" | "durationSeconds">[] };
export type CatalogSeries = Series;
export type CatalogCard = ReturnType<typeof toMovieCard> | ReturnType<typeof toSeriesCard>;

export function isCurrentlyAvailable(item: { availabilityStart?: Date | null; availabilityEnd?: Date | null }, now = new Date()) {
  return (!item.availabilityStart || item.availabilityStart <= now) && (!item.availabilityEnd || item.availabilityEnd > now);
}

export function movieHasReadyAsset(movie: CatalogMovie) {
  return movie.assets?.some((asset) => asset.state === "READY") ?? false;
}

export function toMovieCard(movie: CatalogMovie) {
  return {
    id: movie.id,
    kind: "MOVIE" as const,
    title: movie.title,
    slug: movie.slug,
    synopsis: movie.synopsis,
    maturityRating: movie.maturityRating,
    artworkUrl: movie.posterUrl,
    durationSeconds: movie.assets?.find((asset) => asset.state === "READY")?.durationSeconds,
  };
}

export function toSeriesCard(series: CatalogSeries) {
  return {
    id: series.id,
    kind: "SERIES" as const,
    title: series.title,
    slug: series.slug,
    synopsis: series.synopsis,
    artworkUrl: series.posterUrl,
    maturityRating: null,
    durationSeconds: null,
  };
}
