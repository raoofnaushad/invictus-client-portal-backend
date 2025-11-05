package com.asbitech.document_ms.domain.entity;

import org.springframework.context.ApplicationContext;

import com.asbitech.common.domain.AggregateRoot;
import com.asbitech.document_ms.commands.UpdateDocumentCommand;
import com.asbitech.document_ms.commands.UploadDocumentCommand;
import com.asbitech.document_ms.commands.handlers.UpdateDocumentCommandHandler;
import com.asbitech.document_ms.commands.handlers.UploadDocumentCommandHandler;

public class DocumentAggregateRoot extends AggregateRoot<Document> {

   Document domainEntity;


	public DocumentAggregateRoot(ApplicationContext applicationContext, Document domainEntity) {
		super(applicationContext, domainEntity);
	}

	@Override
    protected AggregateRootBehavior initialBehavior() {
        AggregateRootBehaviorBuilder behaviorBuilder = new AggregateRootBehaviorBuilder();
        behaviorBuilder.setCommandHandler(UploadDocumentCommand.class, getHandler(UploadDocumentCommandHandler.class));
        behaviorBuilder.setCommandHandler(UpdateDocumentCommand.class, getHandler(UpdateDocumentCommandHandler.class));

        return behaviorBuilder.build();
    }

}
