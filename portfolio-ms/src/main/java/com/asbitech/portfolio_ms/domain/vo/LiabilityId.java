package com.asbitech.portfolio_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class LiabilityId extends EntityId {

	// Constructor for creating a new ID
	public LiabilityId() {
		super();
	}	

	// Constructor for creating an ID from an existing string
	public LiabilityId(String id) {
		super(id);
	}

	@Override
	public String getPrefix() {
		return "lia-%s";
	}
}

