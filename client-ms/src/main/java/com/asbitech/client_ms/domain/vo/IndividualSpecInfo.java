package com.asbitech.client_ms.domain.vo;

import java.time.LocalDate;
import java.util.List;

import com.asbitech.common.domain.vo.CodeType;

import lombok.*;



public record IndividualSpecInfo(
        String firstName,
        String lastName,
        IdPaper idPaperCountryOfResidence, // e.g., Social Security Number, Passport
        CodeType countryOfResidence,
        LocalDate dateOfBirth,
        LocalDate deathDate,
        List<Citizenship> citizenships
        // TODO add more fields
) {}
