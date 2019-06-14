package se.skltp.components.statusprobe;


import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import se.skltp.components.statusprobe.config.ServicesConfig;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// todo Hantera exceptions på bra sett
// todo kanske returnera contentType=application/json om det behövs
// todo kanske det är möligt göra algoritm enklare(eller mer generaliserad). Med det behövs att veta hur LB jobbar med response
// todo validera response
// todo tester

@Slf4j
@RestController
public class ProcessingStatusService {

    @Value("probe.return.ok.string")
    String defaultOkReturnString;

    @Autowired
    private ProbeOwnStatus probeStatus;

    @Autowired
    ServicesConfig servicesConfig;

    @Autowired
    StatusConverter responseConverter;

    @GetMapping("/probe")
    void getStatus(HttpServletResponse response, @PathVariable(name = "verbose", required = false) boolean verbose) throws IOException {
        log.debug("getStatus() called on all resources behind probe using verbose={}", verbose);

        probeStatus.updateStatus();

        if (servicesConfig.getServices().isEmpty()) {
            ProcessingStatus statusProbeOwnStatus = generateProcessingStatus();

            if (!probeStatus.isProbeAvailable()) {
                generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(statusProbeOwnStatus));
            } else {
                if (!verbose) {
                    generateResponse(response, HttpServletResponse.SC_OK, defaultOkReturnString);
                } else {
                    generateResponse(response, HttpServletResponse.SC_OK, responseConverter.convert(statusProbeOwnStatus));
                }
            }
        } else {
            List<ProcessingStatus> services = getServicesToProcess();

            if (!probeStatus.isProbeAvailable()) {
                for (ProcessingStatus status : services) {
                    status.setProbeAvailable(probeStatus.isProbeAvailable());
                    status.setProbeMessage(probeStatus.getProbeMessage());
                }
                generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(services));
            } else {
                for (ProcessingStatus status : services) {
                    checkServiceStatus(status.getName(), status);
                }

                for (ProcessingStatus status : services) {
                    if (!status.isServiceAvailable()) {
                        generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(services));
                        return;
                    }
                }

                if (!verbose) {
                    generateResponse(response, HttpServletResponse.SC_OK, defaultOkReturnString);
                } else {
                    generateResponse(response, HttpServletResponse.SC_OK, responseConverter.convert(services));
                }
            }
        }
    }

    private void generateResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().println(message);
    }

    private List<ProcessingStatus> getServicesToProcess() {
        List<ProcessingStatus> responseContent = new ArrayList<>();
        for (String service : servicesConfig.getServices()) {
            ProcessingStatus processingStatus = generateProcessingStatus();
            fillServiceConfiguration(service, processingStatus);
            responseContent.add(processingStatus);
        }
        return responseContent;
    }


    @GetMapping("/probe/{service}")
    public void getStatusByName(@PathVariable String service, HttpServletResponse response, @PathVariable(name = "verbose", required = false) boolean verbose) throws IOException {
        log.debug("getStatus() called on resource name={} using verbose={}", service, verbose);

        probeStatus.updateStatus();

        ProcessingStatus processingStatus = generateProcessingStatus();

        if (!servicesConfig.serviceExists(service)) {
            log.error("Requested resource with name: $name, was not found in list of resources in property file");
            generateResponse(response, HttpServletResponse.SC_NOT_FOUND, responseConverter.convert(processingStatus));
            return;
        }


        if (!probeStatus.isProbeAvailable()) {
            generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(processingStatus));
            return;
        }

        fillServiceConfiguration(service, processingStatus);
        checkServiceStatus(service, processingStatus);

        if (!processingStatus.isServiceAvailable()) {
            generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(processingStatus));
            return;
        }

        if (!verbose) {
            generateResponse(response, HttpServletResponse.SC_OK, defaultOkReturnString);
        } else {
            generateResponse(response, HttpServletResponse.SC_OK, responseConverter.convert(processingStatus));
        }
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
            log.info("Resource: {}, HTTP status code: {}, URL: {}", name, status, serviceToProcess.getUrl());

            String responseMessage = method.getResponseBodyAsString();
            serviceToProcess.setServiceMessage(responseMessage);

            boolean validatingStatus = validateResponse(method.getResponseBodyAsString());


            if (status == HttpStatus.OK.value() && validatingStatus) {
                serviceToProcess.setServiceAvailable(true);
            } else {
                serviceToProcess.setServiceAvailable(false);
            }

            method.releaseConnection();
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage() + " occured. Resource: " + serviceToProcess.getName() + ", URL " + serviceToProcess.getUrl());
            log.error("Exception: $e");

            serviceToProcess.setServiceAvailable(false);
            serviceToProcess.setServiceMessage(e.getMessage());
        }
    }


    private boolean validateResponse(String response) {
        //todo
        return true;
    }

}
