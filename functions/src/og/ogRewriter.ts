import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

if (!admin.apps.length) {
  admin.initializeApp();
}

const db = admin.firestore();

// Read the built index.html once at cold start.
// This file is copied from dist/index.html via the build/predeploy step.
// Falls back to dist/index.html relative path for local emulator use.
let indexHtml = "";
const candidatePaths = [
  path.join(__dirname, "../../public/index.html"),   // deployed: functions/public/index.html
  path.join(__dirname, "../../../dist/index.html"),   // local emulator: project/dist/index.html
];
for (const candidate of candidatePaths) {
  try {
    indexHtml = fs.readFileSync(candidate, "utf-8");
    break;
  } catch {
    // try next
  }
}

const CRAWLER_UA =
  /twitterbot|facebookexternalhit|linkedinbot|discordbot|slackbot|googlebot|bingbot|whatsapp|telegrambot/i;

const RESERVED_ROUTES = new Set([
  "dashboard",
  "settings",
  "admin",
  "api",
  "auth",
  "login",
  "signup",
  "register",
  "profile",
  "discover",
  "favorites",
  "onboarding",
  "connect",
  "wallet",
  "about",
  "help",
  "support",
  "terms",
  "privacy",
  "__",
  "favicon.svg",
  "manifest.json",
  "og-default.svg",
  "assets",
  ".well-known",
]);

export const ogRewriter = functions.https.onRequest(async (req, res) => {
  const urlPath = req.path.replace(/\/+$/, "") || "/";
  const segments = urlPath.split("/").filter(Boolean);

  // Only handle single-segment paths: /{slug} (slug = username or page slug)
  if (segments.length === 0 || segments.length > 1) {
    serveApp(res);
    return;
  }

  const slug = segments[0].toLowerCase();

  // Let reserved routes and static-file-looking paths fall through to the SPA
  if (RESERVED_ROUTES.has(slug) || slug.startsWith("_") || slug.includes(".")) {
    serveApp(res);
    return;
  }

  // Only serve dynamic OG HTML for known crawlers; regular browsers get the SPA
  const userAgent = req.headers["user-agent"] || "";
  if (!CRAWLER_UA.test(userAgent)) {
    serveApp(res);
    return;
  }

  // --- Crawler path: resolve slug → profile, then build dynamic OG HTML ---
  try {
    let username = slug;
    let pageTitle = "";
    const canonicalUrl = `https://w3nto.app/${slug}`;

    // Try slug registry first: slug may point to a specific page
    const slugDoc = await db.collection("slugs").doc(slug).get();
    if (slugDoc.exists) {
      const slugData = slugDoc.data()!;
      username = slugData.username;
      const pageId = slugData.pageId as string;

      // Fetch page name for richer OG title
      const pageDoc = await db
        .collection("profiles")
        .doc(username)
        .collection("pages")
        .doc(pageId)
        .get();
      if (pageDoc.exists) {
        pageTitle = pageDoc.data()?.name || "";
      }
    }

    // Fetch the profile (using resolved username)
    const profileDoc = await db.collection("profiles").doc(username).get();

    if (!profileDoc.exists) {
      res.set("Cache-Control", "public, max-age=60");
      res.set("Content-Type", "text/html; charset=utf-8");
      res.send(buildDefaultHtml(urlPath));
      return;
    }

    const profile = profileDoc.data()!;
    const displayName = profile.displayName || username;
    const bio = profile.bio || "Web3 profile on W3nto";
    const avatar = profile.avatar || "https://w3nto.app/og-default.svg";

    const title = pageTitle
      ? `${pageTitle} — ${displayName} on W3nto`
      : `${displayName} on W3nto`;
    const description =
      bio.length > 160 ? bio.substring(0, 157) + "..." : bio;

    const html = buildDynamicHtml(indexHtml, title, description, avatar, canonicalUrl);

    res.set("Cache-Control", "public, max-age=300, s-maxage=600");
    res.set("Content-Type", "text/html; charset=utf-8");
    res.send(html);
  } catch {
    res.set("Cache-Control", "public, max-age=60");
    res.set("Content-Type", "text/html; charset=utf-8");
    res.send(buildDefaultHtml(urlPath));
  }
});

/**
 * Serve the SPA index.html for regular (non-crawler) browser requests.
 * If the built index.html isn't available in this function's context,
 * falls back to a minimal HTML shell that lets the browser load from root.
 */
