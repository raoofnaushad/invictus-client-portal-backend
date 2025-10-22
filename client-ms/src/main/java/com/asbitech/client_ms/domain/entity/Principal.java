package com.asbitech.client_ms.domain.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.asbitech.client_ms.domain.vo.BusinessSpecInfo;
import com.asbitech.client_ms.domain.vo.Email;
import com.asbitech.client_ms.domain.vo.IndividualSpecInfo;
import com.asbitech.client_ms.domain.vo.PhoneNumber;
import com.asbitech.client_ms.domain.vo.PrincipalId;
import com.asbitech.client_ms.domain.vo.PrincipalType;
import com.asbitech.common.domain.Entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
@ToString
public class Principal extends Entity<PrincipalId> {
    BusinessSpecInfo businessSpecInfo;
    String alias;
    PrincipalType principalType;
    IndividualSpecInfo individualSpecInfo;
    List<PhoneNumber> phoneNumbers;
    List<Email> emails;
    List<ThirdPartyConnection> thirdPartyConnections;
    UserCredential userCredential;
    Boolean isNew;
}
