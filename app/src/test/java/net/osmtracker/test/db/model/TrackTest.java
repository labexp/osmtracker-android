package net.osmtracker.test.db.model;

import android.content.ContentResolver;
import android.database.Cursor;
import android.opengl.Visibility;

import net.osmtracker.db.TrackContentProvider;
import net.osmtracker.db.model.Track;
import static net.osmtracker.db.TrackContentProvider.Schema.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.*;

//@RunWith(PowerMockRunner.class)
public class TrackTest {

    final long START_DATE = 123;
    final String NAME = "some name";
    final String DESCRIPTION = "foo desc";
    final String TAGS = "tag1,tag2,tag3";
    final List TAGS_LIST = Arrays.asList("tag1","tag2","tag3");
    final String VISIBILITY = Track.OSMVisibility.Public.name();
    final int TRACKPOINT_COUNT = 10;
    final int WAYPOINT_COUNT = 20;

    public Cursor initMockCursor(){
        Cursor mockCursor = mock(Cursor.class);

        // Columns mocks
        when(mockCursor.getColumnIndex(COL_START_DATE)).thenReturn(1);
        when(mockCursor.getLong(1)).thenReturn(START_DATE);


        when(mockCursor.getColumnIndex(TrackContentProvider.Schema.COL_NAME)).thenReturn(2);
        when(mockCursor.getString(2)).thenReturn(NAME);

        when(mockCursor.getColumnIndex(TrackContentProvider.Schema.COL_DESCRIPTION)).thenReturn(3);
        when(mockCursor.getString(3)).thenReturn(DESCRIPTION);

        when(mockCursor.getColumnIndex(TrackContentProvider.Schema.COL_TAGS)).thenReturn(4);
        when(mockCursor.getString(4)).thenReturn(TAGS);

        when(mockCursor.getColumnIndex(TrackContentProvider.Schema.COL_OSM_VISIBILITY)).thenReturn(5);
        when(mockCursor.getString(5)).thenReturn(VISIBILITY);

        when(mockCursor.getColumnIndex(TrackContentProvider.Schema.COL_TRACKPOINT_COUNT)).thenReturn(6);
        when(mockCursor.getInt(6)).thenReturn(TRACKPOINT_COUNT);

        when(mockCursor.getColumnIndex(TrackContentProvider.Schema.COL_WAYPOINT_COUNT)).thenReturn(7);
        when(mockCursor.getInt(7)).thenReturn(WAYPOINT_COUNT);

        return mockCursor;
    }

    @Test
    public void testBuild(){

        int trackId = 1;
        ContentResolver resolver = null; // Not used in the method
        Cursor mockCursor = initMockCursor();
        boolean withExtraInfo = false;

        Track t = Track.build(1, mockCursor, resolver, withExtraInfo);


        try {
            Field startDateField = t.getClass().getDeclaredField("trackDate");
            startDateField.setAccessible(true);

            Field tagsField = t.getClass().getDeclaredField("tags");
            tagsField.setAccessible(true);


            assertEquals(START_DATE, startDateField.get(t));
            assertEquals(NAME, t.getName());
            assertEquals(DESCRIPTION, t.getDescription());
            assertEquals(TAGS_LIST, tagsField.get(t));
            assertEquals(Track.OSMVisibility.valueOf(VISIBILITY), t.getVisibility());
            assertEquals( TRACKPOINT_COUNT, (long) t.getTpCount() );
            assertEquals( WAYPOINT_COUNT, (long) t.getWpCount() );


        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
