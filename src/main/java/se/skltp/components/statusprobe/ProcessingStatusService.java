package se.skltp.components.statusprobe;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.HttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import se.skltp.components.statusprobe.config.ServicesConfig;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
// todo validera response

@Slf4j
@RestController
public class ProcessingStatusService {

    @Value("${probe.return.ok.string}")
    private String defaultOkReturnString;

    private HttpClient client;

    @Autowired
    private ProbeOwnStatus probeStatus;

    @Autowired
    private ServicesConfig servicesConfig;

    @Autowired
    private StatusConverter responseConverter;

    @Autowired
    private RequestSender requestSender;

    @Autowired
    private ServiceStatusChecker statusChecker;


    @GetMapping(value = "/probe")
    void getStatus(HttpServletResponse response, @RequestParam(name = "verbose", required = false) boolean verbose) {
        log.debug("getStatus() called on all resources behind probe using verbose={}", verbose);
        try {
            probeStatus.updateStatus();

            if (servicesConfig.getServices().isEmpty()) {
                ServiceStatus statusProbeOwnStatus = fillProbeStatus(new ServiceStatus());

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
                List<ServiceStatus> services = getServicesToProcess();

                if (!probeStatus.isProbeAvailable()) {
                    generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(services));
                } else {
                    for (ServiceStatus status : services) {
                        checkServiceStatus(status);
                    }

                    for (ServiceStatus status : services) {
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
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            log.error(e.getMessage());
        }
    }


    @GetMapping("/probe/{service}")
    public void getStatusByName(@PathVariable String service, HttpServletResponse response, @RequestParam(name = "verbose", required = false) boolean verbose) {
        log.debug("getStatus() called on resource name={} using verbose={}", service, verbose);

        try {
            probeStatus.updateStatus();

            ServiceStatus serviceStatus = fillProbeStatus(new ServiceStatus());
            fillServiceConfiguration(service, serviceStatus);

            if (!servicesConfig.serviceExists(service)) {
                log.error("Requested resource with name: {}, was not found in list of resources in property file", service);
                generateResponse(response, HttpServletResponse.SC_NOT_FOUND, responseConverter.convert(serviceStatus));
                return;
            }

            if (!probeStatus.isProbeAvailable()) {
                generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(serviceStatus));
                return;
            }

            checkServiceStatus(serviceStatus);

            if (!serviceStatus.isServiceAvailable()) {
                generateResponse(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, responseConverter.convert(serviceStatus));
            } else {
                if (!verbose) {
                    generateResponse(response, HttpServletResponse.SC_OK, defaultOkReturnString);
                } else {
                    generateResponse(response, HttpServletResponse.SC_OK, responseConverter.convert(serviceStatus));
                }
            }
        } catch (IOException e) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            log.error(e.getMessage());
        }
    }

    private void generateResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.getWriter().println(message);
        response.setContentType("application/json");
    }

    private List<ServiceStatus> getServicesToProcess() {
        List<ServiceStatus> serviceStatuses = new ArrayList<>();
        for (String service : servicesConfig.getServices()) {
            ServiceStatus serviceStatus = fillProbeStatus(new ServiceStatus());
            fillServiceConfiguration(service, serviceStatus);
            serviceStatuses.add(serviceStatus);
        }
        return serviceStatuses;
    }

    private ServiceStatus fillProbeStatus(ServiceStatus serviceStatus) {
        serviceStatus.setProbeAvailable(probeStatus.isProbeAvailable());
        serviceStatus.setProbeMessage(probeStatus.getProbeMessage());
        return serviceStatus;
    }

    private void fillServiceConfiguration(String name, ServiceStatus serviceStatus) {
        serviceStatus.setName(name);
        serviceStatus.setConnecttimeout(Integer.toString(servicesConfig.getConnectTimeout(name)));
        serviceStatus.setResponsetimeout(Integer.toString(servicesConfig.getSocketTimeout(name)));
        serviceStatus.setUrl((servicesConfig.getUrl(name)));
    }


    public void checkServiceStatus(ServiceStatus serviceToProcess) {
        try {
            String url = servicesConfig.getUrl(serviceToProcess.getName());
            int connectionTimeout = servicesConfig.getConnectTimeout(serviceToProcess.getName());
            int responseTimeout = servicesConfig.getSocketTimeout(serviceToProcess.getName());

            ServiceResponse response = requestSender.sendStatusRequest(url, connectionTimeout, responseTimeout);
            log.info("Resource: {}, HTTP status code: {}, URL: {}", serviceToProcess.getName(), response.getStatusCode(), url);

            Set<String> missing = statusChecker.checkStatus(response.getMessage(), servicesConfig.getStatusValues(serviceToProcess.getName()));

            if (response.getStatusCode() == HttpStatus.OK.value()) {
                serviceToProcess.setServiceAvailable(true);
            } else {
                serviceToProcess.setServiceAvailable(false);
            }

            if (missing.isEmpty()) {
                serviceToProcess.setServiceMessage(response.getMessage());
            } else {
                serviceToProcess.setServiceAvailable(false);
                serviceToProcess.setServiceMessage("Missing values: " + Arrays.toString(missing.toArray()) + " In server response: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("Exception: " + e.getMessage() + " occured. Resource: " + serviceToProcess.getName() + ", URL " + serviceToProcess.getUrl());

            serviceToProcess.setServiceAvailable(false);
            serviceToProcess.setServiceMessage(e.getMessage());
        }
    }
}
