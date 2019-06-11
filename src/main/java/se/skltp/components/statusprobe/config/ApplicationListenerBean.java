package se.skltp.components.statusprobe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ApplicationListenerBean implements ApplicationListener {

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            ApplicationContext applicationContext = ((ContextRefreshedEvent) event).getApplicationContext();

            log.info("############### Properties in mule-probe-config ####################");

//            for(String key : rrb.properties.stringPropertyNames()) {
//                log.info("Property: $key, value: " + rrb.getString(key));
//            }
            log.info("####################################################################");
        }
    }
}
