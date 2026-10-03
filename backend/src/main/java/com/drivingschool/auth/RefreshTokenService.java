package com.drivingschool.auth;

import com.drivingschool.security.AppUserDetails;
import com.drivingschool.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final Duration expiration;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            @Value("${app.jwt.refresh-expiration}") Duration expiration
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.expiration = expiration;
    }

    @Transactional
    public String issue(UUID userId) {
        String rawToken = generateToken();
        refreshTokenRepository.save(new RefreshToken(
                userRepository.getReferenceById(userId),
                hash(rawToken),
                Instant.now().plus(expiration)
        ));
        return rawToken;
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        Instant now = Instant.now();
        RefreshToken existing = refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null && token.getExpiresAt().isAfter(now))
                .orElseThrow(InvalidRefreshTokenException::new);

        existing.revoke(now);
        AppUserDetails user = AppUserDetails.from(existing.getUser());
        return new Rotation(user, issue(user.id()));
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHashForUpdate(hash(rawToken))
                .filter(token -> token.getRevokedAt() == null)
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    public record Rotation(AppUserDetails user, String refreshToken) {
    }
}
