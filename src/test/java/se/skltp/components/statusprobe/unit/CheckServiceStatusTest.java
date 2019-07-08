package se.skltp.components.statusprobe.unit;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import se.skltp.components.statusprobe.ProcessingStatusService;
import se.skltp.components.statusprobe.RequestSender;
import se.skltp.components.statusprobe.ServiceResponse;
import se.skltp.components.statusprobe.ServiceStatus;
import se.skltp.components.statusprobe.config.ServicesConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:application.properties")
@SpringBootTest(classes = TestConfig.class)
public class CheckServiceStatusTest {

    @Autowired
    ProcessingStatusService processingStatusService;

    @MockBean
    private RequestSender requestSender;

    @MockBean
    ServicesConfig servicesConfig;

    private String SERVICE_1 = "Service1";
    private String SERVICE_2 = "Service2";

    private String URL_1 = "http://url1";
    private String URL_2 = "http://url2";

    private String response = "" +
            "Name\"vp-services-camel\"" +
            "Version\"1.0.0-SNAPSHOT\"" +
            "BuildTime\"2019-07-01T08:57:11.518Z\"" +
            "ServiceStatus\"Started\"" +
            "Uptime\"3 days 12 hours\"" +
            "ManagementName\"vp-services\"" +
            "JavaVersion\"1.8.0_161\"" +
            "CamelVersion\"2.24.0\"" +
            "TakCacheInitialized\"true\"" +
            "TakCacheResetInfo\"Date:2019-07-04T16:14 Status:REFRESH_OK vagval:565 behorigheter:1033\"" +
            "HsaCacheInitialized\"true\"" +
            "HsaCacheResetInfo\"Date:2019-07-03T08:42 Status:true oldNum:0 newNum:43285\"" +
            "JvmTotalMemory\"701 mB\"" +
            "JvmFreeMemory\"246 mB\"" +
            "JvmUsedMemory\"454 mB\"" +
            "JvmMaxMemory\"910 mB\"" +
            "Endpoints" +
            "0\"http://0.0.0.0:24000/resethsacache\"" +
            "1\"http://0.0.0.0:23000/resetcache\"" +
            "2\"https://0.0.0.0:20000/vp\"" +
            "3\"http://0.0.0.0:8080/vp\"" +
            "4\"http://0.0.0.0:8080/status\"" ;

    @Before
    public void before() {
        HashSet<String> hashSet = new HashSet<>();
        hashSet.add(SERVICE_1);
        hashSet.add(SERVICE_2);
        Mockito.when(servicesConfig.getServices()).thenReturn(hashSet);

        Mockito.when(servicesConfig.getUrl(SERVICE_1)).thenReturn(URL_1);
        Mockito.when(servicesConfig.getUrl(SERVICE_2)).thenReturn(URL_2);

        Mockito.when(servicesConfig.getConnectTimeout(SERVICE_1)).thenReturn(1000);
        Mockito.when(servicesConfig.getConnectTimeout(SERVICE_2)).thenReturn(1000);

        Mockito.when(servicesConfig.getSocketTimeout(SERVICE_1)).thenReturn(1000);
        Mockito.when(servicesConfig.getSocketTimeout(SERVICE_2)).thenReturn(1000);
    }


    @Test
    public void test_ok() throws IOException {
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, response));
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(new ArrayList<>());

        ServiceStatus serviceStatus = new ServiceStatus();
        serviceStatus.setName(SERVICE_1);

        processingStatusService.checkServiceStatus(serviceStatus);

        assertTrue(serviceStatus.isServiceAvailable());
        assertEquals(response, serviceStatus.getServiceMessage());
    }

    @Test
    public void test_ok_with_status_values() throws IOException {
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, response));
        List statusValues = new ArrayList<>();
        String value1 = "ServiceStatus\"Started\"";
        statusValues.add(value1);
        String value2 = "TakCacheInitialized\"true\"";
        statusValues.add(value2);
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(statusValues);

        ServiceStatus serviceStatus = new ServiceStatus();
        serviceStatus.setName(SERVICE_1);

        processingStatusService.checkServiceStatus(serviceStatus);

        assertTrue(serviceStatus.isServiceAvailable());
        assertEquals(response, serviceStatus.getServiceMessage());
    }

    @Test
    public void test_ok_fail_with_status_values() throws IOException {
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(200, response));
        List statusValues = new ArrayList<>();
        String value1 = "ServiceStatus\"Started\"";
        statusValues.add(value1);
        String value2 = "TakCacheInitialized\"false\"";
        statusValues.add(value2);
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(statusValues);

        ServiceStatus serviceStatus = new ServiceStatus();
        serviceStatus.setName(SERVICE_1);

        processingStatusService.checkServiceStatus(serviceStatus);

        assertFalse(serviceStatus.isServiceAvailable());
        assertThat(serviceStatus.getServiceMessage(), CoreMatchers.containsString("Missing values:"));
        assertThat(serviceStatus.getServiceMessage(), CoreMatchers.containsString(value2));
    }

    @Test
    public void test_fail() throws IOException{
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(503, response));
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(new ArrayList<>());

        ServiceStatus serviceStatus = new ServiceStatus();
        serviceStatus.setName(SERVICE_1);

        processingStatusService.checkServiceStatus(serviceStatus);

        assertFalse(serviceStatus.isServiceAvailable());
        assertEquals(response, serviceStatus.getServiceMessage());
    }

    @Test
    public void test_fail_fail_with_status_values() throws IOException{
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ServiceResponse(503, response));
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(new ArrayList<>());
        List statusValues = new ArrayList<>();
        String value1 = "ServiceStatus\"Started\"";
        statusValues.add(value1);
        String value2 = "TakCacheInitialized\"false\"";
        statusValues.add(value2);
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(statusValues);

        ServiceStatus serviceStatus = new ServiceStatus();
        serviceStatus.setName(SERVICE_1);

        processingStatusService.checkServiceStatus(serviceStatus);

        assertFalse(serviceStatus.isServiceAvailable());
        assertThat(serviceStatus.getServiceMessage(), CoreMatchers.containsString("Missing values:"));
        assertThat(serviceStatus.getServiceMessage(), CoreMatchers.containsString(value2));
    }

    @Test
    public void test_fail_exception() throws IOException{
        Mockito.when(requestSender.sendStatusRequest(Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt())).thenThrow(new IOException("Exception"));
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(new ArrayList<>());
        List statusValues = new ArrayList<>();
        String value1 = "ServiceStatus\"Started\"";
        statusValues.add(value1);
        String value2 = "TakCacheInitialized\"false\"";
        statusValues.add(value2);
        Mockito.when(servicesConfig.getStatusValues(SERVICE_1)).thenReturn(statusValues);

        ServiceStatus serviceStatus = new ServiceStatus();
        serviceStatus.setName(SERVICE_1);

        processingStatusService.checkServiceStatus(serviceStatus);

        assertFalse(serviceStatus.isServiceAvailable());
        assertThat(serviceStatus.getServiceMessage(), CoreMatchers.containsString("Exception"));
    }

}