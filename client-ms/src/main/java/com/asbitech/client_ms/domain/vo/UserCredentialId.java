package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class UserCredentialId extends EntityId {
	// Constructor for creating a new ID
	public UserCredentialId() {
		super();
	}

	// Constructor for creating an ID from an existing string
	public UserCredentialId(String id) {
		super(id);
	}

	@Override
	public String getPrefix() {
		return "ucd-%s";
	}
}
