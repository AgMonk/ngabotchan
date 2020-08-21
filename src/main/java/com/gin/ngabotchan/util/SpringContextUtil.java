package com.gin.ngabotchan.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring工具类,获取Spring上下文对象等
 *
 * @author Mr.Qu
 * @since 2020/1/9 16:26
 */
@Component
public class SpringContextUtil implements ApplicationContextAware {
 
    private static ApplicationContext applicationContext = null;
 
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        if(com.gin.ngabotchan.util.SpringContextUtil.applicationContext == null){
           com.gin.ngabotchan.util.SpringContextUtil.applicationContext  = applicationContext;
        }
    }
 
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }
 
    public static Object getBean(String name){
        return getApplicationContext().getBean(name);
    }
 
    public static <T> T getBean(Class<T> clazz){
        return getApplicationContext().getBean(clazz);
    }
 
    public static <T> T getBean(String name,Class<T> clazz){
        return getApplicationContext().getBean(name, clazz);
    }
}