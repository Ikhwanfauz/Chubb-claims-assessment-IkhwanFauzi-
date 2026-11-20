package com.chubb.claims;

class InvalidClaimTransition extends RuntimeException {
    InvalidClaimTransition(String message) {
        super(message);
    }
}
