package se.skltp.components.statusprobe;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;

@TestConfiguration
public class ProcessingStatusServiceTestConfig {
    @Bean
    StatusConverter getStatusConverter(){
        return new StatusConverter();
    }

    @Bean
    RequestSender getRequestSender(){
        return new RequestSender();
    }
}
