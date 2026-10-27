package com.woori.wonnie.assistant;

import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;

@Component
public class AssistantResponderContextPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (!(bean instanceof AssistantResponder responder)) return bean;
        return Proxy.newProxyInstance(
                bean.getClass().getClassLoader(),
                new Class<?>[]{AssistantResponder.class},
                (proxy, method, args) -> {
                    if ((!method.getName().equals("respond") && !method.getName().equals("respondStreaming"))
                            || args == null || args.length < 3 || !(args[0] instanceof String customerId)) {
                        return invoke(method, responder, args);
                    }
                    return AssistantCustomerContext.callAs(customerId,
                            () -> invoke(method, responder, args));
                });
    }

    private static Object invoke(java.lang.reflect.Method method, Object target, Object[] args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException exception) {
            var cause = exception.getCause();
            if (cause instanceof RuntimeException runtime) throw runtime;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException(cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
