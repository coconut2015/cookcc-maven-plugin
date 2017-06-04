package org.yuanheng.cookcc.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

class Task
{
	private String m_src;
	private Option[] m_options;

	private Task (String src, Option[] options)
	{
		m_src = src;
		m_options = options;
	}

	public void execute (Log log, Compiler compiler, CompilerConfiguration config) throws MojoExecutionException
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
				config.addCompilerCustomArgument (option.opt, option.arg);
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

	private static boolean getBoolean (PlexusConfiguration xml, String attr)
	{
		if ("true".equals (xml.getAttribute (attr)))
			return true;
		return false;
	}

	public static Task getTask (PlexusConfiguration taskXml) throws MojoExecutionException
	{
		String src = taskXml.getAttribute ("src");
		if (src == null)
		{
			throw new MojoExecutionException ("Missing src attribute.");
		}

		String str;
		ArrayList<Option> options = new ArrayList<Option> ();

		if (getBoolean (taskXml, "debug"))
			options.add (new Option ("-Adebug", null));

		if (getBoolean (taskXml, "defaultReduce"))
			options.add (new Option ("-Adefaultreduce", null));

		if (getBoolean (taskXml, "lexerAnalysis"))
			options.add (new Option ("-Alexeranalysis", null));

		if (getBoolean (taskXml, "analysis"))
			options.add (new Option ("-Aanalysis", null));

		if ((str = taskXml.getAttribute ("lexerTable")) != null)
			options.add (new Option ("-Alexertable", str));
		if ((str = taskXml.getAttribute ("parserTable")) != null)
			options.add (new Option ("-Aparsertable", str));

		if (src.endsWith (".java"))
		{
			// deal with java specific options here
			if (getBoolean (taskXml, "public"))
				options.add (new Option ("-Apublic", null));
			if (getBoolean (taskXml, "abstract"))
				options.add (new Option ("-Aabstract", null));
			if ((str = taskXml.getAttribute ("extend")) != null)
				options.add (new Option ("-Aextend=" + str, null));
		}

		if (taskXml.getChildCount () > 0)
		{
			for (PlexusConfiguration optionXml : taskXml.getChildren ())
			{
				if (!"option".equals (optionXml.getName ()))
					throw new MojoExecutionException ("Unknown tag: " + optionXml.getName ());
				String name = optionXml.getAttribute ("name");
				if (name == null)
					throw new MojoExecutionException ("<option> tag missing name attribute.");
				if (!name.startsWith ("-"))
					throw new MojoExecutionException ("Invalid option: " + name);
				String value = optionXml.getAttribute ("value");
				options.add (new Option ("-A" + name.substring (1) + "=" + value, null));
			}
		}

		return new Task (src, options.toArray (new Option[options.size ()]));
	}
}
