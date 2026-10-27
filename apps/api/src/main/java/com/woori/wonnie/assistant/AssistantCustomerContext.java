package com.woori.wonnie.assistant;

import java.util.function.Supplier;

public final class AssistantCustomerContext {
    private static final ThreadLocal<String> CURRENT = new ThreadLocal<>();

    private AssistantCustomerContext() {
    }

    public static String currentCustomerId() {
        return CURRENT.get();
    }

    static <T> T callAs(String customerId, Supplier<T> action) {
        var previous = CURRENT.get();
        CURRENT.set(customerId);
        try {
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
