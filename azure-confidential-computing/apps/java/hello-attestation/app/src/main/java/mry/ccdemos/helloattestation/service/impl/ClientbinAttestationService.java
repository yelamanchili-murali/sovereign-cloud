package mry.ccdemos.helloattestation.service.impl;

import mry.ccdemos.helloattestation.config.AttestationConfig;
import mry.ccdemos.helloattestation.model.ExecutionTrail;
import mry.ccdemos.helloattestation.service.AttestationService;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

@Service
public class ClientbinAttestationService implements AttestationService {

    private final String clientPath;
    private final String tokenFile;
    private final String attestationTenantUrl;

    public ClientbinAttestationService(AttestationConfig attestationConfig) {
        System.out.println("AttestationConfig: " + attestationConfig);

        this.clientPath = attestationConfig.getAttestationClientbinPath();
        this.tokenFile = attestationConfig.getAttestationTokenFile();
        this.attestationTenantUrl = attestationConfig.getAttestationTenantUrl();
    }

    /**
     * Runs Microsoft's AttestationClient and returns the JWT string.
     * We use "-o <file>" so we don't have to guess stdout format
     */
    @Override
    public String runAndGetJwt(ExecutionTrail trail) {

        Path tokenPath = Path.of(tokenFile);
        File tokenFile = tokenPath.toFile();

        try {
            var cmd = buildAttestationCommand();
            trail.add("Invoking AttestationClient: " + String.join(" ", cmd));

            var pb = new ProcessBuilder(cmd);

            // stdout -> file (equivalent to `> /var/tmp/maa_token.jwt`)
            pb.redirectOutput(tokenFile);

            // keep stderr visible (either pipe to trail or inherit to console)
            pb.redirectError(ProcessBuilder.Redirect.PIPE);

            trail.add("Invoking: /usr/local/bin/AttestationClient -o token > " + tokenPath);
            var proc = pb.start();

            // Capture stderr into the trail
            StringBuilder errorOutput = new StringBuilder();
            Thread errT = getErrorCaptureThread(trail, proc, errorOutput);

            int code = proc.waitFor();
            errT.join();
            trail.add("AttestationClient exited with code " + code);

            if(code != 0) {
                throw new IllegalStateException("AttestationClient failed with code " + code + ". Errors: " + errorOutput);
            }

            if(!tokenFile.exists() || tokenFile.length() == 0) {
                throw new IllegalStateException("Attestation token file missing or empty: " + tokenPath);
            }

            String jwt = Files.readString(tokenPath, StandardCharsets.UTF_8).trim();
            if(!jwt.isBlank()) {
                trail.add("AttestationClient output (truncated): " +
                        jwt.substring(0, Math.min(60, jwt.length())).replace('\n', ' '));
            }

            if(!jwt.matches("^[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+\\.[A-Za-z0-9_\\-]+$")) {
                throw new IllegalStateException("Attestation token file does not contain a valid JWT: " + jwt);
            }
            trail.add("Wrote token file " + tokenPath + " (len = " + tokenFile.length() + ")");
            trail.add("Obtained JWT from AttestationClient (len=" + jwt.length() + ")");
            return jwt;

        } catch(IOException | InterruptedException e) {
            throw new RuntimeException("Attestation invocation failed", e);
        }
    }

    private static Thread getErrorCaptureThread(ExecutionTrail trail, Process proc, StringBuilder errorOutput) {
        Thread errT = new Thread(() -> {
            try(var errReader = new BufferedReader(new InputStreamReader(proc.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while((line = errReader.readLine()) != null) {
                        errorOutput.append(line).append(System.lineSeparator());
                        trail.add("stderr: " + line);
                }
            } catch(Exception e) {
                throw new IllegalStateException("AttestationClient failed. Errors: " + e.getMessage());
            }
        }, "stderr-gobbler");
        errT.setDaemon(true);
        errT.start();
        return errT;
    }

    private ArrayList<String> buildAttestationCommand() {
        // Build command: AttestationClient [-t <tenantUrl>] -o <tokenFile>

        var cmd = new ArrayList<String>();
        cmd.add(clientPath);

        if(!attestationTenantUrl.isBlank()) {
            cmd.add("-a");cmd.add(attestationTenantUrl);
        }

        cmd.add("-o");cmd.add("token");
        return cmd;
    }
}
