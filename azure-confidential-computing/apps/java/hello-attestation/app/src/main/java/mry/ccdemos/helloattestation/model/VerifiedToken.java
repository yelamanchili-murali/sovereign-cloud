package mry.ccdemos.helloattestation.model;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;

public record VerifiedToken(JWSHeader header, JWTClaimsSet  claims) {}
