package net.osmtracker.layout;

import android.content.Context;

import org.junit.Test;

import static org.powermock.api.mockito.PowerMockito.mock;

public class DownloadCustomLayoutTaskTest {

    DownloadCustomLayoutTask downloadCustomLayoutTask;

    Context mockContext;

    public void setupMocks() {
        // Create PreferenceManager mock
        mockContext = mock(Context.class);

        downloadCustomLayoutTask = new DownloadCustomLayoutTask(mockContext);
    }

    @Test
    public void downloadLayoutTest() {
        setupMocks();
        //TODO: check if after downloading the layout all files are correctly organized.

    }
}
