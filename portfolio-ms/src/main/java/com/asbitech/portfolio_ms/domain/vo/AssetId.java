package com.asbitech.portfolio_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class AssetId extends EntityId {

	// Constructor for creating a new ID
	public AssetId() {
		super();
	}	

	// Constructor for creating an ID from an existing string
	public AssetId(String id) {
		super(id);
	}

	@Override
	public String getPrefix() {
		return "asset-%s";
	}
}
