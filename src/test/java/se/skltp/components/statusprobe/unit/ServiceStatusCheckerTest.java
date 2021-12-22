package se.skltp.components.statusprobe.unit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import se.skltp.components.statusprobe.ServiceStatusChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
public class ServiceStatusCheckerTest {

    @Test
    public void checkStatus() {
        ServiceStatusChecker serviceStatusChecker = new ServiceStatusChecker();
        String test = "" +
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
        List<String> statusValues = new ArrayList<>();
        String value1 = "ServiceStatus\"Started\"";
        statusValues.add(value1);
        String value2 = "TakCacheInitialized\"false\"";
        statusValues.add(value2);
        Set<String> strings = serviceStatusChecker.checkStatus(test, statusValues);
        assertFalse(strings.contains(value1));
        assertTrue(strings.contains(value2));
    }
}