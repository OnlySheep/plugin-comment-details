package com.itsheep.commentdetails.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IpLocationTest {

    @Test
    void readsCityAndProvinceFromIp2RegionResult() {
        var location = IpLocation.fromIp2Region("中国|0|湖北省|武汉市|电信");

        assertEquals("中国", location.country());
        assertEquals("湖北省", location.region());
        assertEquals("武汉市", location.city());
        assertTrue(location.isKnown());
    }

    @Test
    void readsProvinceAndCityFromIp2RegionV4Result() {
        var location = IpLocation.fromIp2Region("中国|湖北省|武汉市|电信|CN");

        assertEquals("中国", location.country());
        assertEquals("湖北省", location.region());
        assertEquals("武汉市", location.city());
        assertTrue(location.isKnown());
    }

    @Test
    void excludesNetworkProviderFromCity() {
        var location = IpLocation.fromIp2Region("中国|0|湖北省|电信|0");

        assertEquals("湖北省", location.region());
        assertEquals(null, location.city());
    }

    @Test
    void ignoresUnknownSegments() {
        var location = IpLocation.fromIp2Region("0|0|0|0|0");

        assertFalse(location.isKnown());
    }

    @Test
    void usesSecondSegmentWhenProvinceIsUnavailable() {
        var location = IpLocation.fromIp2Region("美国|加利福尼亚|0|洛杉矶|0");

        assertEquals("加利福尼亚", location.region());
        assertEquals("洛杉矶", location.city());
    }
}
