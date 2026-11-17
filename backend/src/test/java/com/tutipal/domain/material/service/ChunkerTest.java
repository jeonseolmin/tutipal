package com.tutipal.domain.material.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ChunkerTest {

    @Test
    void keepsShortTextAsOneChunk() {
        assertThat(Chunker.split("자바의 for 문은 정해진 횟수만큼 반복할 때 쓴다.\n\n예: for (int i = 0; i < 10; i++)", 800, 100))
                .hasSize(1);
    }

    @Test
    void splitsLongTextWithinSizePlusOverlap() {
        String para = "가".repeat(300);
        List<String> chunks = Chunker.split(String.join("\n\n", para, para, para, para, para), 800, 100);
        assertThat(chunks.size()).isGreaterThan(1);
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(800 + 100 + 4));
        // 모든 글자가 어딘가에 들어 있음 (겹침 때문에 합은 원문보다 길거나 같음)
        assertThat(chunks.stream().mapToInt(String::length).sum()).isGreaterThanOrEqualTo(1500);
    }

    @Test
    void hardSplitsOneHugeParagraph() {
        List<String> chunks = Chunker.split("a".repeat(2500), 800, 100);
        assertThat(chunks.size()).isGreaterThanOrEqualTo(3);
        assertThat(chunks).allSatisfy(c -> assertThat(c.length()).isLessThanOrEqualTo(904));
    }

    @Test
    void dropsTinyFragments() {
        assertThat(Chunker.split("3\n\n\n\n- 12 -", 800, 100)).isEmpty();
    }

    @Test
    void safeNameStripsPathsAndControlChars() {
        assertThat(MaterialService.safeName("C:\\Users\\x\\..\\반복문.pdf")).isEqualTo("반복문.pdf");
        assertThat(MaterialService.safeName("../../etc/pass\"wd\u0007.txt")).isEqualTo("passwd.txt");
        assertThat(MaterialService.safeName(null)).isEqualTo("자료");
        assertThat(MaterialService.extension("노트.MD")).isEqualTo("md");
    }
}
