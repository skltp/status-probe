package se.skltp.components.statusprobe;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StatusConverter {
    private ObjectMapper mapper = new ObjectMapper();

    public String convert(ProcessingStatus processingStatus) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(processingStatus);

    }

    public String convert(List<ProcessingStatus> processingStatusList) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(processingStatusList);
    }



}
