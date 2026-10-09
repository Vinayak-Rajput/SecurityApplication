package com.hdfclife.capstoneone.ratelimit.aspect;

import com.hdfclife.capstoneone.auth.dto.LoginRequest;
import com.hdfclife.capstoneone.ratelimit.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitingAspect {

    private final RateLimitService rateLimitService;

    @Around("execution(* com.hdfclife.capstoneone.auth.service.AuthService.login(..))")
    public Object limitLoginAttempts(ProceedingJoinPoint invocation) throws Throwable {
        Object[] arguments = invocation.getArgs();
        if (arguments.length != 2 || !(arguments[0] instanceof LoginRequest request)
                || !(arguments[1] instanceof String clientIp)) {
            throw new IllegalStateException("AuthService.login must accept a LoginRequest and client IP");
        }

        return rateLimitService.limitLogin(request.getUsername(), clientIp, invocation::proceed);
    }

    @Around("execution(* com.hdfclife.capstoneone.auth.controller.AuthController.refresh(..)) || " +
            "execution(* com.hdfclife.capstoneone.auth.controller.AuthController.logout(..)) || " +
            "execution(* com.hdfclife.capstoneone.auth.controller.AuthController.logoutWithRefreshToken(..))")
    public Object limitTokenEndpointRequests(ProceedingJoinPoint invocation) throws Throwable {
        rateLimitService.limitTokenEndpoint(currentRequest().getRemoteAddr());
        return invocation.proceed();
    }

    @Around("execution(* com.hdfclife.capstoneone.auth.controller..*(..)) || " +
            "execution(* com.hdfclife.capstoneone.user.controller..*(..)) || " +
            "execution(* com.hdfclife.capstoneone.audit.controller..*(..))")
    public Object limitAuthenticatedApiRequests(ProceedingJoinPoint invocation) throws Throwable {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return invocation.proceed();
        }

        HttpServletRequest request = currentRequest();
        rateLimitService.limitAuthenticatedRequest(
                authentication.getName(),
                request.getRemoteAddr(),
                isSensitiveRoute(request.getRequestURI()));
        return invocation.proceed();
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return servletAttributes.getRequest();
        }
        throw new IllegalStateException("An HTTP request is required for controller rate limiting");
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static boolean isSensitiveRoute(String requestUri) {
        return requestUri.equals("/users") || requestUri.startsWith("/users/") || requestUri.startsWith("/audit/");
    }
}
