package com.dtbafrica.tradefinanceussdservice.service.menu;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class UssdTemplateRenderer {

  private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

  public String render(String template, Map<String, String> variables) {
    Matcher matcher = PLACEHOLDER.matcher(template);
    StringBuffer buffer = new StringBuffer();
    while (matcher.find()) {
      String value = variables.getOrDefault(matcher.group(1), "");
      matcher.appendReplacement(buffer, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(buffer);
    return buffer.toString();
  }
}
