package se.skltp.components.statusprobe.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Slf4j
@Configuration
public class ServicesConfigJson {
    Map<String, ServicesConfig> services = new HashMap();

    public ServicesConfigJson(@Value("${services.file}") String servicesFile, @Value("${socket.timeout.ms}") Integer defaultSocketTimeout, @Value("${connection.timeout.ms}") Integer defaultConnectionTimeout) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<ServicesConfig> servicesList = objectMapper.readValue(new File(servicesFile), new TypeReference<List<ServicesConfig>>() {
            });
            if (servicesList == null) {
                servicesList = new ArrayList<>();
            }
            for (ServicesConfig service : servicesList) {
                if (service.getConnectTimeout() == null) {
                    service.setConnectTimeout(defaultConnectionTimeout);
                }
                if (service.getSocketTimeout() == null) {
                    service.setSocketTimeout(defaultSocketTimeout);
                }
                services.put(service.getName(), service);
            }
        } catch (FileNotFoundException e) {
            log.error("Json file with services not found at " + servicesFile + ".");
        } catch (IOException e) {
            log.error("Json file with services " + servicesFile + " could not be parsed.");
        }
    }

    public Set<String> getServices() {
        return services.keySet();
    }

    public int getConnectTimeout(String name) {
            return services.get(name).getConnectTimeout();
    }

    public int getSocketTimeout(String name) {
        return services.get(name).getSocketTimeout();
    }

    public String getUrl(String name) {
        return services.get(name).getUrl();
    }
}
