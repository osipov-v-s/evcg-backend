package com.profession.suggest.configuration.security.aspect;

import com.profession.suggest.configuration.security.annotation.HasRole;
import com.profession.suggest.database.entities.auth.Account;
import com.profession.suggest.database.entities.auth.role.Role;
import com.profession.suggest.database.entities.auth.role.RoleEnum;
import com.profession.suggest.database.services.auth.AccountService;
import com.profession.suggest.dto.auth.RoleDTO;
import com.profession.suggest.services.jwt.JWTService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Aspect
@Component
@Slf4j
public class RoleAspect {
    @Autowired
    private AccountService accountService;
    @Autowired
    private JWTService jwtService;

    @Before("@annotation(hasRole)")
    public void checkRole(JoinPoint joinPoint, HasRole hasRole) throws Throwable {
        try{
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getCredentials() == null) {
                log.error("No authenticated user found");
                throw new AccessDeniedException("No user found");
            }
            String jwtToken = (String) authentication.getCredentials();
            Long accountId = Long.valueOf(jwtService.extractSubject(jwtToken));
            Set<RoleEnum> accountRoleNames = accountService.getRolesByAccount(accountId).stream()
                    .map(Role::getName)
                    .filter(RoleEnum::isActive)
                    .collect(Collectors.toSet());
            boolean hasRequiredRole = Arrays.stream(hasRole.value())
                    .anyMatch(accountRoleNames::contains);
            if (!hasRequiredRole) {
                Account account = accountService.getAccountById(accountId);
                log.warn("Access denied for {}. Required roles: {}, but has {}", account.getEmail(), Arrays.toString(hasRole.value()), accountRoleNames);
                throw new AccessDeniedException("access denied, Required roles: " + Arrays.toString(hasRole.value()));
            }
        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred during role validation", e);
            throw new AccessDeniedException("access denied due internal error");
        }
    }
}
