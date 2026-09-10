package com.itsheep.commentdetails.location;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itsheep.commentdetails.settings.DisplaySettings;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.content.Comment;
import run.halo.app.core.extension.content.Reply;
import run.halo.app.extension.Metadata;
import run.halo.app.extension.ReactiveExtensionClient;
import run.halo.app.plugin.ReactiveSettingFetcher;

class CommentLocationWatcherTest {

    @Test
    void storesLocationAnnotationsForNewComment() {
        var client = mock(ReactiveExtensionClient.class);
        var resolver = mock(IpLocationResolver.class);
        var comment = comment("comment-1", "8.8.8.8");
        var location = new IpLocation("United States", "California", "Mountain View");

        when(resolver.resolve("8.8.8.8")).thenReturn(Mono.just(location));
        when(client.fetch(Comment.class, "comment-1")).thenReturn(Mono.just(comment));
        when(client.update(any(Comment.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var watcher = new CommentLocationWatcher(client, resolver, settingFetcher(DisplaySettings.defaults()));
        watcher.onAdd(comment);

        verify(client, timeout(1000)).update(comment);
        assertEquals("United States", comment.getMetadata().getAnnotations()
            .get(CommentLocationWatcher.COUNTRY_ANNOTATION));
        assertEquals("California", comment.getMetadata().getAnnotations()
            .get(CommentLocationWatcher.REGION_ANNOTATION));
        assertEquals("Mountain View", comment.getMetadata().getAnnotations()
            .get(CommentLocationWatcher.CITY_ANNOTATION));
        assertEquals(CommentLocationWatcher.SCHEMA_VERSION, comment.getMetadata().getAnnotations()
            .get(CommentLocationWatcher.SCHEMA_VERSION_ANNOTATION));
        assertFalse(comment.getMetadata().getAnnotations().containsValue("8.8.8.8"));
    }

    @Test
    void storesLocationAnnotationsForNewReply() {
        var client = mock(ReactiveExtensionClient.class);
        var resolver = mock(IpLocationResolver.class);
        var reply = reply("reply-1", "1.1.1.1");
        var location = new IpLocation("Australia", "Queensland", "Brisbane");

        when(resolver.resolve("1.1.1.1")).thenReturn(Mono.just(location));
        when(client.fetch(Reply.class, "reply-1")).thenReturn(Mono.just(reply));
        when(client.update(any(Reply.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var watcher = new CommentLocationWatcher(client, resolver, settingFetcher(DisplaySettings.defaults()));
        watcher.onAdd(reply);

        verify(client, timeout(1000)).update(reply);
        assertEquals("Brisbane", reply.getMetadata().getAnnotations()
            .get(CommentLocationWatcher.CITY_ANNOTATION));
    }

    @Test
    void recordsUnknownStatusWithoutStoringTheIpAddress() {
        var client = mock(ReactiveExtensionClient.class);
        var resolver = mock(IpLocationResolver.class);
        var comment = comment("comment-2", "203.0.113.1");

        when(resolver.resolve("203.0.113.1")).thenReturn(Mono.empty());
        when(client.fetch(Comment.class, "comment-2")).thenReturn(Mono.just(comment));
        when(client.update(any(Comment.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        var watcher = new CommentLocationWatcher(client, resolver, settingFetcher(DisplaySettings.defaults()));
        watcher.onAdd(comment);

        verify(client, timeout(1000)).update(comment);
        assertEquals("true", comment.getMetadata().getAnnotations()
            .get(CommentLocationWatcher.UNKNOWN_ANNOTATION));
        assertFalse(comment.getMetadata().getAnnotations().containsValue("203.0.113.1"));
    }

    @Test
    void skipsProcessingWhenDisplayIsDisabled() {
        var client = mock(ReactiveExtensionClient.class);
        var resolver = mock(IpLocationResolver.class);
        var comment = comment("comment-3", "8.8.4.4");

        var watcher = new CommentLocationWatcher(client, resolver,
            settingFetcher(DisplaySettings.disabled()));
        watcher.onAdd(comment);

        verifyNoInteractions(resolver);
    }

    private ReactiveSettingFetcher settingFetcher(DisplaySettings settings) {
        var settingFetcher = mock(ReactiveSettingFetcher.class);
        when(settingFetcher.fetch(DisplaySettings.GROUP, DisplaySettings.class))
            .thenReturn(Mono.just(settings));
        return settingFetcher;
    }

    private Comment comment(String name, String ipAddress) {
        var comment = new Comment();
        comment.setMetadata(metadata(name));
        var spec = new Comment.CommentSpec();
        spec.setIpAddress(ipAddress);
        comment.setSpec(spec);
        return comment;
    }

    private Reply reply(String name, String ipAddress) {
        var reply = new Reply();
        reply.setMetadata(metadata(name));
        var spec = new Reply.ReplySpec();
        spec.setIpAddress(ipAddress);
        reply.setSpec(spec);
        return reply;
    }

    private Metadata metadata(String name) {
        var metadata = new Metadata();
        metadata.setName(name);
        return metadata;
    }
}
