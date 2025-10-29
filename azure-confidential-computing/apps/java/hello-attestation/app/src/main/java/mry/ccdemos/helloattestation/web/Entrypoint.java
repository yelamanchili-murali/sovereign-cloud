package mry.ccdemos.helloattestation.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import mry.ccdemos.helloattestation.model.ExecutionTrail;
import mry.ccdemos.helloattestation.service.AttestationService;
import mry.ccdemos.helloattestation.service.JwtVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class Entrypoint {

    private final AttestationService attestationService;
    private final JwtVerifier jwtVerifier;

    public Entrypoint(AttestationService attestationService, JwtVerifier jwtVerifier) {
        this.attestationService = attestationService;
        this.jwtVerifier = jwtVerifier;
    }

    @GetMapping("/execute")
    public ResponseEntity<String> execute(@RequestParam(value = "includeDetails", defaultValue = "false") boolean includeDetails) {
        ExecutionTrail trail = new ExecutionTrail();
        try {
            trail.add("Boot: starting attestation flow");

            // 1) Collect attestation evidence and get MAA JWT
            String token = attestationService.runAndGetJwt(trail);
            trail.add("Collected evidence and obtained MAA token");

            // 2) Verify signature and mandatory claims
            var verified = jwtVerifier.verify(token);
            JWSHeader header = verified.header();
            JWTClaimsSet claims = verified.claims();

            trail.add("Verified token signature (alg = " + header.getAlgorithm() + ", kid = " + header.getKeyID() + ")");
            trail.add("Verified claims: x-ms-compliance-status=azure-compliant-vm, secure-boot=true");

            // 3) Run protected business logic(demo)
            String result = protectedBusinessAction(trail);

            // Prepare response (omit the signature to keep output readable)
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("trail", trail.getSteps());

            // only include details if requested
            if(includeDetails) {
                body.put("tokenHeader", header.toJSONObject());
                body.put("tokenClaims", claims.toJSONObject());
            }
            body.put("businessResult", result);

            return ResponseEntity.ok(convertToJson(body));
        } catch(SecurityException se) {
            trail.add("DENY business logic: " + se.getMessage());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("trail", trail.getSteps());
            body.put("error", se.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(convertToJson(body));
        } catch (Exception e) {
            trail.add("ERROR: " + e.getMessage());
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("trail", trail.getSteps());
            body.put("error", "Internal error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(convertToJson(body));
        } finally {
            trail.add("Flow ended");
        }
    }

    // Simple stand-in "business action": decrypt/transform after attestation.
    private String protectedBusinessAction(ExecutionTrail trail) {
        trail.add("Business action: allowed (attestation passed)");
        // Keep it trivial for this demo (can be replaced with real SKR/Key Vault).
        return "secret-unlocked";
    }

    private String convertToJson(Map<String, Object> body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.writeValueAsString(body);
        } catch(Exception e) {
            throw new RuntimeException("JSON serialization error", e);
        }
    }
}
