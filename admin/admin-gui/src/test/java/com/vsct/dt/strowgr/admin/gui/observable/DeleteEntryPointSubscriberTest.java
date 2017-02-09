package com.vsct.dt.strowgr.admin.gui.observable;

import com.vsct.dt.strowgr.admin.core.event.out.DeleteEntryPointEvent;
import com.vsct.dt.strowgr.admin.nsq.producer.NSQDispatcher;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.*;

public class DeleteEntryPointSubscriberTest {

    private final NSQDispatcher nsqDispatcher = mock(NSQDispatcher.class);

    private final DeleteEntryPointSubscriber deleteEntryPointSubscriber = new DeleteEntryPointSubscriber(nsqDispatcher);

    private final DeleteEntryPointEvent deleteEntryPointEvent = mock(DeleteEntryPointEvent.class);

    @Before
    public void setUp() throws Exception {
        when(deleteEntryPointEvent.getCorrelationId()).thenReturn("cId");
        when(deleteEntryPointEvent.getHaproxyName()).thenReturn("hap");
        when(deleteEntryPointEvent.getApplication()).thenReturn("app");
        when(deleteEntryPointEvent.getPlatform()).thenReturn("ptf");
    }

    @Test
    public void should_send_event_to_nsq_dispatcher() throws Exception {

        // when
        deleteEntryPointSubscriber.accept(deleteEntryPointEvent);

        // then
        verify(nsqDispatcher).sendDeleteRequested("cId", "hap", "app", "ptf");
    }

    @Test
    public void should_handle_exceptions_from_nsq_dispatcher() throws Exception {
        // given
        doThrow(new RuntimeException()).when(nsqDispatcher).sendDeleteRequested("cId", "hap", "app", "ptf");

        // when
        deleteEntryPointSubscriber.accept(deleteEntryPointEvent);

        // then no exception is thrown
    }
}