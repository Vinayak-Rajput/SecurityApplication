package com.vinayak.secapp.circuitbreaker.aspect;

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
    @Around("execution(* com.vinayak.secapp.auth.service.AuthenticationService.login(..)) || " +
            "execution(* com.vinayak.secapp.auth.service.AuthenticationService.refresh(..)) || " +
            "execution(* com.vinayak.secapp.auth.service.AuthenticationService.logout(..)) || " +
            "execution(* com.vinayak.secapp.auth.service.AuthenticationService.logoutRefresh(..)) || " +
            "execution(* com.vinayak.secapp.user.service.UserService.getAllUsers(..)) || " +
            "execution(* com.vinayak.secapp.user.service.UserService.getUser(..)) || " +
            "execution(* com.vinayak.secapp.user.service.UserService.getUserByUsername(..)) || " +
            "execution(* com.vinayak.secapp.user.service.UserService.deleteUser(..)) || " +
            "execution(* com.vinayak.secapp.user.service.UserRegistrationService.register(..)) || " +
            "execution(* com.vinayak.secapp.user.service.UserRegistrationService.createAdminManagedUser(..)) || " +
            "execution(* com.vinayak.secapp.auth.service.AccessTokenAuthenticationService.loadActiveUser(..))")
    public Object protectAuthenticationDatabase(ProceedingJoinPoint invocation) throws Throwable {
        return circuitBreakerRegistry.circuitBreaker(AUTHENTICATION_DATABASE)
                .executeCheckedSupplier(invocation::proceed);
    }

    @Around("execution(* com.vinayak.secapp.audit.service.AuditLogReaderService.findRecentLogs(..))")
    public Object protectAuditLogDatabase(ProceedingJoinPoint invocation) throws Throwable {
        return circuitBreakerRegistry.circuitBreaker(AUDIT_LOG_DATABASE)
                .executeCheckedSupplier(invocation::proceed);
    }

    @Around("execution(* com.vinayak.secapp.audit.service.AuditLogWriterService.persist(..))")
    public Object protectAuditLogWrites(ProceedingJoinPoint invocation) throws Throwable {
        return circuitBreakerRegistry.circuitBreaker(AUDIT_LOG_DATABASE)
                .executeCheckedSupplier(invocation::proceed);
    }
}
