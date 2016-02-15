package com.vsct.dt.haas.admin.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vsct.dt.haas.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.haas.admin.core.configuration.EntryPointConfiguration;
import com.vsct.dt.haas.admin.core.configuration.EntryPointFrontend;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

/**
 * Created by william_montaz on 04/02/2016.
 */
public class EntryPointStateManagerTest {

    EntryPointStateManager entryPointStateManager;
    EntryPointRepository repositoryMock;

    @Before
    public void setUp() {
        repositoryMock = mock(EntryPointRepository.class);
        entryPointStateManager = new EntryPointStateManager(repositoryMock);
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_committing_one__should_add_pending_configuration_if_it_is_different_than_committing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration differentNewConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("51000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration committingConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(committingConfiguration)
        );

        entryPointStateManager.prepare(key, differentNewConfiguration);

        verify(repositoryMock).setPendingConfiguration(key, differentNewConfiguration);
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_committing_one__should_not_add_pending_configuration_if_it_is_same_as_committing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration sameNewConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration committingConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(committingConfiguration)
        );

        entryPointStateManager.prepare(key, sameNewConfiguration);

        verify(repositoryMock, never()).setPendingConfiguration(any(), any());
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_nothing_is_committing__should_add_pending_configuration_if_it_is_different_than_current() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration differentNewConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("51000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration currentConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );
        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );

        entryPointStateManager.prepare(key, differentNewConfiguration);

        verify(repositoryMock).setPendingConfiguration(key, differentNewConfiguration);
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_nothing_is_committing__should_not_add_pending_configuration_if_it_is_same_as_current() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration sameNewConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration currentConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );
        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );

        entryPointStateManager.prepare(key, sameNewConfiguration);

        verify(repositoryMock, never()).setPendingConfiguration(any(), any());
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_nothing_is_committing_or_current__should_add_pending_configuration() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration newConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );
        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.prepare(key, newConfiguration);

        verify(repositoryMock).setPendingConfiguration(key, newConfiguration);
    }

    @Test
    public void try_commit_pending_configuration__with_pending_without_committing_should_create_committing_from_pending() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration pendingConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getPendingConfiguration(key)).thenReturn(
                Optional.of(pendingConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.tryCommitPending(key);

        verify(repositoryMock).setCommittingConfiguration(key, pendingConfiguration);
        verify(repositoryMock).removePendingConfiguration(key);
    }

    @Test
    public void try_commit_pending_configuration__with_pending_with_committing_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration pendingConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("51000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration existingCommittingConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getPendingConfiguration(key)).thenReturn(
                Optional.of(pendingConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(existingCommittingConfiguration)
        );

        entryPointStateManager.tryCommitPending(key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any());
        verify(repositoryMock, never()).removePendingConfiguration(any());
    }

    @Test
    public void try_commit_pending_configuration__without_pending_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        when(repositoryMock.getPendingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.tryCommitPending(key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any());
        verify(repositoryMock, never()).removePendingConfiguration(any());
    }

    @Test
    public void try_commit_current_configuration__with_current_without_committing_should_create_committing_from_current() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration currentConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.tryCommitCurrent(key);

        verify(repositoryMock).setCommittingConfiguration(eq(key), eq(currentConfiguration));
    }

    @Test
    public void try_commit_current_configuration__with_current_with_committing_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration currentConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("51000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPointConfiguration existingCommittingConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("52000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(existingCommittingConfiguration)
        );

        entryPointStateManager.tryCommitCurrent(key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any());
    }

    @Test
    public void try_commit_current_configuration__without_current_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.tryCommitCurrent(key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any());
    }

    @Test
    public void commit_configuration__without_committing_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.commit(key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any());
        verify(repositoryMock, never()).removePendingConfiguration(any());
    }

    @Test
    public void commit_configuration__with_committing_should_replace_current_by_committing_and_leave_no_committing_and_no_pending() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPointConfiguration committingConfiguration = EntryPointConfiguration
                .onHaproxy("haproxy")
                .withUser("hapuser")
                .withSyslogPort("51000")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(committingConfiguration)
        );

        entryPointStateManager.commit(key);

        verify(repositoryMock).setCurrentConfiguration(eq(key), eq(committingConfiguration));
        verify(repositoryMock).removeCommittingConfiguration(key);
        verify(repositoryMock, never()).removePendingConfiguration(key);
    }

}
