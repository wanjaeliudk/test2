package com.dtbafrica.tradefinanceussdservice.utilities;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CountryCodeUtil {

  private static final Pattern COUNTRY_CODE_PATTERN = Pattern.compile("^\\+?(\\d{1,3})");
  private static final Map<String, String> COUNTRY_CODE_MAP = new HashMap<>();

  static {
    COUNTRY_CODE_MAP.put("254", "KE"); // Kenya
    COUNTRY_CODE_MAP.put("256", "UG"); // Uganda
    COUNTRY_CODE_MAP.put("255", "TZ"); // Tanzania
  }

  public static String getCountryCode(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.isEmpty()) {
      return null;
    }

    Matcher matcher = COUNTRY_CODE_PATTERN.matcher(phoneNumber);

    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  public static String getCountryCodeRegex() {
    return "^\\+?(" + String.join("|", COUNTRY_CODE_MAP.keySet()) + ")";
  }

  public static String getCountry_ISO_CODE(String phoneNumber) {
    if (phoneNumber == null || phoneNumber.isEmpty()) {
      log.error("error getting phoneNumber from session");
      throw new InternalError("END Error occurred");
    }
    try {
      return COUNTRY_CODE_MAP.get(getCountryCode(phoneNumber));
    } catch (Throwable e) {
      log.error("error getting country code{}", e.getMessage());
      throw new InternalError("Country code not available for this service");
    }
  }

  public static String appendCountryCodeToNumber(String to, String sessionNumber) {
    String countryCode = getCountryCode(sessionNumber);
    if (to.matches("^0\\d{8,}$")) {
      return countryCode + to.substring(1);
    } else if (!to.startsWith(countryCode)) {
      return countryCode + to;
    }
    return to;
  }
}
