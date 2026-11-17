package com.tutipal.global.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * React 화면(빌드 결과, classpath:/static)을 Spring 이 직접 준다 (nginx 없음).
 * 파일이 없는 화면 주소(/login, /chat, /widget …)는 index.html 로 → React 라우터가 처리.
 * 컨트롤러(/api …)가 먼저 매칭되므로 여기에는 API 가 아닌 요청만 온다.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    /** 화면이 아닌 경로: 없는 파일이면 index.html 대신 그냥 404 */
    private static final String[] NOT_SPA = {"api/", "ws", "oauth2/", "login/oauth2/", "dev/", "health", "logout"};

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource file = location.createRelative(path);
                        if (file.exists() && file.isReadable()) {
                            return file;
                        }
                        if (isSpaRoute(path)) {
                            Resource index = location.createRelative("index.html");
                            return index.exists() ? index : null;
                        }
                        return null;
                    }
                });
    }

    static boolean isSpaRoute(String path) {
        if (path.contains(".")) {
            return false;   // 없는 파일(assets/xx.js 등)은 404
        }
        for (String prefix : NOT_SPA) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }
}
