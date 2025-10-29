package mry.ccdemos.helloattestation.service;

import mry.ccdemos.helloattestation.model.VerifiedToken;

public interface JwtVerifier {
    VerifiedToken verify(String token);
}
