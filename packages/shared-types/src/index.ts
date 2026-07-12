export const roles = ["SUPER_ADMIN", "ADMIN", "EDITOR", "VIEWER"] as const;
export type Role = (typeof roles)[number];
export type ApiError = { error: { code: string; message: string; requestId: string; details?: unknown } };
export type PlaybackGrant = { sessionId: string; manifestUrl: string; licenseUrl?: string; headers: Record<string,string>; watermark?: { text:string; opacity:number; moveEverySeconds:number }; expiresAt:string };
export type CatalogCard = { id:string; kind:"MOVIE"|"SERIES"; title:string; synopsis:string; artworkUrl?:string; maturityRating?:string };
