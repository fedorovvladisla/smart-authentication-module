package com.vkr.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private JwtService jwtService;

    // Тестовые секреты (минимум 256 бит для HS256)
    private static final String ACCESS_SECRET = "myaccesssecretmyaccesssecretmyaccesssecret12";
    private static final String REFRESH_SECRET = "myrefreshsecretmyrefreshsecretmyrefreshsecret12";
    private static final long ACCESS_EXPIRATION_MS = 900000;   // 15 минут
    private static final long REFRESH_EXPIRATION_MS = 604800000; // 7 дней

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                ACCESS_SECRET,
                REFRESH_SECRET,
                ACCESS_EXPIRATION_MS,
                REFRESH_EXPIRATION_MS
        );
    }

    @Test
    void shouldGenerateValidAccessToken() {
        // Генерируем access-токен для пользователя user1 с ролью USER
        String token = jwtService.generateAccessToken("user1", "USER");
        // Проверяем, что токен не пустой и содержит точку (JWT)
        assertThat(token).isNotEmpty().contains(".");

        // Валидируем токен и проверяем claims
        Jws<Claims> claims = jwtService.validateAccessToken(token);
        assertThat(claims.getPayload().getSubject()).isEqualTo("user1");
        assertThat(claims.getPayload().get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void shouldGenerateValidRefreshToken() {
        String token = jwtService.generateRefreshToken("user1");
        assertThat(token).isNotEmpty().contains(".");

        Jws<Claims> claims = jwtService.validateRefreshToken(token);
        assertThat(claims.getPayload().getSubject()).isEqualTo("user1");
    }

    @Test
    void shouldDetectExpiredAccessToken() throws InterruptedException {
        // Создаём сервис с очень коротким временем жизни access-токена (100 мс)
        JwtService shortLivedService = new JwtService(
                ACCESS_SECRET,
                REFRESH_SECRET,
                100,   // 100 мс
                1000
        );
        String token = shortLivedService.generateAccessToken("user1", "USER");
        // Ждём чуть больше 100 мс
        Thread.sleep(200);

        // Ожидаем исключение ExpiredJwtException
        assertThrows(ExpiredJwtException.class,
                () -> shortLivedService.validateAccessToken(token));
    }

    @Test
    void shouldRejectTokenWithInvalidSignature() {
        // Генерируем токен с одним секретом
        String token = jwtService.generateAccessToken("user1", "USER");
        // Создаём другой сервис с другим секретом
        JwtService otherService = new JwtService(
                "differentaccesssecretdifferentaccesssecret12",
                REFRESH_SECRET,
                ACCESS_EXPIRATION_MS,
                REFRESH_EXPIRATION_MS
        );
        // Попытка валидации токена другим сервисом должна вызвать исключение
        assertThrows(SignatureException.class,
                () -> otherService.validateAccessToken(token));
    }

    @Test
    void shouldRejectEmptyOrNullToken() {
        // Пустой токен должен выбрасывать исключение
        assertThrows(IllegalArgumentException.class,
                () -> jwtService.validateAccessToken(""));
        assertThrows(IllegalArgumentException.class,
                () -> jwtService.validateAccessToken(null));
    }
}