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
import java.util.HashSet;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;
import org.codehaus.plexus.compiler.CompilerException;

/**
 * @author	Heng Yuan
 */
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
			m_src.replace ('/', File.separatorChar);
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
