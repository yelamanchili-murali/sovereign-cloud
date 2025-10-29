package mry.ccdemos.helloattestation.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AttestationConfig {
    @Value("${attestation.clientbin.path}")
    private String attestationClientbinPath;

    @Value("${attestation.token.file}")
    private String attestationTokenFile;

    @Value("${attestation.tenant.url}")
    private String attestationTenantUrl;

    public String getAttestationClientbinPath() {
        return attestationClientbinPath;
    }

    public String getAttestationTokenFile() {
        return attestationTokenFile;
    }

    public String getAttestationTenantUrl() {
        return attestationTenantUrl;
    }

    // generate toString() method
    @Override
    public String toString() {
        return "AttestationConfig{" +
                "attestationClientbinPath='" + attestationClientbinPath + '\'' +
                ", attestationTokenFile='" + attestationTokenFile + '\'' +
                ", attestationTenantUrl='" + attestationTenantUrl + '\'' +
                '}';
    }
}
