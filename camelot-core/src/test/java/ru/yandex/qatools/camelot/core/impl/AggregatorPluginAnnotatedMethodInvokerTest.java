package ru.yandex.qatools.camelot.core.impl;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.spi.AggregationRepository;
import org.junit.Test;
import ru.yandex.qatools.camelot.api.annotations.OnClientMessage;
import ru.yandex.qatools.camelot.config.Plugin;
import ru.yandex.qatools.camelot.config.PluginContext;
import ru.yandex.qatools.camelot.core.FoundMethodProcessor;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;

import static java.util.Arrays.asList;
import static org.mockito.Mockito.*;
import static ru.yandex.qatools.camelot.api.Constants.Headers.BODY_CLASS;

/**
 * @author Ilya Sadykov (mailto: smecsia@yandex-team.ru)
 */
public class AggregatorPluginAnnotatedMethodInvokerTest {
    private static TestAggregator aggMock = mock(TestAggregator.class);

    public static class TestState implements Serializable {
    }

    public static interface TestAggregator {
        @OnClientMessage
        void onBroadcast(TestState state);

        @OnClientMessage
        void onBroadcast2(TestState state, String message);
    }

    public static class TestAggregatorImpl implements TestAggregator {
        @Override
        @OnClientMessage
        public void onBroadcast(TestState state) {
            aggMock.onBroadcast(state);
        }

        @Override
        @OnClientMessage
        public void onBroadcast2(TestState state, String message) {
            aggMock.onBroadcast2(state, message);
        }
    }

    /**
     * Bad style test :(
     */
    @Test
    public void testThatAnnotatedMethodInvokerIsWorkingAsExpected() throws Exception {
        CamelContext context = mock(CamelContext.class);
        Plugin plugin = new Plugin();
        PluginContext pluginContext = new PluginContext();
        pluginContext.setClassLoader(getClass().getClassLoader());
        pluginContext.setInjector(new PluginContextInjectorImpl());
        pluginContext.setPluginClass(TestAggregatorImpl.class.getName());
        plugin.setAggregator(TestAggregatorImpl.class.getName());
        plugin.setContext(pluginContext);
        AggregationRepository repo = mock(AggregationRepository.class);
        FoundMethodProcessor proc = mock(FoundMethodProcessor.class);
        TestState state = new TestState();
        Exchange exchange = mock(Exchange.class);
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(state);
        when(message.getHeader(BODY_CLASS)).thenReturn(TestState.class.getName());
        when(exchange.getIn()).thenReturn(message);
        when(repo.getKeys()).thenReturn(new HashSet<>(asList("key1", "key2")));
        when(repo.get(context, "key1")).thenReturn(exchange);
        when(repo.get(context, "key2")).thenReturn(exchange);
        when(proc.appliesTo(any(Method.class), any(Annotation.class))).thenReturn(true);
        pluginContext.setAggregationRepo(repo);


        AggregatorPluginAnnotatedMethodInvoker invoker = new AggregatorPluginAnnotatedMethodInvoker(
                context, plugin, OnClientMessage.class, true
        );
        invoker.process(proc);

        invoker.invoke("test");

        verify(proc).appliesTo(eq(TestAggregatorImpl.class.getMethod("onBroadcast", TestState.class)), any(Annotation.class));
        verify(proc).appliesTo(eq(TestAggregatorImpl.class.getMethod("onBroadcast2", TestState.class, String.class)), any(Annotation.class));
        verify(aggMock, times(2)).onBroadcast(state);
        verify(aggMock, times(2)).onBroadcast2(state, "test");
        verify(message, times(4)).setBody(any());
        verify(repo, times(2)).get(context, "key1");
        verify(repo, times(2)).get(context, "key2");
        verify(repo, times(2)).getKeys();
        verify(repo, times(2)).add(context, "key1", exchange);
        verify(repo, times(2)).add(context, "key2", exchange);
        verifyNoMoreInteractions(aggMock, repo);
    }
}
