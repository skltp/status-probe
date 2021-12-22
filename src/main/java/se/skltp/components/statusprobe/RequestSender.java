package se.skltp.components.statusprobe;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RequestSender {
    public ServiceResponse sendStatusRequest(String url, int connectTimeout, int socketTimeout) throws IOException {
        HttpClient client = getHttpClient(connectTimeout, socketTimeout);

        GetMethod method = new GetMethod(url);
        method.setRequestHeader("Connection", "close");

        int status = client.executeMethod(method);
        String responseMessage = method.getResponseBodyAsString();

        ServiceResponse serviceResponse = new ServiceResponse(status, responseMessage);
        method.releaseConnection();
        return serviceResponse;
    }

    private HttpClient getHttpClient(int connectTimeout, int socketTimeout) {
        HttpClient client = new HttpClient();
        client.getHttpConnectionManager().getParams().setConnectionTimeout(connectTimeout);
        client.getHttpConnectionManager().getParams().setSoTimeout(socketTimeout);
        return client;
    }
}
