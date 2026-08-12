package com.profession.suggest.database.entities.auth.role;

public enum RoleEnum {
    ADMIN,
    PUPIL,
    SPECIALIST,
    CURATOR,

    /**
     * Legacy values are kept only so copied databases containing historical
     * assignments can still be read. They are not accepted by active access
     * checks or by new account registration.
     */
    @Deprecated
    HR,
    @Deprecated
    APPLICANT,
    @Deprecated
    TEACHER,
    @Deprecated
    DIRECTOR,
    @Deprecated
    EMPLOYEE,

    ;

    public boolean isActive() {
        return this == ADMIN || this == SPECIALIST || this == PUPIL || this == CURATOR;
    }
}
