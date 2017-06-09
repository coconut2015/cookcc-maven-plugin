package org.yuanheng.cookcc.maven;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.manager.CompilerManager;
import org.codehaus.plexus.compiler.manager.NoSuchCompilerException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

@Mojo (name = "run")
public class CookCCMojo extends AbstractMojo
{
	public final static String PROCESSOR = "org.yuanheng.cookcc.input.ap.CookCCProcessor";

	/**
	 * Plexus compiler manager.
	 */
	@Component
	private CompilerManager compilerManager;
	@Parameter (property = "maven.compiler.compilerId", defaultValue = "javac")
	private String compilerId;
	@Parameter (property = "maven.compiler.compilerVersion")
	private String compilerVersion;

	/**
	 * The source directories containing the sources to be compiled.
	 */
	@Parameter (defaultValue = "${project.compileSourceRoots}", readonly = true, required = true)
	private List<String> compileSourceRoots;

	@Parameter (defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@Component
	private RepositorySystem repositorySystem;

	@Parameter (property = "source", defaultValue = "1.8")
	private String source;

	@Parameter (property = "target", defaultValue = "1.8")
	private String target;

	@Parameter (property = "verbose")
	private boolean verbose;

	@Parameter
	private PlexusConfiguration tasks;

	@Parameter (defaultValue = "${project}", readonly = true, required = true)
	private MavenProject m_project;

	private CompilerConfiguration createBaseCompilerConfig (String cookccPath) throws MojoExecutionException
	{
		CompilerConfiguration config = new CompilerConfiguration ();
		
		String classDir = m_project.getBuild ().getOutputDirectory ();

		// javac -d option
		config.setOutputLocation (classDir);
		// source version
		config.setSourceVersion (source);
		// target version
		config.setTargetVersion (target);
		// disable warining of bootstrap class path not set in conjunction with -source
		config.addCompilerCustomArgument ("-Xlint:-options", null);
		// set the working directory
		config.setWorkingDirectory (new File (classDir));

		// annotation processing only
		config.setProc ("only");
		// set the annotation process to CookCC
		config.setAnnotationProcessors (new String[]{ PROCESSOR });
		// add the CookCC Jar to the class path.
		config.addClasspathEntry (cookccPath);
		// add the class output directory to the class path as well.
		config.addClasspathEntry (classDir);
		// also add source directories
		String src = getJavaSrcDir ();
		if (src == null)
		{
			throw new MojoExecutionException ("Unable to locate Java source directory.");
		}
		config.addSourceLocation (src);
		// set the CookCC output directory to be the java source directory.
		config.addCompilerCustomArgument ("-Ad=" + compileSourceRoots.get (0), null);

		// always forking
		config.setFork (true);
		config.setVerbose (verbose);
		return config;
	}

	public String getJavaSrcDir ()
	{
		for (String src : compileSourceRoots)
		{
			if (src.endsWith ("/java"))
			{
				return src;
			}
		}
		return null;
	}

	public void execute () throws MojoExecutionException
	{
		// Get the compiler
		Compiler compiler;
		try
		{
			compiler = compilerManager.getCompiler (compilerId);
		}
		catch (NoSuchCompilerException ex)
		{
			throw new MojoExecutionException ("no compiler found", ex);
		}

		// Check the dependencies and find CookCC being used.
		Dependency cookccDependency = null;
		for (Dependency dependency : m_project.getDependencies ())
		{
			if (!("cookcc".equals (dependency.getArtifactId ()) && "org.yuanheng.cookcc".equals (dependency.getGroupId ())))
				continue;
			if ("0.3.3".equals (dependency.getVersion ()))
			{
				throw new MojoExecutionException ("CookCC version needs to be 0.4.0 and later.");
			}
			cookccDependency = dependency;
			break;
		}
		if (cookccDependency == null)
		{
			throw new MojoExecutionException ("No cookcc dependency found.");
		}
		Artifact artifact = repositorySystem.createDependencyArtifact (cookccDependency);
		session.getLocalRepository ().find (artifact);
		File cookccJar = artifact.getFile ();
		String cookccPath = null;
		if (cookccJar != null && cookccJar.isFile ())
		{
			try
			{
				cookccPath = cookccJar.getCanonicalPath ();
			}
			catch (IOException ex)
			{
			}
		}
		if (cookccPath == null)
		{
			throw new MojoExecutionException ("Unable to locate CookCC jar.");
		}

		if (tasks == null || tasks.getChildCount () == 0)
		{
			throw new MojoExecutionException ("No CookCC tasks.");
		}

		PlexusConfiguration[] children = tasks.getChildren ();
		Log log = getLog ();
		for (PlexusConfiguration child : children)
		{
			if (!"task".equals (child.getName ()))
			{
				continue;
			}
			Task t = TaskFactory.getTask (child);
			t.execute (log, compiler, createBaseCompilerConfig (cookccPath), getJavaSrcDir (), cookccPath);
		}

	}
}
