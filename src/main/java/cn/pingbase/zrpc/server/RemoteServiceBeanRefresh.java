package cn.pingbase.zrpc.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

/**
 * @author: Zak
 * @date 2022/08/31 17:44
 * @description: Listen for context refresh events.
 */
@Slf4j
@Component
public class RemoteServiceBeanRefresh implements ApplicationListener<ContextRefreshedEvent> {

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        RemoteServiceBeanStore.getCopyMap().forEach((serviceIdentifier, bean) -> {
            Object newBean = applicationContext.getBean(bean.getClass());
            this.refreshBean(serviceIdentifier, newBean);
            log.info(String.format("Remote service bean refresh successfully. %s -> %s", serviceIdentifier,
                    newBean.getClass().getName()));
        });
    }

    private void refreshBean(String serviceIdentifier, Object newBean) {
        RemoteServiceBeanStore.put(serviceIdentifier, newBean);
    }
}
