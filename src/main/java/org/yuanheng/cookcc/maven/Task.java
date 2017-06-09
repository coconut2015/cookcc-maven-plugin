package org.yuanheng.cookcc.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.compiler.Compiler;
import org.codehaus.plexus.compiler.CompilerConfiguration;

interface Task
{
	public void execute (Log log, Compiler compiler, CompilerConfiguration config, String javaSrcDir, String cookccPath) throws MojoExecutionException;
}
