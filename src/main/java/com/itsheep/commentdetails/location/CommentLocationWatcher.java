package com.itsheep.commentdetails.location;

import com.itsheep.commentdetails.settings.DisplaySettings;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.Extension;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.extension.Watcher;
import run.halo.app.plugin.ReactiveSettingFetcher;

@Component
public class CommentLocationWatcher implements Watcher {

    public static final String ANNOTATION_PREFIX = "comment-details.itsheep.com/";
    public static final String COUNTRY_ANNOTATION = ANNOTATION_PREFIX + "location-country";
    public static final String REGION_ANNOTATION = ANNOTATION_PREFIX + "location-region";
    public static final String CITY_ANNOTATION = ANNOTATION_PREFIX + "location-city";
    public static final String UNKNOWN_ANNOTATION = ANNOTATION_PREFIX + "location-unknown";
    public static final String SCHEMA_VERSION_ANNOTATION = ANNOTATION_PREFIX + "schema-version";
    public static final String SCHEMA_VERSION = "4";

    private static final Logger log = LoggerFactory.getLogger(CommentLocationWatcher.class);

    private final ReactiveExtensionClient client;
    private final IpLocationResolver locationResolver;
    private final ReactiveSettingFetcher settingFetcher;
    private final AtomicBoolean disposed = new AtomicBoolean();
    private final Map<String, Boolean> processing = new ConcurrentHashMap<>();
    private Runnable disposeHook = () -> { };

    public CommentLocationWatcher(
        ReactiveExtensionClient client,
        IpLocationResolver locationResolver,
        ReactiveSettingFetcher settingFetcher
    ) {
        this.client = client;
        this.locationResolver = locationResolver;
        this.settingFetcher = settingFetcher;
        client.watch(this);
    }

    @PostConstruct
    void migrateExistingLocations() {
        settingFetcher.fetch(DisplaySettings.GROUP, DisplaySettings.class)
            .filter(DisplaySettings::isEnabled)
            .flatMapMany(settings -> Flux.concat(
                client.list(Comment.class, this::requiresMigration, byName())
                    .doOnNext(comment -> process(Comment.class, comment.getMetadata().getName(),
                        comment.getSpec().getIpAddress(), false)),
                client.list(Reply.class, this::requiresMigration, byName())
                    .doOnNext(reply -> process(Reply.class, reply.getMetadata().getName(),
                        reply.getSpec().getIpAddress(), true))
            ))
            .subscribe(
                ignored -> { },
                exception -> log.warn("Unable to migrate existing comment IP locations", exception)
            );
    }

    private boolean requiresMigration(Extension extension) {
        return !isProcessed(extension);
    }

    private static <T extends Extension> Comparator<T> byName() {
        return Comparator.comparing((T extension) -> extension.getMetadata().getName(),
            Comparator.nullsLast(Comparator.naturalOrder()));
    }

    @Override
    public void onAdd(Extension extension) {
        if (extension instanceof Comment comment) {
            if (!isProcessed(comment)) {
                process(Comment.class, comment.getMetadata().getName(),
                    comment.getSpec().getIpAddress(), false);
            }
        } else if (extension instanceof Reply reply) {
            if (!isProcessed(reply)) {
                process(Reply.class, reply.getMetadata().getName(), reply.getSpec().getIpAddress(), true);
            }
        }
    }

    private <T extends Extension> void process(
        Class<T> type,
        String name,
        String ipAddress,
        boolean reply
    ) {
        if (name == null || ipAddress == null || ipAddress.isBlank()) {
            return;
        }
        var key = type.getName() + ':' + name;
        if (processing.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }

        settingFetcher.fetch(DisplaySettings.GROUP, DisplaySettings.class)
            .onErrorResume(exception -> {
                log.warn("Unable to read comment details settings", exception);
                return Mono.just(DisplaySettings.disabled());
            })
            .filter(settings -> settings.isEnabled() && (!reply || settings.isShowOnReplies()))
            .flatMap(settings -> locationResolver.resolve(ipAddress)
                .map(LocationResolution::known)
                .switchIfEmpty(Mono.just(LocationResolution.missing()))
                .flatMap(resolution -> updateLocation(type, name, resolution)))
            .doFinally(signalType -> processing.remove(key))
            .subscribe(
                ignored -> { },
                exception -> log.warn("Unable to save IP location for comment {}", name, exception)
            );
    }

    private <T extends Extension> Mono<T> updateLocation(
        Class<T> type,
        String name,
        LocationResolution resolution
    ) {
        return client.fetch(type, name)
            .filter(extension -> !isProcessed(extension))
            .map(extension -> withLocation(extension, resolution))
            .flatMap(client::update);
    }

    private boolean isProcessed(Extension extension) {
        var annotations = extension.getMetadata().getAnnotations();
        return annotations != null && SCHEMA_VERSION.equals(annotations.get(SCHEMA_VERSION_ANNOTATION));
    }

    private <T extends Extension> T withLocation(T extension, LocationResolution resolution) {
        var annotations = new LinkedHashMap<String, String>();
        if (extension.getMetadata().getAnnotations() != null) {
            annotations.putAll(extension.getMetadata().getAnnotations());
        }
        if (resolution.unknown()) {
            annotations.put(UNKNOWN_ANNOTATION, "true");
        } else {
            annotations.remove(UNKNOWN_ANNOTATION);
            putIfPresent(annotations, COUNTRY_ANNOTATION, resolution.location().country());
            putIfPresent(annotations, REGION_ANNOTATION, resolution.location().region());
            putIfPresent(annotations, CITY_ANNOTATION, resolution.location().city());
        }
        annotations.put(SCHEMA_VERSION_ANNOTATION, SCHEMA_VERSION);
        extension.getMetadata().setAnnotations(annotations);
        return extension;
    }

    private void putIfPresent(Map<String, String> annotations, String key, String value) {
        if (value != null && !value.isBlank()) {
            annotations.put(key, value);
        }
    }

    @Override
    public void onUpdate(Extension oldExtension, Extension newExtension) {
    }

    @Override
    public void onDelete(Extension extension) {
    }

    @Override
    public void registerDisposeHook(Runnable dispose) {
        disposeHook = dispose;
    }

    @PreDestroy
    @Override
    public void dispose() {
        if (disposed.compareAndSet(false, true)) {
            disposeHook.run();
        }
    }

    @Override
    public boolean isDisposed() {
        return disposed.get();
    }

    private record LocationResolution(IpLocation location, boolean unknown) {

        private static LocationResolution known(IpLocation location) {
            return new LocationResolution(location, false);
        }

        private static LocationResolution missing() {
            return new LocationResolution(null, true);
        }
    }
}
