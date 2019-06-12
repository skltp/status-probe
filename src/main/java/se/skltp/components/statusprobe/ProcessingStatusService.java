package se.skltp.components.statusprobe;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.skltp.components.statusprobe.config.ServicesConfig;
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
    ServicesConfig servicesConfig;

    @GetMapping("/probe")
    @ResponseBody
    List<ProcessingStatus> getStatus() {
        List<ProcessingStatus> response = new ArrayList<>();

        probeStatus.updateStatus();

        if (!probeStatus.isProbeAvailable()) {
            response.add(generateProcessingStatus());
            return response;
        }

        Set<String> services = servicesConfig.getServices();
        for (String service : services) {
            ProcessingStatus processingStatus = generateProcessingStatus();
            fillServiceConfiguration(service, processingStatus);
            checkServiceStatus(service, processingStatus);
            response.add(processingStatus);
        }

        return response;
    }

    @GetMapping("/probe/{service}")
    @ResponseBody
    ProcessingStatus getStatusOnName(@PathVariable String service) {
        probeStatus.updateStatus();

        ProcessingStatus processingStatus = generateProcessingStatus();
        if (!probeStatus.isProbeAvailable()) {
            return processingStatus;
        }

        fillServiceConfiguration(service, processingStatus);
        checkServiceStatus(service, processingStatus);
        return processingStatus;
    }


    private ProcessingStatus generateProcessingStatus() {
        ProcessingStatus processingStatus = new ProcessingStatus();
        processingStatus.setProbeAvailable(probeStatus.isProbeAvailable());
        processingStatus.setProbeMessage(probeStatus.getProbeMessage());
        return processingStatus;
    }

    private void fillServiceConfiguration(String name, ProcessingStatus processingStatus) {
        processingStatus.setName(name);
        processingStatus.setConnecttimeout(Integer.toString(servicesConfig.getConnectTimeout(name)));
        processingStatus.setResponsetimeout(Integer.toString(servicesConfig.getSocketTimeout(name)));
        processingStatus.setUrl((servicesConfig.getUrl(name)));
    }


    //Do HTTP Get on the selected resource
    private void checkServiceStatus(String name, ProcessingStatus serviceToProcess) {
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(servicesConfig.getConnectTimeout(name));
        client.getHttpConnectionManager().getParams().setSoTimeout(servicesConfig.getSocketTimeout(name));

        log.debug("Connection timeout used for resource " + serviceToProcess.getName() + ": " + client.getHttpConnectionManager().getParams().getConnectionTimeout());
        log.debug("Response timeout used for resource " + serviceToProcess.getName() + ": " + client.getHttpConnectionManager().getParams().getSoTimeout());

        GetMethod method = new GetMethod(servicesConfig.getUrl(name));
        method.setRequestHeader("Connection", "close");

        try {
            int status = client.executeMethod(method);

            log.info("Resource: {}, HTTP status code: {}, URL: {}", name, status, servicesConfig.getUrl(name));

            if (status == HttpStatus.OK.value()) {
                serviceToProcess.setServiceAvailable(true);
            } else {
                serviceToProcess.setServiceAvailable(false);
            }

            serviceToProcess.setServiceMessage(method.getResponseBodyAsString());
            //todo validateResponse

            method.releaseConnection();
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage() + " occured. Resource: " + serviceToProcess.getName() + ", URL " + serviceToProcess.getUrl());
            log.error("Exception: $e");

            serviceToProcess.setServiceAvailable(false);
            serviceToProcess.setServiceMessage(e.getMessage());
        }
    }


    private boolean validateResponse(String response) {
        return true;
    }

}
