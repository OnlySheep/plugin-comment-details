package com.itsheep.commentdetails.location;

import java.util.Arrays;
import java.util.List;

public record IpLocation(String country, String region, String city) {

    private static final String UNKNOWN = "0";

    public static IpLocation fromIp2Region(String result) {
        if (result == null || result.isBlank()) {
            return new IpLocation(null, null, null);
        }

        var values = Arrays.stream(result.split("\\|", -1))
            .map(IpLocation::normalize)
            .toList();
        if (hasCountryCode(valueAt(values, 4))) {
            return new IpLocation(
                valueAt(values, 0),
                valueAt(values, 1),
                valueAt(values, 2)
            );
        }
        if (values.size() == 4) {
            return new IpLocation(
                valueAt(values, 0),
                valueAt(values, 1),
                isNetworkProvider(valueAt(values, 2)) ? null : valueAt(values, 2)
            );
        }
        var city = valueAt(values, 3);
        return new IpLocation(
            valueAt(values, 0),
            firstKnown(valueAt(values, 2), valueAt(values, 1)),
            isNetworkProvider(city) ? null : city
        );
    }

    public boolean isKnown() {
        return country != null || region != null || city != null;
    }

    private static String valueAt(List<String> values, int index) {
        return index < values.size() ? values.get(index) : null;
    }

    private static String firstKnown(String primary, String fallback) {
        return primary != null ? primary : fallback;
    }

    private static boolean hasCountryCode(String value) {
        return value != null && value.matches("[A-Z]{2}");
    }

    private static boolean isNetworkProvider(String value) {
        return value != null && (value.contains("电信") || value.contains("移动") || value.contains("联通"));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim();
        return normalized.isEmpty() || UNKNOWN.equals(normalized) ? null : normalized;
    }
}
