package se.skltp.components.statusprobe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@Slf4j
public class ApplicationListenerBean implements ApplicationListener {


    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
//            final Environment env = ((ContextRefreshedEvent) event).getApplicationContext().getEnvironment();
//            log.info("====== Environment and configuration ======");
//            log.info("Active profiles: {}", Arrays.toString(env.getActiveProfiles()));
//            final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
//            StreamSupport.stream(sources.spliterator(), false)
//                    .filter(ps -> ps instanceof EnumerablePropertySource)
//                    .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
//                    .flatMap(Arrays::stream)
//                    .distinct()
//                    .filter(prop -> !(prop.contains("credentials") || prop.contains("password")))
//                    .forEach(prop -> log.info("{}: {}", prop, env.getProperty(prop)));
//            log.info("===========================================");
        }
    }

    private static void printMap (Map<?, ?> map) {
        map.entrySet()
                .stream()
                .forEach(e -> System.out.println(e.getKey() + " = " + e.getValue()));

    }
}
