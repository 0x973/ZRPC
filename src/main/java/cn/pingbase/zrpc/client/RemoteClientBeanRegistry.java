package cn.pingbase.zrpc.client;

import cn.pingbase.zrpc.annotation.ZRPCPackageScan;
import cn.pingbase.zrpc.annotation.ZRPCRemoteClient;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author: Zak
 * @date 2022/08/18 11:40
 * @description: TODO
 */
@Component
public class RemoteClientBeanRegistry implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private static ApplicationContext context;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        List<Reflections> reflectionsList = new ArrayList<>();

        Object packageScanAnnotation = this.getZRPCPackageScanAnnotationClass();
        if (packageScanAnnotation != null) {
            Class<?> aClass = AopUtils.getTargetClass(packageScanAnnotation);
            List<Reflections> reflections = Arrays.stream(aClass.getDeclaredAnnotation(ZRPCPackageScan.class).basePackages()).map(Reflections::new).collect(Collectors.toList());
            reflectionsList.addAll(reflections);
        }

        if (reflectionsList.isEmpty()) {
            List<URL> urls = ClasspathHelper.forJavaClassPath().stream()
                    .filter(p -> !p.toString().endsWith(".jar"))
                    .collect(Collectors.toList());
            reflectionsList.add(new Reflections(urls));
        }

        reflectionsList.forEach(r -> {
            Set<Class<?>> clazzSet = r.getTypesAnnotatedWith(ZRPCRemoteClient.class);
            clazzSet.stream().filter(Class::isInterface).forEach(clazz -> this.registerBean(registry, clazz));
        });
    }

    private void registerBean(BeanDefinitionRegistry registry, Class<?> clazz) {
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz);
        GenericBeanDefinition definition = (GenericBeanDefinition) builder.getRawBeanDefinition();
        definition.getConstructorArgumentValues().addGenericArgumentValue(clazz);
        definition.setBeanClass(RemoteClientFactory.class);
        definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);
        registry.registerBeanDefinition(clazz.getSimpleName(), definition);
    }

    @Override
    public void postProcessBeanFactory(@NotNull ConfigurableListableBeanFactory beanFactory) {
    }

    @Override
    public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {
        RemoteClientBeanRegistry.context = applicationContext;
    }

    private Object getZRPCPackageScanAnnotationClass() {
        Map<String, Object> beansWithAnnotation = RemoteClientBeanRegistry.context.getBeansWithAnnotation(ZRPCPackageScan.class);
        if (beansWithAnnotation.size() > 0) {
            return beansWithAnnotation.entrySet().stream().findFirst().get().getValue();
        }
        return null;
    }
}