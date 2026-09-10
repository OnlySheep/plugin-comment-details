package com.itsheep.commentdetails.theme;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.itsheep.commentdetails.settings.DisplaySettings;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IText;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.plugin.ReactiveSettingFetcher;

class CommentDetailsHeadProcessorTest {

    @Test
    void injectsVersionedFrontendScriptWithDisplayOptions() {
        var pluginContext = mock(PluginContext.class);
        when(pluginContext.getName()).thenReturn("comment-details");
        when(pluginContext.getVersion()).thenReturn("1.0.0");

        var context = mock(ITemplateContext.class);
        var model = mock(IModel.class);
        var factory = mock(IModelFactory.class);
        var text = mock(IText.class);
        when(context.getModelFactory()).thenReturn(factory);
        when(factory.createText(contains("comment-details.js"))).thenReturn(text);

        var processor = new CommentDetailsHeadProcessor(pluginContext,
            settingFetcher(new DisplaySettings(true, "region", true, false)));
        processor.process(context, model,
            mock(org.thymeleaf.processor.element.IElementModelStructureHandler.class)).subscribe();

        verify(factory).createText(
            contains("/plugins/comment-details/assets/static/comment-details.js?version=1.0.0"));
        verify(factory).createText(contains("data-location-format=\"region\""));
        verify(factory).createText(contains("data-show-unknown-location=\"true\""));
        verify(factory).createText(contains("data-show-on-replies=\"false\""));
        verify(model).add(text);
    }

    @Test
    void omitsScriptWhenDisplayIsDisabled() {
        var context = mock(ITemplateContext.class);
        var model = mock(IModel.class);
        var processor = new CommentDetailsHeadProcessor(mock(PluginContext.class),
            settingFetcher(DisplaySettings.disabled()));

        processor.process(context, model,
            mock(org.thymeleaf.processor.element.IElementModelStructureHandler.class)).subscribe();

        verifyNoInteractions(context, model);
    }

    private ReactiveSettingFetcher settingFetcher(DisplaySettings settings) {
        var settingFetcher = mock(ReactiveSettingFetcher.class);
        when(settingFetcher.fetch(DisplaySettings.GROUP, DisplaySettings.class))
            .thenReturn(Mono.just(settings));
        return settingFetcher;
    }
}
