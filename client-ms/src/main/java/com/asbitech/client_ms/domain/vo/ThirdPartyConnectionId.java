package com.asbitech.client_ms.domain.vo;

import com.asbitech.common.domain.EntityId;

public class ThirdPartyConnectionId extends EntityId {

	@Override
	public String getPrefix() {
		return "tpc-";
	}
}