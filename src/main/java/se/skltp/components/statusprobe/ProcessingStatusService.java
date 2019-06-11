package se.skltp.components.statusprobe;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.skltp.components.statusprobe.config.ServicesConfigJson;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
public class ProcessingStatusService {
    @Autowired
    private ProbeStatus probeStatus;

    @Autowired
    ServicesConfigJson servicesConfigJson;

    @GetMapping("/probe")
    @ResponseBody
    List<ProcessingStatus> getStatus() {
        List<ProcessingStatus> response = new ArrayList<>();

        probeStatus.updateStatus();

        if (!probeStatus.isProbeAvailable()) {
            response.add(generateProcessingStatus());
            return response;
        }

        Set<String> services = servicesConfigJson.getServices();
        for (String service : services) {
            ProcessingStatus processingStatus = generateProcessingStatus();
            fillServiceConfiguration(processingStatus, service);


            response.add(processingStatus);
        }

        return response;
    }

    @GetMapping("/probe/{service}")
    @ResponseBody
    ProcessingStatus getStatusOnName(String service) {
        probeStatus.updateStatus();

        if (!probeStatus.isProbeAvailable()) {
            return generateProcessingStatus();
        }
        ProcessingStatus processingStatus = generateProcessingStatus();
        fillServiceConfiguration(processingStatus, service);

        return processingStatus;
    }


    private ProcessingStatus generateProcessingStatus() {
        ProcessingStatus processingStatus = new ProcessingStatus();
        processingStatus.setProbeAvailable(probeStatus.isProbeAvailable());
        processingStatus.setProbeMessage(probeStatus.getProbeMessage());
        return processingStatus;
    }

    private void fillServiceConfiguration(ProcessingStatus processingStatus, String name){
        processingStatus.setName(name);
        processingStatus.setConnecttimeout(Integer.toString(servicesConfigJson.getConnectTimeout(name)));
        processingStatus.setResponsetimeout(Integer.toString(servicesConfigJson.getSocketTimeout(name)));
        processingStatus.setUrl((servicesConfigJson.getUrl(name)));
    }


    //Do HTTP Post on the selected resource
    void doPost(ProcessingStatus serviceToProcess, String name){
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(servicesConfigJson.getConnectTimeout(name));
        client.getHttpConnectionManager().getParams().setSoTimeout(servicesConfigJson.getSocketTimeout(name));

        log.debug("Connection timeout used for resource " + serviceToProcess.name + ": " + client.getHttpConnectionManager().getParams().getConnectionTimeout());
        log.debug("Response timeout used for resource " + serviceToProcess.name + ": " + client.getHttpConnectionManager().getParams().getSoTimeout());

        GetMethod method = new GetMethod(serviceToProcess.url);
        method.setRequestHeader("Connection","close");

        try {
            int status = client.executeMethod(method);

            log.info("Resource: $serviceToProcess.name, HTTP status code: ${status}, URL: $serviceToProcess.url");

            if(status == HttpStatus.OK.value()){
                serviceToProcess.serviceAvailable = true;
            }else{
                serviceToProcess.serviceAvailable = false;
            }

            serviceToProcess.serviceMessage = method.getResponseBodyAsString();
            //todo validateResponse

            method.releaseConnection();
        } catch (Exception e) {
            log.error ("Exception: " + e.getMessage() + " occured. Resource: " + serviceToProcess.name + ", URL " + serviceToProcess.url);
            log.error ("Exception: $e");

            serviceToProcess.serviceAvailable = false;
            serviceToProcess.serviceMessage = e.getMessage();
        }
    }

    private boolean validateResponse(String response){
        return true;
    }

}
