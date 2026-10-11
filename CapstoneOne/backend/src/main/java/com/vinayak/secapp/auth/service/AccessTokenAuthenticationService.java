package com.vinayak.secapp.auth.service;

import com.vinayak.secapp.auth.service.JwtService.TokenDetails;
import com.vinayak.secapp.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AccessTokenAuthenticationService {

    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;

    public Optional<UserDetails> loadActiveUser(TokenDetails tokenDetails) {
        if (!tokenService.isAccessTokenActive(tokenDetails.sessionId(), tokenDetails.tokenId())) {
            return Optional.empty();
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(tokenDetails.username());
        if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
            return Optional.empty();
        }

        return Optional.of(userDetails);
    }
}
