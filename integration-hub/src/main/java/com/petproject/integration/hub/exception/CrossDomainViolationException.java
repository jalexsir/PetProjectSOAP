package com.petproject.integration.hub.exception;

import java.util.List;

/**
 * Кидається CrossDomainGuardProcessor, коли повідомлення не пройшло перевірку
 * на межі довіри (Cross Domain Solution) — або не відповідає контракту (JSON Schema),
 * або порушує бізнес-правило пропуску даних між доменами.
 */
public class CrossDomainViolationException extends RuntimeException {

    private final List<String> reasons;

    public CrossDomainViolationException(List<String> reasons) {
        super("Cross-Domain Solution guard rejected the message: " + reasons);
        this.reasons = reasons;
    }

    public List<String> getReasons() {
        return reasons;
    }
}
