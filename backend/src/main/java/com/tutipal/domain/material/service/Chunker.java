package com.tutipal.domain.material.service;

import java.util.ArrayList;
import java.util.List;

/**
 * 자료 글을 검색용 조각으로 자른다. 문단(빈 줄) 단위로 모으다가 size 를 넘으면 자르고, 앞 조각 끝 일부를 겹쳐 문맥을 잇는다.
 * ponytail: 글자 수 기준 단순 분할. 검색 품질이 아쉬우면 문장 · 제목 단위 분할로
 */
final class Chunker {

    private Chunker() {
    }

    static List<String> split(String text, int size, int overlap) {
        String clean = text.replace("\r\n", "\n").replaceAll("[ \\t]+", " ").replaceAll("\n{3,}", "\n\n").strip();
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String para : clean.split("\n\n")) {
            for (int i = 0; i < para.length(); i += size) {   // 너무 긴 문단은 size 단위로
                String piece = para.substring(i, Math.min(para.length(), i + size));
                if (cur.length() > 0 && cur.length() + piece.length() > size) {
                    out.add(cur.toString().strip());
                    String done = cur.toString();
                    cur = new StringBuilder(done.substring(Math.max(0, done.length() - overlap)));
                }
                cur.append(piece).append("\n\n");
            }
        }
        if (!cur.toString().isBlank()) {
            out.add(cur.toString().strip());
        }
        return out.stream().filter(s -> s.length() >= 20).toList();   // 쪽 번호 · 머리글 같은 짧은 조각은 버림
    }
}
