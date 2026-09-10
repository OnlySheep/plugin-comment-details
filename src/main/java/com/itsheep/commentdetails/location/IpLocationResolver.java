package com.itsheep.commentdetails.location;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Optional;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Component
public class IpLocationResolver {

    private static final Logger log = LoggerFactory.getLogger(IpLocationResolver.class);
    private static final String DATABASE_RESOURCE = "ip2region/ip2region_v4.xdb";

    private volatile Searcher searcher;

    @PostConstruct
    void initialize() {
        try (var input = new ClassPathResource(DATABASE_RESOURCE).getInputStream()) {
            searcher = Searcher.newWithBuffer(input.readAllBytes());
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load IP location database", exception);
        }
    }

    public Mono<IpLocation> resolve(String ipAddress) {
        return Mono.fromCallable(() -> resolveBlocking(ipAddress))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(Mono::justOrEmpty)
            .onErrorResume(exception -> {
                log.warn("Unable to resolve comment IP location", exception);
                return Mono.empty();
            });
    }

    private Optional<IpLocation> resolveBlocking(String ipAddress) throws Exception {
        if (!isPublicIpv4Address(ipAddress) || searcher == null) {
            return Optional.empty();
        }
        var location = IpLocation.fromIp2Region(searcher.search(ipAddress));
        return location.isKnown() ? Optional.of(location) : Optional.empty();
    }

    private boolean isPublicIpv4Address(String ipAddress) {
        try {
            var address = InetAddress.getByName(ipAddress);
            return address instanceof Inet4Address
                && !address.isAnyLocalAddress()
                && !address.isLoopbackAddress()
                && !address.isLinkLocalAddress()
                && !address.isSiteLocalAddress()
                && !address.isMulticastAddress();
        } catch (IOException exception) {
            return false;
        }
    }

    @PreDestroy
    void close() {
        var currentSearcher = searcher;
        if (currentSearcher == null) {
            return;
        }
        try {
            currentSearcher.close();
        } catch (IOException exception) {
            log.warn("Unable to close IP location database", exception);
        }
    }
}
