package mry.ccdemos.helloattestation.service;

import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.web.client.RestClient;
import org.springframework.stereotype.Service;

@Service
public class JwksFetcher {

    private final RestClient http;

    public JwksFetcher(RestClient restClient) {
        this.http = restClient;
    }

    public JWKSet fetchJwks(String openidConfigUrl) {
        String oidcJson = http.get().uri(openidConfigUrl).retrieve().body(String.class);
        String jwksUri = extractJwksUri(oidcJson);

        String jwksJson = http.get().uri(jwksUri).retrieve().body(String.class);
        if (jwksJson == null) {
            throw new SecurityException("JWKS JSON response is null");
        }

        try {
            return JWKSet.parse(jwksJson);
        } catch (Exception e) {
            throw new SecurityException("Failed to parse JWKS JSON", e);
        }
    }

    private String extractJwksUri(String json) {
        String needle = "\"" + "jwks_uri" + "\"";
        int i = json.indexOf(needle);
        if (i < 0) throw new IllegalStateException("Field not found: " + "jwks_uri");
        int q1 = json.indexOf('"', json.indexOf(':', i) + 1);
        int q2 = json.indexOf('"', q1 + 1);
        return json.substring(q1 + 1, q2);
    }
}
