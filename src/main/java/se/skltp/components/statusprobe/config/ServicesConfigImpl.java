package se.skltp.components.statusprobe.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import se.skltp.components.statusprobe.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

@Slf4j
@Configuration
public class ServicesConfigImpl implements ServicesConfig {

    private Map<String, Service> services;

    public ServicesConfigImpl(@Value("${services.file}") String servicesFile, @Value("${socket.timeout.ms}") Integer defaultSocketTimeout, @Value("${connection.timeout.ms}") Integer defaultConnectionTimeout) {
        services = new HashMap<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<Service> servicesList = objectMapper.readValue(new File(servicesFile), new TypeReference<List<Service>>() {
            });
            log.info("############### Service för status check: #########################");
            for (Service service : servicesList) {
                if (service.getConnectTimeout() == null) {
                    service.setConnectTimeout(defaultConnectionTimeout);
                }
                if (service.getSocketTimeout() == null) {
                    service.setSocketTimeout(defaultSocketTimeout);
                }

                log.info("Service namn:{}, url:{}, connectTimeout:{}, socketTimeout:{} ", service.getName(), service.getUrl(), service.getConnectTimeout(), service.getSocketTimeout());
                log.info("Status values: " + service.getStatusValues());
                services.put(service.getName(), service);
            }
            log.info("####################################################################");
        } catch (FileNotFoundException e) {
            log.error("Json file with services not found at " + servicesFile + ".");
        } catch (IOException e) {
            log.error("Json file with services " + servicesFile + " could not be parsed.");
        }
    }

    public Set<String> getServices() throws StartupException {
        if (services.isEmpty()) throw new StartupException("SatusProbe fail during start.");
        return services.keySet();
    }

    public boolean serviceExists(String name) throws StartupException {
        if (services.isEmpty()) throw new StartupException("SatusProbe fail during start.");
        return services.containsKey(name);
    }

    public int getConnectTimeout(String name) throws StartupException {
        if (serviceExists(name))
            return services.get(name).getConnectTimeout();
        else return 0;
    }

    public int getSocketTimeout(String name) throws StartupException {
        if (serviceExists(name))
            return services.get(name).getSocketTimeout();
        else return 0;
    }

    public String getUrl(String name) throws StartupException {
        if (serviceExists(name))
            return services.get(name).getUrl();
        else return null;
    }

    public List<String> getStatusValues(String name) throws StartupException {
        if (serviceExists(name))
            return services.get(name).getStatusValues();
        else return new ArrayList<>();
    }
}
