package com.hdfclife.capstoneone.circuitbreaker.aspect;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component("applicationCircuitBreakerAspect")
@RequiredArgsConstructor
public class CircuitBreakerAspect {

    private static final String AUTHENTICATION_DATABASE = "authenticationDatabase";
    private static final String AUDIT_LOG_DATABASE = "auditLogDatabase";

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    // Keep database protection centralized while applying separate breaker state to each datasource.
    @Around("execution(* com.hdfclife.capstoneone.auth.service.AuthService.login(..)) || " +
            "execution(* com.hdfclife.capstoneone.auth.service.AuthService.refresh(..)) || " +
            "execution(* com.hdfclife.capstoneone.auth.service.AuthService.logout(..)) || " +
            "execution(* com.hdfclife.capstoneone.auth.service.AuthService.logoutRefresh(..)) || " +
            "execution(* com.hdfclife.capstoneone.user.service.UserService.getAllUsers(..)) || " +
            "execution(* com.hdfclife.capstoneone.user.service.UserService.getUser(..)) || " +
            "execution(* com.hdfclife.capstoneone.user.service.UserService.getUserByUsername(..)) || " +
            "execution(* com.hdfclife.capstoneone.user.service.UserService.deleteUser(..)) || " +
            "execution(* com.hdfclife.capstoneone.user.service.UserRegistrationService.register(..)) || " +
            "execution(* com.hdfclife.capstoneone.user.service.UserRegistrationService.createAdminManagedUser(..)) || " +
            "execution(* com.hdfclife.capstoneone.auth.service.AccessTokenAuthenticationService.loadActiveUser(..))")
    public Object protectAuthenticationDatabase(ProceedingJoinPoint invocation) throws Throwable {
        return circuitBreakerRegistry.circuitBreaker(AUTHENTICATION_DATABASE)
                .executeCheckedSupplier(invocation::proceed);
    }

    @Around("execution(* com.hdfclife.capstoneone.audit.service.AuditLogReaderService.findRecentLogs(..))")
    public Object protectAuditLogDatabase(ProceedingJoinPoint invocation) throws Throwable {
        return circuitBreakerRegistry.circuitBreaker(AUDIT_LOG_DATABASE)
                .executeCheckedSupplier(invocation::proceed);
    }

    @Around("execution(* com.hdfclife.capstoneone.audit.service.AuditLogWriterService.persist(..))")
    public Object protectAuditLogWrites(ProceedingJoinPoint invocation) throws Throwable {
        return circuitBreakerRegistry.circuitBreaker(AUDIT_LOG_DATABASE)
                .executeCheckedSupplier(invocation::proceed);
    }
}
