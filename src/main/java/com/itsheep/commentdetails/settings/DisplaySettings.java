package com.itsheep.commentdetails.settings;

public record DisplaySettings(
    Boolean enabled,
    String locationFormat,
    Boolean showUnknownLocation,
    Boolean showOnReplies
) {

    public static final String GROUP = "display";

    public static DisplaySettings defaults() {
        return new DisplaySettings(true, "city", false, true);
    }

    public static DisplaySettings disabled() {
        return new DisplaySettings(false, "city", false, false);
    }

    public boolean isEnabled() {
        return enabled == null || enabled;
    }

    public boolean isShowUnknownLocation() {
        return Boolean.TRUE.equals(showUnknownLocation);
    }

    public boolean isShowOnReplies() {
        return showOnReplies == null || showOnReplies;
    }

    public String normalizedLocationFormat() {
        if ("country".equals(locationFormat) || "region".equals(locationFormat)) {
            return locationFormat;
        }
        return "city";
    }
}
