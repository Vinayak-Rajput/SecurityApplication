package com.vinayak.secapp.auth.config;

import com.vinayak.secapp.auth.config.UserAuthenticationConfig;
import com.vinayak.secapp.user.entity.User;
import com.vinayak.secapp.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationConfigTest {

    private final UserAuthenticationConfig config = new UserAuthenticationConfig();

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("UserDetailsService Mapping Tests")
    class UserDetailsServiceTests {

        private UserDetailsService userDetailsService;

        @BeforeEach
        void setUp() {
            userDetailsService = config.userDetailsService(userRepository);
        }

        @Test
        @DisplayName("Should load user and map username, password, authority, and status flags correctly")
        void loadsUserAndMapsDetailsCorrectly() {
            // Given domain user: enabled, not locked, role "USER"
            User domainUser = mock(User.class);
            given(domainUser.getUsername()).willReturn("vinayak");
            given(domainUser.getPassword()).willReturn("encoded_secret");
            given(domainUser.getRole()).willReturn("USER");
            given(domainUser.isEnabled()).willReturn(true);
            given(domainUser.isLocked()).willReturn(false);

            given(userRepository.findByUsername("vinayak")).willReturn(Optional.of(domainUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("vinayak");

            // Then
            assertThat(userDetails.getUsername()).isEqualTo("vinayak");
            assertThat(userDetails.getPassword()).isEqualTo("encoded_secret");
            assertThat(userDetails.isEnabled()).isTrue();
            assertThat(userDetails.isAccountNonLocked()).isTrue();
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_USER");
        }

        @Test
        @DisplayName("Should correctly map disabled and account-locked flags to UserDetails")
        void mapsDisabledAndLockedFlags() {
            // Given domain user: disabled and locked
            User domainUser = mock(User.class);
            given(domainUser.getUsername()).willReturn("locked_user");
            given(domainUser.getPassword()).willReturn("secret");
            given(domainUser.getRole()).willReturn("ADMIN");
            given(domainUser.isEnabled()).willReturn(false);
            given(domainUser.isLocked()).willReturn(true);

            given(userRepository.findByUsername("locked_user")).willReturn(Optional.of(domainUser));

            // When
            UserDetails userDetails = userDetailsService.loadUserByUsername("locked_user");

            // Then
            assertThat(userDetails.isEnabled()).isFalse();
            assertThat(userDetails.isAccountNonLocked()).isFalse();
            assertThat(userDetails.getAuthorities())
                    .extracting(GrantedAuthority::getAuthority)
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("Should throw UsernameNotFoundException when username does not exist in repository")
        void throwsExceptionWhenUserNotFound() {
            given(userRepository.findByUsername("unknown_user")).willReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("unknown_user"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("User not found");
        }
    }

    @Nested
    @DisplayName("PasswordEncoder Bean Tests")
    class PasswordEncoderTests {

        @Test
        @DisplayName("passwordEncoder() should return a BCryptPasswordEncoder instance")
        void returnsBCryptPasswordEncoder() {
            PasswordEncoder encoder = config.passwordEncoder();

            assertThat(encoder).isInstanceOf(BCryptPasswordEncoder.class);
            
            // Verify BCrypt hashing and matching behavior
            String rawPassword = "mySecurePassword123";
            String encoded = encoder.encode(rawPassword);

            assertThat(encoded).isNotEqualTo(rawPassword);
            assertThat(encoder.matches(rawPassword, encoded)).isTrue();
        }
    }

    @Nested
    @DisplayName("Spring ApplicationContext Integration")
    class ContextWiringTests {

        private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
                .withUserConfiguration(UserAuthenticationConfig.class)
                .withBean(UserRepository.class, () -> mock(UserRepository.class));

        @Test
        @DisplayName("Context should wire UserDetailsService and PasswordEncoder beans cleanly")
        void contextLoadsBeansSuccessfully() {
            contextRunner.run(context -> {
                assertThat(context).hasSingleBean(UserDetailsService.class);
                assertThat(context).hasSingleBean(PasswordEncoder.class);
                assertThat(context.getBean(PasswordEncoder.class)).isInstanceOf(BCryptPasswordEncoder.class);
            });
        }
    }
}