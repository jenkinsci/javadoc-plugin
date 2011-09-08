package hudson.tasks.javadoc;

import hudson.model.FreeStyleProject;
import hudson.tasks.JavadocArchiver;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author Kohsuke Kawaguchi
 */
public class JavadocArchiverTest extends HudsonTestCase {
    public void testConfigRoundtrip() throws Exception {
        verify(new JavadocArchiver("foo", true));
        verify(new JavadocArchiver(".", false));
    }

    private void verify(JavadocArchiver before) throws Exception {
        FreeStyleProject p = createFreeStyleProject();
        p.getPublishersList().add(before);
        configRoundtrip(p);
        assertEqualDataBoundBeans(before, p.getPublishersList().get(JavadocArchiver.class));
    }
}
