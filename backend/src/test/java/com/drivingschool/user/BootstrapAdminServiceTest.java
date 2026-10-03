package com.drivingschool.user;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapAdminServiceTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
    private final BootstrapAdminService service = new BootstrapAdminService(userRepository, passwordEncoder);

    @Test
    void createsFirstAdminWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByRolesContaining(Role.ADMIN)).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("owner@example.com")).thenReturn(false);
        when(passwordEncoder.encode("strong-password")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createFirstAdmin(" Owner@Example.com ", "strong-password");

        var userCaptor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("owner@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(userCaptor.getValue().getRoles()).containsExactly(Role.ADMIN);
        verify(passwordEncoder).encode("strong-password");
    }

    @Test
    void refusesToCreateAdminWhenOneAlreadyExists() {
        when(userRepository.existsByRolesContaining(Role.ADMIN)).thenReturn(true);

        assertThatThrownBy(() -> service.createFirstAdmin("owner@example.com", "strong-password"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void refusesWeakBootstrapPassword() {
        assertThatThrownBy(() -> service.createFirstAdmin("owner@example.com", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("between 12 and 72 UTF-8 bytes");

        verify(userRepository, never()).save(any(User.class));
    }
}
