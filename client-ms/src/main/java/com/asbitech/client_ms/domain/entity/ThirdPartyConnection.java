package com.asbitech.client_ms.domain.entity;

import com.asbitech.client_ms.domain.vo.ThirdPartyConnectionId;
import com.asbitech.common.domain.Entity;
import com.asbitech.common.domain.vo.CodeType;

import lombok.Getter;
import lombok.experimental.SuperBuilder;


@Getter
@SuperBuilder
public class ThirdPartyConnection extends Entity<ThirdPartyConnectionId> {
    private String externalId;
    private CodeType provider;
    private String accessToken;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scope;
    private String authorizationUrl;
}
