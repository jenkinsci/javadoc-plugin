/*
 * The MIT License
 * 
 * Copyright (c) 2004-2009, Sun Microsystems, Inc., Kohsuke Kawaguchi, Martin Eigenbrodt, Peter Hayes
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.tasks;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.Extension;
import hudson.EnvVars;
import hudson.model.*;
import hudson.util.FormValidation;
import hudson.tasks.javadoc.Messages;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.AncestorInPath;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import jenkins.model.RunAction2;
import jenkins.tasks.SimpleBuildStep;

/**
 * Saves Javadoc for the project and publish them.
 *
 * This class is in a wrong package for a historical reason.
 *
 * @author Kohsuke Kawaguchi
 */
public class JavadocArchiver extends Recorder implements SimpleBuildStep {
    
    static final String HELP_PNG = "help.png";

    private static final String NOFRAMES_INDEX = "overview-summary.html";

    /**
     * Path to the Javadoc directory in the workspace.
     */
    private final String javadocDir;
    /**
     * If true, retain javadoc for all the successful builds.
     */
    private final boolean keepAll;
    
    @DataBoundConstructor
    public JavadocArchiver(String javadocDir, boolean keepAll) {
        this.javadocDir = javadocDir;
        this.keepAll = keepAll;
    }

    public String getJavadocDir() {
        return javadocDir;
    }

    public boolean isKeepAll() {
        return keepAll;
    }

    /**
     * Gets the directory where the Javadoc is stored for the given project.
     */
    private static File getJavadocDir(Job<?,?> project) {
        return new File(project.getRootDir(),"javadoc");
    }

    /**
     * Gets the directory where the Javadoc is stored for the given build.
     */
    private static File getJavadocDir(Run run) {
        return new File(run.getRootDir(),"javadoc");
    }

    @Override public void perform(Run<?,?> build, FilePath workspace, Launcher launcher, TaskListener listener) throws InterruptedException, IOException {
        listener.getLogger().println(Messages.JavadocArchiver_Publishing());

        EnvVars env = build.getEnvironment(listener);
        
        FilePath javadoc = workspace.child(env.expand(javadocDir));
        FilePath target = new FilePath(keepAll ? getJavadocDir(build) : getJavadocDir(build.getParent()));

        try {
            if (javadoc.copyRecursiveTo("**/*",target)==0) {
                final Result result = build.getResult();
                if(result == null || result.isBetterOrEqualTo(Result.UNSTABLE)) {
                    // If the build failed, don't complain that there was no javadoc.
                    // The build probably didn't even get to the point where it produces javadoc.
                    // If the build is running (AnyBuildStep, etc.), we also fail it
                    listener.error(Messages.JavadocArchiver_NoMatchFound(javadoc,javadoc.validateAntFileMask("**/*")));
                }
                build.setResult(Result.FAILURE);
                return;
            }
        } catch (IOException e) {
            Util.displayIOException(e,listener);
            e.printStackTrace(listener.fatalError(Messages.JavadocArchiver_UnableToCopy(javadoc,target)));
            build.setResult(Result.FAILURE);
             return;
        }
        
        build.addAction(new JavadocBuildAction());
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }
    
    protected static abstract class BaseJavadocAction implements Action {
        public String getUrlName() {
            return "javadoc";
        }

        public String getDisplayName() {
            File dir = dir();
            if (dir != null && new File(dir, "help-doc.html").exists())
                return Messages.JavadocArchiver_DisplayName_Javadoc();
            else
                return Messages.JavadocArchiver_DisplayName_Generic();
        }

        public String getIconFileName() {
            File dir = dir();
            if(dir != null && dir.exists())
                return HELP_PNG;
            else
                // hide it since we don't have javadoc yet.
                return null;
        }

        /**
         * Serves javadoc.
         */
        public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {
            DirectoryBrowserSupport dbs = new DirectoryBrowserSupport(this, new FilePath(dir()), getTitle(), HELP_PNG, false);
            if (new File(dir(), NOFRAMES_INDEX).exists() && Boolean.valueOf(
                    System.getProperty(JavadocArchiver.class.getName() + ".useFramelessIndex", "true"))) {
                /* If an overview-summary.html exists, serve that, unless the system property evaluates to false */
                dbs.setIndexFileName(NOFRAMES_INDEX);
            }
            dbs.generateResponse(req, rsp, this);
        }

        protected abstract String getTitle();

        protected abstract File dir();
    }

    public static class JavadocAction extends BaseJavadocAction implements ProminentProjectAction {
        private final Job<?,?> project;

        @Deprecated public JavadocAction(AbstractItem project) {
            this((Job) project);
        }

        public JavadocAction(Job<?,?> project) {
            this.project = project;
        }

        protected File dir() {
            Run<?,?> run = project.getLastSuccessfulBuild();
            if (run != null) {
                File javadocDir = getJavadocDir(run);
                if (javadocDir.exists()) {
                    return javadocDir;
                }
            }
            return getJavadocDir(project);
        }

        protected String getTitle() {
            return project.getDisplayName()+" javadoc";
        }
    }
    
    public static class JavadocBuildAction extends BaseJavadocAction implements RunAction2, SimpleBuildStep.LastBuildAction {

    	private transient Run<?,?> build;

        public JavadocBuildAction() {}

        @Deprecated
    	public JavadocBuildAction(AbstractBuild<?,?> build) {
    	    this.build = build;
    	}

        @Override public void onAttached(Run<?,?> r) {
            build = r;
        }

        @Override public void onLoad(Run<?,?> r) {
            build = r;
        }

        protected String getTitle() {
            return build.getDisplayName()+" javadoc";
        }

        protected File dir() {
            return getJavadocDir(build);
        }

        @Override public Collection<? extends Action> getProjectActions() {
            return Collections.singleton(new JavadocAction(build.getParent()));
        }

    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public String getDisplayName() {
            return Messages.JavadocArchiver_DisplayName();
        }

        /**
         * Performs on-the-fly validation on the file mask wildcard.
         */
        public FormValidation doCheckJavadocDir(@AncestorInPath AbstractProject project, @QueryParameter String value) throws IOException, ServletException {
            if (project == null) {
                return FormValidation.ok();
            }
            FilePath ws = project.getSomeWorkspace();
            return ws != null ? ws.validateRelativeDirectory(value) : FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
