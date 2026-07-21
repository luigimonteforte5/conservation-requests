package com.luigimonteforte.conservationrequests.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class RequestCompletedListener {
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onRequestCompletedEvent(RequestCompletedEvent event) {
		log.info("Request {} completed and committed (producerId={}, externalId={}, from {})", event.requestId(),
						event.producerId(), event.externalId(), event.previousStatus());
	}
}
