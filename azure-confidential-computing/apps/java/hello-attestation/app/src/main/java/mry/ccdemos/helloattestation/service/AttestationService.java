package mry.ccdemos.helloattestation.service;

import mry.ccdemos.helloattestation.model.ExecutionTrail;

public interface AttestationService {
    String runAndGetJwt(ExecutionTrail trail);
}
