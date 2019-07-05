package se.skltp.components.statusprobe;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class ProcessingStatusServiceTestConfig2 {
    @Bean
    StatusConverter getStatusConverter(){
        return new StatusConverter();
    }
}
