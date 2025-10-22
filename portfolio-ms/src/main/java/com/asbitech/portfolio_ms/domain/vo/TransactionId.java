package com.asbitech.portfolio_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class TransactionId extends EntityId {

	// Constructor for creating a new ID
	public TransactionId() {
		super();
	}	

	// Constructor for creating an ID from an existing string
	public TransactionId(String id) {
		super(id);
	}

	@Override
	public String getPrefix() {
		return "tra-%s";
	}
}
