package com.profession.suggest.database.entities.auth.role;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleEnumTest {
    @Test
    void exposesOnlyCareerGuidanceRolesAsActive() {
        Set<RoleEnum> activeRoles = Stream.of(RoleEnum.values())
                .filter(RoleEnum::isActive)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                RoleEnum.ADMIN,
                RoleEnum.SPECIALIST,
                RoleEnum.PUPIL,
                RoleEnum.CURATOR
        ), activeRoles);
    }
}
