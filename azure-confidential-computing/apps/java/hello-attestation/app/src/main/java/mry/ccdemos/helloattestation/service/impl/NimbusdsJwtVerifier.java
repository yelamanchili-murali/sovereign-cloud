package mry.ccdemos.helloattestation.service.impl;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import mry.ccdemos.helloattestation.model.VerifiedToken;
import mry.ccdemos.helloattestation.service.JwtVerifier;
import mry.ccdemos.helloattestation.service.JwksFetcher;
import mry.ccdemos.helloattestation.service.ClaimValidator;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPublicKey;

@Service
public class NimbusdsJwtVerifier implements JwtVerifier {

    private static final String ISSUER_CLAIM_ERROR = "Missing iss (issuer) claim";
    private static final String JWS_SIGNATURE_ERROR = "Invalid JWS signature";

    private final JwksFetcher jwksFetcher;
    private final ClaimValidator claimValidator;

    public NimbusdsJwtVerifier(JwksFetcher jwksFetcher, ClaimValidator claimValidator) {
        this.jwksFetcher = jwksFetcher;
        this.claimValidator = claimValidator;
    }

    /**
     * Verify signature using the attestation service's JWKS discovered via the token's "iss".
     * Enforce required claims recommended by Microsoft docs for Azure CVMs:
     *  - x-ms-isolation-tee.x-ms-compliance-status == "azure-compliant-vm"
     *  - xm-ms-runtime.vm-configuration.secure-boot == true
     *  - (optional) x-ms-runtime.vm-configuration.tmp-enabled == true
     */
    @Override
    public VerifiedToken verify(String token) {
        try {
            SignedJWT jws = SignedJWT.parse(token);
            JWTClaimsSet claims = jws.getJWTClaimsSet();
            JWSHeader header = jws.getHeader();

            // Discover JWKS from the issuer
            String iss = claims.getIssuer();
            if (iss == null || iss.isBlank()) {
                throw new SecurityException(ISSUER_CLAIM_ERROR);
            }
            String openid = normalize(iss) + "/.well-known/openid-configuration";

            JWKSet jwkSet = jwksFetcher.fetchJwks(openid);
            JWK key = jwkSet.getKeyByKeyId(header.getKeyID());

            if (key == null) throw new SecurityException("Key not found in JWKS: " + header.getKeyID() + ")");
            if (!(key instanceof RSAKey rsa)) throw new SecurityException("Expected RSA key");

            RSAPublicKey pub = rsa.toRSAPublicKey();
            if (!jws.verify(new RSASSAVerifier(pub))) {
                throw new SecurityException(JWS_SIGNATURE_ERROR);
            }

            claimValidator.validateTemporalClaims(claims);
            claimValidator.validateCustomClaims(claims);

            return new VerifiedToken(header, claims);

        } catch (Exception e) {
            throw new SecurityException("Token verification error: " + e.getMessage(), e);
        }
    }

    private static String normalize(String iss) {
        // Ensure no trailing slash duplication
        return iss.endsWith("/") ? iss.substring(0, iss.length() - 1) : iss;
    }
}
