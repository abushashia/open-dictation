package com.sitedictation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
class TransactionSavedEvent extends ApplicationEvent {

    private final Transaction transaction;

    TransactionSavedEvent(Transaction transaction, Object source) {
        super(source);
        this.transaction = transaction;
    }
}
