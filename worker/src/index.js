/**
 * Cloudflare Worker — Quran.com OAuth 2.0 Token Exchange Proxy
 *
 * Purpose:
 *   The Android app uses AppAuth-Android with PKCE. AppAuth sends the authorization_code
 *   and code_verifier to this Worker instead of directly to Quran Foundation.
 *   This Worker injects the client_secret (keeping it off-device) and forwards the
 *   request to the real /oauth2/token endpoint.
 *
 * Endpoints:
 *   POST /token     — proxies to /oauth2/token  (auth code exchange + refresh)
 *   POST /revoke    — proxies to /oauth2/revoke  (token revocation)
 *   GET  /health    — returns 200 OK
 *
 * The client_id in the request body determines which environment (test vs prod) to use.
 *
 * Auth method: client_secret_basic — credentials sent via Authorization header,
 *   NOT in the request body (client_secret_post), as required by the Quran Foundation
 *   OAuth 2.0 server.
 */
const CLIENT_CONFIG = {
  // Pre-Production (Test)
  "YOUR_TEST_CLIENT_ID": {
    secret: "YOUR_TEST_CLIENT_SECRET",
    tokenUrl: "https://prelive-oauth2.quran.foundation/oauth2/token",
    revokeUrl: "https://prelive-oauth2.quran.foundation/oauth2/revoke",
  },
  // Production (Live)
  "YOUR_PROD_CLIENT_ID": {
    secret: "YOUR_PROD_CLIENT_SECRET",
    tokenUrl: "https://oauth2.quran.foundation/oauth2/token",
    revokeUrl: "https://oauth2.quran.foundation/oauth2/revoke",
  },
};

function corsHeaders() {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
  };
}

function handleOptions() {
  return new Response(null, { status: 204, headers: corsHeaders() });
}

async function proxyRequest(request, upstreamUrlKey) {
  const body = await request.text();
  const params = new URLSearchParams(body);
  const clientId = params.get("client_id");

  if (!clientId || !CLIENT_CONFIG[clientId]) {
    return new Response(
      JSON.stringify({
        error: "invalid_client",
        error_description: "Unknown client_id. Check QuranOAuthConfig on the Android side.",
      }),
      { status: 400, headers: { "Content-Type": "application/json", ...corsHeaders() } }
    );
  }

  const config = CLIENT_CONFIG[clientId];

  // Remove client_secret from body — server requires client_secret_basic,
  // meaning credentials must go in the Authorization header, not the POST body.
  params.delete("client_secret");

  // Build Basic Auth header: base64(client_id:client_secret)
  const credentials = btoa(`${clientId}:${config.secret}`);

  const upstream = await fetch(config[upstreamUrlKey], {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
      "Authorization": `Basic ${credentials}`,
    },
    body: params.toString(),
  });

  const responseBody = await upstream.text();
  return new Response(responseBody, {
    status: upstream.status,
    headers: { "Content-Type": "application/json", ...corsHeaders() },
  });
}

export default {
  async fetch(request) {
    const url = new URL(request.url);

    // CORS preflight
    if (request.method === "OPTIONS") {
      return handleOptions();
    }

    // Health check
    if (request.method === "GET" && url.pathname === "/health") {
      return new Response("OK", { status: 200, headers: corsHeaders() });
    }

    // Token exchange & refresh
    if (request.method === "POST" && url.pathname === "/token") {
      return proxyRequest(request, "tokenUrl");
    }

    // Token revocation
    if (request.method === "POST" && url.pathname === "/revoke") {
      return proxyRequest(request, "revokeUrl");
    }

    return new Response("Not Found", { status: 404, headers: corsHeaders() });
  },
};