import type { FastifyDynamicSwaggerOptions } from "@fastify/swagger";
import type { FastifySwaggerUiOptions } from "@fastify/swagger-ui";

const publicApiUrl = process.env.API_PUBLIC_URL ?? "http://localhost:4000";

export const openApiOptions: FastifyDynamicSwaggerOptions = {
  openapi: {
    openapi: "3.0.3",
    info: {
      title: "SecureStream API",
      description:
        "SecureStream viewer, playback, media, and admin API. Authentication uses bearer access tokens for viewers and admin cookies/bearer tokens for the admin web UI.",
      version: "1.0.0",
    },
    servers: [
      {
        url: publicApiUrl,
        description: process.env.NODE_ENV === "production" ? "Production API" : "Configured API",
      },
      {
        url: "http://localhost:4000",
        description: "Local development",
      },
    ],
    tags: [
      { name: "health", description: "Liveness and dependency readiness checks" },
      { name: "auth", description: "Viewer and admin authentication" },
      { name: "viewer", description: "Viewer account, catalog, messages, notifications, and history" },
      { name: "playback", description: "Playback sessions, progress, and protected media delivery" },
      { name: "admin", description: "Admin dashboard and catalog management endpoints" },
      { name: "media", description: "Protected media, preview, and image endpoints" },
    ],
    components: {
      securitySchemes: {
        bearerAuth: {
          type: "http",
          scheme: "bearer",
          bearerFormat: "JWT",
          description: "Viewer/admin access token returned by login or refresh endpoints.",
        },
        csrfToken: {
          type: "apiKey",
          in: "header",
          name: "x-csrf-token",
          description: "Required for admin/browser state-changing requests.",
        },
        adminCookie: {
          type: "apiKey",
          in: "cookie",
          name: process.env.ADMIN_COOKIE_NAME ?? "ss_admin",
          description: "Admin httpOnly session cookie.",
        },
      },
    },
  },
  hideUntagged: false,
};

export const swaggerUiOptions: FastifySwaggerUiOptions = {
  routePrefix: "/documentation",
  staticCSP: true,
  uiConfig: {
    deepLinking: true,
    docExpansion: "list",
    persistAuthorization: false,
  },
};

type SecurityRequirement = { [securityLabel: string]: readonly string[] };

export const bearerSecurity: SecurityRequirement[] = [{ bearerAuth: [] }];
export const adminSecurity: SecurityRequirement[] = [{ bearerAuth: [] }, { adminCookie: [], csrfToken: [] }];
