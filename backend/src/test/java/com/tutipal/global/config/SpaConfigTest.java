package com.tutipal.global.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpaConfigTest {

    @Test
    void screenAddressesGetIndexHtml() {
        for (String path : new String[] {"", "login", "chat", "widget", "session/12"}) {
            assertTrue(SpaConfig.isSpaRoute(path), path);
        }
    }

    @Test
    void apiAuthAndMissingFilesDoNot() {
        for (String path : new String[] {"api/unknown", "ws", "oauth2/authorization/kakao",
                "login/oauth2/code/kakao", "dev/login", "health", "logout", "assets/missing.js", "favicon.ico"}) {
            assertFalse(SpaConfig.isSpaRoute(path), path);
        }
    }
}
