/*
 * Copyright (c) 2017-2018 Heng Yuan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.yuanheng.cookcc.maven;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * This task run cookcc directly.  The most common use is to compile .xcc files.
 *
 * @author	Heng Yuan
 */
class XccTask implements Task
{
	private String m_src;
	private Option[] m_options;

	XccTask (String src, Option[] options)
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

		// Build arguments
		ArrayList<String> arguments = new ArrayList<String> ();
		for (Option option : m_options)
		{
			arguments.add ("-" + option.opt);
			if (option.arg != null)
			{
				arguments.add (option.arg);
			}
		}

		arguments.add ("-d");
		arguments.add (javaSrcDir);

		arguments.add (m_src);
		String[] args = arguments.toArray (new String[arguments.size ()]);

		String msg = "java -jar " + cookccPath;
		for (String arg : arguments)
			msg += " " + arg;
		log.debug (msg);

		// Load the jar
		File cookccJar = new File (cookccPath);
		if (!cookccJar.exists ())
		{
			throw new MojoExecutionException ("Failed to load " + cookccPath);
		}
		URLClassLoader cl = null;
		try
		{
			cl = new URLClassLoader (new URL[]{ cookccJar.toURI ().toURL () });
			Class<?> c = cl.loadClass ("org.yuanheng.cookcc.Main");
			Method method = c.getMethod ("main", String[].class);
			method.invoke (null, new Object[]{ args });
		}
		catch (Exception ex)
		{
			try
			{
				if (cl != null)
					cl.close ();
			}
			catch (IOException ex2)
			{
			}
			throw new MojoExecutionException ("Error executing CookCC", ex);
		}

		try
		{
			cl.close ();
		}
		catch (IOException ex)
		{
		}
	}

	private static boolean getBoolean (PlexusConfiguration xml, String attr)
	{
		if ("true".equals (xml.getAttribute (attr)))
			return true;
		return false;
	}

	public static XccTask getTask (PlexusConfiguration taskXml) throws MojoExecutionException
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

		return new XccTask (src, options.toArray (new Option[options.size ()]));
	}
}
