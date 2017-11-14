/*
 * Copyright 2017 MojoHaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.codehaus.mojo.rip;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

@Mojo(
    name = "attach-formatter",
    defaultPhase = LifecyclePhase.PACKAGE,
    requiresDependencyResolution = ResolutionScope.TEST,
    threadSafe = true
)
public class AttachFormatterMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;

    @Component
    protected MavenProjectHelper projectHelper;

    @Parameter(property = "session", readonly = true, required = true)
    protected MavenSession mavenSession;
    
    @Parameter
    private Set<String> validPropertyPrefixes;

	public void execute() throws MojoExecutionException, MojoFailureException {
	    
	    // default valid prefixes
	    if ( validPropertyPrefixes == null || validPropertyPrefixes.isEmpty() ) {
	        validPropertyPrefixes = new HashSet<String>();
	        validPropertyPrefixes.add("org.eclipse.jdt.core.formatter");
	        validPropertyPrefixes.add("org.eclipse.jdt.core.javaFormatter");
	    }
	    
	    // eclipse.preferences.version is always valid
	    validPropertyPrefixes.add("eclipse.preferences.version");
	    
		File preferences = new File(project.getBuild().getOutputDirectory(), Constants.FN_CORE_PREFS);
		
		if ( !preferences.exists() ) {
			throw new MojoExecutionException("Did not find formatter file " + preferences.getAbsolutePath());
		}

		validateKeys(preferences);

		project.getArtifact().setFile(preferences);
	}

    private void validateKeys(File preferences) throws MojoExecutionException, MojoFailureException {

        Properties props = new Properties();
		InputStream in = null;
		try {
		    in = new BufferedInputStream(new FileInputStream(preferences));
		    props.load(in);
		    
		    keys: for ( Object obj : props.keySet() ) {
		        String key = (String) obj;
		        
		        for ( String validPropertyPrefix : validPropertyPrefixes ) {
		            if ( key.startsWith(validPropertyPrefix)) {
		                continue keys;
		            }
		        }
		        
                throw new MojoFailureException("Invalid property key '" + key
                        + "', expected to match one of the following prefixes: " + validPropertyPrefixes);
		        
		    }
		} catch (IOException e) {
		    throw new MojoExecutionException(e.getMessage(), e);
        } finally {
		    IOUtils.closeQuietly(in);
		}
    }
}
