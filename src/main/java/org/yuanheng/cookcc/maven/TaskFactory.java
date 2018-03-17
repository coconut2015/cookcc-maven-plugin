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

import java.util.ArrayList;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * @author	Heng Yuan
 */
class TaskFactory
{

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

		boolean isJava = src.endsWith (".java");

		String str;
		ArrayList<Option> options = new ArrayList<Option> ();

		if (getBoolean (taskXml, "debug"))
			options.add (new Option ("debug", null));

		if (getBoolean (taskXml, "defaultReduce"))
			options.add (new Option ("defaultreduce", null));

		if (getBoolean (taskXml, "lexerAnalysis"))
			options.add (new Option ("lexeranalysis", null));

		if (getBoolean (taskXml, "analysis"))
			options.add (new Option ("analysis", null));

		if ((str = taskXml.getAttribute ("lexerTable")) != null)
			options.add (new Option ("lexertable", str));
		if ((str = taskXml.getAttribute ("parserTable")) != null)
			options.add (new Option ("parsertable", str));

		if (!isJava)
		{
			// deal with java specific options here
			if (getBoolean (taskXml, "public"))
				options.add (new Option ("public", null));
			if (getBoolean (taskXml, "abstract"))
				options.add (new Option ("abstract", null));
			if ((str = taskXml.getAttribute ("extend")) != null)
				options.add (new Option ("extend", str));
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
				options.add (new Option (name.substring (1), value));
			}
		}

		Option[] optionA = options.toArray (new Option[options.size ()]);
		if (isJava)
			return new JavacTask (src, optionA);
		else
			return new XccTask (src, optionA);
	}
}
