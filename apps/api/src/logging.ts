const sensitiveQueryParams = new Set(["pt", "dt", "token"]);

export const redactSensitiveQueryParams = (input: string | undefined) => {
  if (!input) return input;
  try {
    const url = new URL(input, "http://securestream.local");
    for (const key of sensitiveQueryParams) {
      if (url.searchParams.has(key)) url.searchParams.set(key, "[redacted]");
    }
    return `${url.pathname}${url.search}${url.hash}`.replace(/%5Bredacted%5D/gi, "[redacted]");
  } catch {
    return input.replace(/([?&](?:pt|dt|token)=)[^&]*/gi, "$1[redacted]");
  }
};

type RequestLogShape = {
  method?: string;
  url?: string;
  hostname?: string;
  headers?: { host?: string };
  remoteAddress?: string;
  remotePort?: number;
};

export const requestLogSerializer = (req: RequestLogShape) => ({
  method: req.method,
  url: redactSensitiveQueryParams(req.url),
  host: req.hostname ?? req.headers?.host,
  remoteAddress: req.remoteAddress,
  remotePort: req.remotePort,
});
