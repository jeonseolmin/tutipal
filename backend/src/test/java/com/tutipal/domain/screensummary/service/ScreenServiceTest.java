package com.tutipal.domain.screensummary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.tutipal.global.exception.ApiException;

class ScreenServiceTest {

    static String b64(int... bytes) {
        byte[] b = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            b[i] = (byte) bytes[i];
        }
        return Base64.getEncoder().encodeToString(b);
    }

    @Test
    void acceptsJpegAndPngAndStripsDataUrl() {
        String jpeg = b64(0xFF, 0xD8, 0xFF, 0xE0, 1, 2, 3);
        assertThat(ScreenService.checkImage(jpeg)).isEqualTo(jpeg);
        assertThat(ScreenService.checkImage("data:image/jpeg;base64," + jpeg)).isEqualTo(jpeg);
        assertThat(ScreenService.checkImage(b64(0x89, 'P', 'N', 'G', 13, 10))).isNotBlank();
    }

    @Test
    void rejectsNonImagesAndBrokenBase64() {
        assertThatThrownBy(() -> ScreenService.checkImage(b64('G', 'I', 'F', '8', '9'))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ScreenService.checkImage("not base64 !!")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> ScreenService.checkImage(b64(0xFF, 0xD8, 0xFF) + "@@@@")).isInstanceOf(ApiException.class);
    }
}
