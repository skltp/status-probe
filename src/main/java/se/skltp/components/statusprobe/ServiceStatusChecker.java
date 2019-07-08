package se.skltp.components.statusprobe;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ServiceStatusChecker {

    public Set<String> checkStatus(String text, List<String> statusValues) {
        Set<String> missingValues = new HashSet<>();

        for (String statusValue : statusValues) {
            if (!checkOne(text, statusValue)) {
                missingValues.add(statusValue);
            }
        }
        return missingValues;
    }

    private boolean checkOne(String text, String regexp) {
        Pattern pattern = Pattern.compile(regexp);
        Matcher m = pattern.matcher(text);
        return m.find();
    }

}
