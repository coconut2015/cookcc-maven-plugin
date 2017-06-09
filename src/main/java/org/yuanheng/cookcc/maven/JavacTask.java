package org.yuanheng.cookcc.maven;

import java.io.File;
import java.util.HashSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;

class JavacTask implements Task
{
	private String m_src;
	private Option[] m_options;

	JavacTask (String src, Option[] options)
	{
		m_src = src;
		m_options = options;
	}

	public void execute (Log log, Compiler compiler, CompilerConfiguration config, String javaSrcDir, String cookccPath) throws MojoExecutionException
	{
		File file = new File (m_src);
		if (!file.isFile ())
		{
			throw new MojoExecutionException ("File " + m_src + " does not exist.");
		}
		try
		{
			for (Option option : m_options)
			{
				if (option.arg  == null)
					config.addCompilerCustomArgument ("-A" + option.opt, null);
				else
					config.addCompilerCustomArgument ("-A" + option.opt + "=" + option.arg, null);
			}
			HashSet<File> set = new HashSet<File> ();
			log.info ("CookCC is processing " + m_src);
			set.add (new File (m_src));
			config.setSourceFiles (set);

			String[] args = compiler.createCommandLine (config);
			String msg = "javac";
			for (String arg : args)
				msg += " " + arg;
			log.debug (msg);

			compiler.performCompile (config);
		}
		catch (CompilerException ex)
		{
			throw new MojoExecutionException ("Error compiling code", ex);
		}
	}
}
