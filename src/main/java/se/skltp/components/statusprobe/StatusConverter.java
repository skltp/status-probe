package se.skltp.components.statusprobe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatusConverter {
    private ObjectMapper mapper = new ObjectMapper();

    public String convert(ServiceStatus serviceStatus) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(serviceStatus);

    }

    public String convert(List<ServiceStatus> serviceStatusList) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(serviceStatusList);
    }
}
