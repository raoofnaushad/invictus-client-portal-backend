package com.asbitech.portfolio_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class PortfolioId  extends EntityId {

	// Constructor for creating a new ID
	public PortfolioId() {
		super();
	}	

	// Constructor for creating an ID from an existing string
	public PortfolioId(String id) {
		super(id);
	}

	@Override
	public String getPrefix() {
		return "pfl-%s";
	}
}
