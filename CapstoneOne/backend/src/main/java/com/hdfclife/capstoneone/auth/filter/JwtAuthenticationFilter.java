package com.hdfclife.capstoneone.auth.filter;

import com.hdfclife.capstoneone.common.security.SecurityExceptionHandler;
import com.hdfclife.capstoneone.auth.exception.InvalidAuthorizationHeaderException;
import com.hdfclife.capstoneone.auth.exception.InvalidTokenException;
import com.hdfclife.capstoneone.auth.service.AccessTokenAuthenticationService;
import com.hdfclife.capstoneone.auth.security.BearerTokenResolver;
import com.hdfclife.capstoneone.auth.service.JwtService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final BearerTokenResolver bearerTokenResolver;
    private final AccessTokenAuthenticationService accessTokenAuthenticationService;
    private final SecurityExceptionHandler securityExceptionHandler;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        return "POST".equals(request.getMethod())
            && ("/logout".equals(request.getServletPath())
            || request.getServletPath().startsWith("/refresh"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null) {

            filterChain.doFilter(request, response);
            return;
        }

        final String jwt;

        try {
            jwt = bearerTokenResolver.resolve(authHeader);
        }

        catch (InvalidAuthorizationHeaderException exception) {

            SecurityContextHolder.clearContext();
            reject(request, response, exception.getMessage());

            return;
        }

        final JwtService.TokenDetails tokenDetails;

        try {

            tokenDetails = jwtService.parseToken(jwt);

            if (tokenDetails.refreshToken()) {
                reject(request, response, "Invalid or expired token.");
                return;
            }
        }

        catch (AuthenticationException exception) {

            SecurityContextHolder.clearContext();
            reject(request, response, "Authentication failed.");

            return;
        }

        catch (InvalidTokenException | JwtException | IllegalArgumentException exception) {

            SecurityContextHolder.clearContext();
            reject(request, response, "Invalid or expired token.");

            return;
        }

        try {

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                Optional<UserDetails> activeUser = accessTokenAuthenticationService.loadActiveUser(tokenDetails);

                if (activeUser.isEmpty()) {

                    SecurityContextHolder.clearContext();
                    reject(request, response, "Token is inactive.");

                    return;
                }

                UserDetails userDetails = activeUser.get();

                if (!tokenDetails.username().equals(userDetails.getUsername())) {

                    SecurityContextHolder.clearContext();
                    reject(request, response, "Invalid or expired token.");

                    return;
                }

                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        catch (CallNotPermittedException | DataAccessResourceFailureException exception) {

            SecurityContextHolder.clearContext();
            securityUnavailable(request, response, exception);

            return;
        }

        catch (AuthenticationException exception) {

            SecurityContextHolder.clearContext();
            reject(request, response, "Authentication failed.");

            return;
        }

        catch (InvalidTokenException | JwtException | IllegalArgumentException exception) {

            SecurityContextHolder.clearContext();
            reject(request, response, "Invalid or expired token.");

            return;
        }

        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message) throws IOException {

        securityExceptionHandler.commence(request, response, new BadCredentialsException(message));
    }

    private void securityUnavailable(HttpServletRequest request, HttpServletResponse response, Exception exception) throws IOException {

        securityExceptionHandler.serviceUnavailable(request, response, exception);
    }
}