function serveApp(res: functions.Response): void {
  if (indexHtml) {
    res.set("Cache-Control", "no-cache");
    res.set("Content-Type", "text/html; charset=utf-8");
    res.send(indexHtml);
  } else {
    // Last resort: minimal shell; the browser will load scripts from / via
    // Firebase Hosting CDN once it parses the empty HTML and retries assets.
    res.set("Cache-Control", "no-store");
    res.set("Content-Type", "text/html; charset=utf-8");
    res.send(buildMinimalShell());
  }
}

/**
 * Replace OG/Twitter meta tags in the real index.html with dynamic values.
 * If the base HTML is unavailable, falls back to a fully constructed page.
 */
function buildDynamicHtml(
  base: string,
  title: string,
  description: string,
  image: string,
  url: string
): string {
  if (!base) {
    return buildFullHtml(title, description, image, url);
  }

  const escaped = {
    title: escapeHtml(title),
    description: escapeHtml(description),
    image: escapeHtml(image),
    url: escapeHtml(url),
  };

  return base
    .replace(/<title>[^<]*<\/title>/, `<title>${escaped.title}</title>`)
    .replace(
      /<meta name="description"[^>]*>/,
      `<meta name="description" content="${escaped.description}" />`
    )
    .replace(
      /<meta property="og:title"[^>]*>/,
      `<meta property="og:title" content="${escaped.title}" />`
    )
    .replace(
      /<meta property="og:description"[^>]*>/,
      `<meta property="og:description" content="${escaped.description}" />`
    )
    .replace(
      /<meta property="og:url"[^>]*>/,
      `<meta property="og:url" content="${escaped.url}" />`
    )
    .replace(
      /<meta property="og:type"[^>]*>/,
      `<meta property="og:type" content="profile" />`
    )
    .replace(
      /<meta property="og:image"[^>]*>/,
      `<meta property="og:image" content="${escaped.image}" />`
    )
    .replace(
      /<meta name="twitter:title"[^>]*>/,
      `<meta name="twitter:title" content="${escaped.title}" />`
    )
    .replace(
      /<meta name="twitter:description"[^>]*>/,
      `<meta name="twitter:description" content="${escaped.description}" />`
    )
    .replace(
      /<meta name="twitter:image"[^>]*>/,
      `<meta name="twitter:image" content="${escaped.image}" />`
    )
    .replace(
      /<link rel="canonical"[^>]*>/,
      `<link rel="canonical" href="${escaped.url}" />`
    );
}

function buildDefaultHtml(urlPath: string): string {
  return buildFullHtml(
    "W3nto — Web3 Profile Builder",
    "Create your Web3 Bento profile. Connect your Solana wallet, add custom blocks, and share your links with the world.",
    "https://w3nto.app/og-default.svg",
    `https://w3nto.app${urlPath}`
  );
}

function buildFullHtml(
  title: string,
  description: string,
  image: string,
  url: string
): string {
  const e = {
    title: escapeHtml(title),
    description: escapeHtml(description),
    image: escapeHtml(image),
    url: escapeHtml(url),
  };
  return `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
    <link rel="manifest" href="/manifest.json" />
    <meta name="theme-color" content="#8b5cf6" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>${e.title}</title>
    <meta name="description" content="${e.description}" />
    <meta property="og:type" content="profile" />
    <meta property="og:title" content="${e.title}" />
    <meta property="og:description" content="${e.description}" />
    <meta property="og:url" content="${e.url}" />
    <meta property="og:site_name" content="W3nto" />
    <meta property="og:image" content="${e.image}" />
    <meta property="og:image:width" content="1200" />
    <meta property="og:image:height" content="630" />
    <meta name="twitter:card" content="summary_large_image" />
    <meta name="twitter:title" content="${e.title}" />
    <meta name="twitter:description" content="${e.description}" />
    <meta name="twitter:image" content="${e.image}" />
    <link rel="canonical" href="${e.url}" />
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>`;
}

function buildMinimalShell(): string {
  return `<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>W3nto</title>
  </head>
  <body>
    <div id="root"></div>
    <script type="module" src="/src/main.tsx"></script>
  </body>
</html>`;
}

function escapeHtml(str: string): string {
  return str
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}
