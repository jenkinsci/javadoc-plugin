package hudson.tasks.javadoc;

import hudson.model.FreeStyleProject;
import hudson.tasks.JavadocArchiver;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class JavadocArchiverTest {

    @Rule public JenkinsRule r = new JenkinsRule();

    @Test public void configRoundtrip() throws Exception {
        verify(new JavadocArchiver("foo", true));
        verify(new JavadocArchiver(".", false));
    }

    private void verify(JavadocArchiver before) throws Exception {
        FreeStyleProject p = r.createFreeStyleProject();
        p.getPublishersList().add(before);
        r.configRoundtrip(p);
        r.assertEqualDataBoundBeans(before, p.getPublishersList().get(JavadocArchiver.class));
    }
}
