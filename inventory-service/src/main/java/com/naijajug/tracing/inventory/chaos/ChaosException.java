package com.naijajug.tracing.inventory.chaos;

public class ChaosException extends RuntimeException {

    public ChaosException() {
        super("Inventory is temporarily unavailable (chaos)");
    }
}
