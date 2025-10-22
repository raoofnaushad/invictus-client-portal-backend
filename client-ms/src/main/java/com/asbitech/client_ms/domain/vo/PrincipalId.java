package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class PrincipalId extends EntityId {

	// Constructor for creating a new ID
	public PrincipalId() {
		super();
	}	

	// Constructor for creating an ID from an existing string
	public PrincipalId(String id) {
		super(id);
	}

	@Override
	public String getPrefix() {
		return "pcl-%s";
	}
}