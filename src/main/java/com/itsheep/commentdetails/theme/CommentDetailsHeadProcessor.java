package com.itsheep.commentdetails.theme;

import com.itsheep.commentdetails.settings.DisplaySettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.plugin.ReactiveSettingFetcher;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

@Component
public class CommentDetailsHeadProcessor implements TemplateHeadProcessor {

    private static final Logger log = LoggerFactory.getLogger(CommentDetailsHeadProcessor.class);

    private final PluginContext pluginContext;
    private final ReactiveSettingFetcher settingFetcher;

    public CommentDetailsHeadProcessor(
        PluginContext pluginContext,
        ReactiveSettingFetcher settingFetcher
    ) {
        this.pluginContext = pluginContext;
        this.settingFetcher = settingFetcher;
    }

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler) {
        return settingFetcher.fetch(DisplaySettings.GROUP, DisplaySettings.class)
            .onErrorResume(exception -> {
                log.warn("Unable to read comment details settings", exception);
                return Mono.just(DisplaySettings.disabled());
            })
            .doOnNext(settings -> {
                if (settings.isEnabled()) {
                    model.add(context.getModelFactory().createText(resourceTag(settings)));
                }
            })
            .then();
    }

    private String resourceTag(DisplaySettings settings) {
        return """
            <!-- comment-details start -->
            <script
              id="comment-details-script"
              defer
              src="/plugins/%s/assets/static/comment-details.js?version=%s"
              data-location-format="%s"
              data-show-unknown-location="%s"
              data-show-on-replies="%s"></script>
            <!-- comment-details end -->
            """.formatted(
            pluginContext.getName(),
            pluginContext.getVersion(),
            settings.normalizedLocationFormat(),
            settings.isShowUnknownLocation(),
            settings.isShowOnReplies()
        );
    }
}
