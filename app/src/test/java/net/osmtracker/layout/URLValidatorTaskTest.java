package net.osmtracker.layout;

import net.osmtracker.OSMTracker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class URLValidatorTaskTest {

    URLValidatorTask urlValidatorTask = new URLValidatorTask();

    @Test
    public void customLayoutsRepoValidatorTest() {
        String validUser = OSMTracker.Preferences.VAL_GITHUB_USERNAME;
        String validRepository = OSMTracker.Preferences.VAL_REPOSITORY_NAME;
        String validBranch = OSMTracker.Preferences.VAL_BRANCH_NAME;
        String invalidBranch = "NONE";
        Boolean result;

        result = urlValidatorTask.customLayoutsRepoValidator(validUser, validRepository, validBranch);
        assertEquals(true, result);

        result = urlValidatorTask.customLayoutsRepoValidator(validUser, validRepository, invalidBranch);
        assertEquals(false, result);
    }

}
