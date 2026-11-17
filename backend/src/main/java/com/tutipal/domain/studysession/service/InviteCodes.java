package com.tutipal.domain.studysession.service;

import java.security.SecureRandom;
import java.util.Locale;

/** 6자리 초대 코드. 헷갈리는 글자(0 O 1 I L)는 뺀다. */
final class InviteCodes {

    static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    private InviteCodes() {
    }

    static String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    /** 사용자가 입력한 코드: 공백 · 하이픈 제거, 대문자로. */
    static String normalize(String input) {
        return input.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }
}
