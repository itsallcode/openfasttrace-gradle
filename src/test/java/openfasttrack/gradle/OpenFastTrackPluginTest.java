package openfasttrack.gradle;

import org.gradle.api.Project;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

public class OpenFastTrackPluginTest
{
    @Rule
    public MockitoRule mrule = MockitoJUnit.rule();

    private OpenFastTrackPlugin plugin;

    @Mock
    private Project projectMock;

    @Before
    public void setUp()
    {
        plugin = new OpenFastTrackPlugin();
    }

    @Test
    public void testApply()
    {
        plugin.apply(projectMock);
    }
}
