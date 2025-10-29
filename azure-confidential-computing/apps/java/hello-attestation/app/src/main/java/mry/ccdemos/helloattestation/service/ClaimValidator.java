package mry.ccdemos.helloattestation.service;

import com.nimbusds.jwt.JWTClaimsSet;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class ClaimValidator {

    private static final String AZURE_COMPLIANT_CVM = "azure-compliant-cvm";
    private static final String SECURE_BOOT = "secure-boot";
    private static final String COMPLIANCE_CHECK_FAILED = "Compliance check failed: ";
    private static final String SECURE_BOOT_ERROR = "Secure Boot is not enabled";
    private static final String EXPIRED_TOKEN_ERROR = "Expired JWT token";
    private static final String NOT_YET_VALID_ERROR = "JWT not yet valid";

    public void validateTemporalClaims(JWTClaimsSet claims) {
        var now = new Date();
        if (claims.getExpirationTime() != null && now.after(claims.getExpirationTime())) {
            throw new SecurityException(EXPIRED_TOKEN_ERROR);
        }
        if (claims.getNotBeforeTime() != null && now.before(claims.getNotBeforeTime())) {
            throw new SecurityException(NOT_YET_VALID_ERROR);
        }
    }

    public void validateCustomClaims(JWTClaimsSet claims) {
        /*
            "x-ms-isolation-tee": {
                "x-ms-attestation-type": "sevsnpvm",
                "x-ms-compliance-status": "azure-compliant-cvm",
                ...
         */
        String compliance = nestedString(claims, "x-ms-isolation-tee", "x-ms-compliance-status");
        if (!AZURE_COMPLIANT_CVM.equalsIgnoreCase(compliance)) {
            throw new SecurityException(COMPLIANCE_CHECK_FAILED + compliance);
        }

        /*
            "x-ms-isolation-tee": {
                ...
                "x-ms-runtime": {
                  ...
                  "user-data": ...,
                  "vm-configuration": {
                    "console-enabled": true,
                    "secure-boot": true,
                    "tpm-enabled": true,
                    ...
         */

        Boolean secureBoot = nestedBoolean(claims, "x-ms-isolation-tee", "x-ms-runtime", "vm-configuration", SECURE_BOOT);
        if (secureBoot == null || !secureBoot) {
            throw new SecurityException(SECURE_BOOT_ERROR);
        }
    }

    private static Object nestedValue(JWTClaimsSet claims, String... path) {
        Object cur = claims.getClaims();
        for (String p : path) {
            if (!(cur instanceof java.util.Map<?, ?> m)) return null;
            cur = m.get(p);
            if (cur == null) return null;
        }
        return cur;
    }

    private static String nestedString(JWTClaimsSet claims, String... path) {
        Object value = nestedValue(claims, path);
        return (value instanceof String s) ? s : null;
    }

    private static Boolean nestedBoolean(JWTClaimsSet claims, String... path) {
        Object value = nestedValue(claims, path);
        return (value instanceof Boolean b) ? b : null;
    }
}
