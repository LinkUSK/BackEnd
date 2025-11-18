package com.example.demo.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SchoolEmailValidator {

    private final String schoolDomain;

    public SchoolEmailValidator(@Value("${app.school-domain:skuniv.ac.kr}") String schoolDomain) {
        this.schoolDomain = schoolDomain;
    }

    public boolean isSchoolEmail(String email) {
        return email != null && email.toLowerCase().endsWith("@"+schoolDomain.toLowerCase());
    }
}
