package cn.pingbase.zrpc.server;

import cn.pingbase.zrpc.annotation.ZRPCPackageScan;
import cn.pingbase.zrpc.annotation.ZRPCRemoteService;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Zak
 * @date 2022/08/19 14:32
 * @description: TODO
 */
@Slf4j
@Component
public class RemoteServiceBeanRegistry implements ApplicationContextAware {

    private static ApplicationContext context;

    @PostConstruct
    public void remoteServiceBeanRegistry() {
        try {
            List<Reflections> reflectionsList = new ArrayList<>(this.getZRPCPackageScanReflections());
            if (reflectionsList.isEmpty()) {
                List<URL> urls = ClasspathHelper.forJavaClassPath().stream()
                        .filter(p -> !p.toString().endsWith(".jar"))
                        .collect(Collectors.toList());
                reflectionsList.add(new Reflections(urls));
            }

            List<Set<Class<?>>> annotatedClasses = reflectionsList.stream().map(r -> r.getTypesAnnotatedWith(ZRPCRemoteService.class, true))
                    .collect(Collectors.toList());
            for (Set<Class<?>> clazzSet : annotatedClasses) {
                clazzSet.forEach(RemoteServiceBeanRegistry::registryClassToBeanStore);
            }
        } catch (Exception e) {
            log.warn("Remote server bean registry error.", e);
        }
    }

    private static void registryClassToBeanStore(Class<?> clazz) {
        ZRPCRemoteService remoteServiceAnnotation = clazz.getDeclaredAnnotation(ZRPCRemoteService.class);
        String serviceIdentifier = remoteServiceAnnotation.serviceIdentifier();
        if (RemoteServiceBeanStore.containsServiceBean(serviceIdentifier)) {
            log.warn("The same serviceIdentifier for serviceBean already exists.");
            return;
        }

        Class<?> serviceImplClass = remoteServiceAnnotation.serviceImplClass();
        Object bean;
        if (!Class.class.equals(serviceImplClass)) {
            // 用户配置了实现类, 寻找对应的具体实现类的bean
            bean = RemoteServiceBeanRegistry.context.getBean(serviceImplClass);
        } else {
            // 用户未配置具体实现类, 根据当前注解所在的class获取bean
            bean = RemoteServiceBeanRegistry.context.getBean(clazz);
        }

        RemoteServiceBeanStore.putServiceBean(serviceIdentifier, bean);
    }

    private List<Reflections> getZRPCPackageScanReflections() {
        Object packageScanAnnotation = this.getZRPCPackageScanAnnotationClass();
        if (packageScanAnnotation == null) {
            return new ArrayList<>();
        }

        Class<?> aClass = AopUtils.getTargetClass(packageScanAnnotation);
        return Arrays.stream(aClass.getDeclaredAnnotation(ZRPCPackageScan.class).basePackages())
                .map(Reflections::new)
                .collect(Collectors.toList());
    }

    private Object getZRPCPackageScanAnnotationClass() {
        Map<String, Object> beansWithAnnotation = RemoteServiceBeanRegistry.context.getBeansWithAnnotation(ZRPCPackageScan.class);
        if (beansWithAnnotation.size() > 0) {
            return beansWithAnnotation.entrySet().stream().findFirst().get().getValue();
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        RemoteServiceBeanRegistry.context = applicationContext;
    }
}
