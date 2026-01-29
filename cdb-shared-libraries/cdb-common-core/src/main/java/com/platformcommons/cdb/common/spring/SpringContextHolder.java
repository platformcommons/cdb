package com.platformcommons.cdb.common.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Holds Spring ApplicationContext statically so non-managed classes (like JPA
 * entity listeners created by JPA provider) can access beans safely.
 */
@Component
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextHolder.context = applicationContext;
    }

    public static ApplicationContext getContext() {
        return context;
    }

    public static <T> T getBean(Class<T> beanClass) {
        ApplicationContext ctx = getContext();
        if (ctx == null) {
            return null;
        }
        ObjectProvider<T> provider = ctx.getBeanProvider(beanClass);
        return provider.getIfAvailable();
    }
}
