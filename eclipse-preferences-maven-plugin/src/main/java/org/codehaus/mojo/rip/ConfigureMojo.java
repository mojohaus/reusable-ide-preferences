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

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.sonatype.plexus.build.incremental.BuildContext;

// TODO - smart merge of the files - do NOT remove settings which do no exist in our file, as the file holds more than formatter stuff
@Mojo(
    name = "configure",
    requiresDependencyResolution = ResolutionScope.TEST,
    threadSafe = true,
    defaultPhase = LifecyclePhase.GENERATE_RESOURCES
)
public class ConfigureMojo extends AbstractMojo {

    @Parameter(property = "project", readonly = true, required = true)
    protected MavenProject project;
	
	@Component
	private BuildContext buildContext;
	
	@Component
	private ProjectBuilder projectBuilder;
	
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;
    
    @Parameter
    private RepositoryConfig repository;
    
    @Parameter
    private String commitTemplate;

	public void execute() throws MojoExecutionException, MojoFailureException {
		
	    Artifact formatterArtifact = null;

		for (Artifact artifact : project.getDependencyArtifacts()) {
			if ( artifact.getType().equals(Constants.PACKAGING_TYPE_ECLIPSE_FORMATTER)) {
			    if ( formatterArtifact != null ) {
			        throw new MojoExecutionException("Found at least two dependencies of type " + Constants.PACKAGING_TYPE_ECLIPSE_FORMATTER + " : "
			                + toGavString(formatterArtifact) + ", " + toGavString(artifact));
			    }
			    
				getLog().info("Picked up formatter " + getPrefs(artifact));
				formatterArtifact = artifact;
			}
		}
		
		if ( formatterArtifact == null ) {
			getLog().info("No dependency of type " + Constants.PACKAGING_TYPE_ECLIPSE_FORMATTER +" found, skipping");
			return;
		}
		
		String formatterName = "Maven-Managed Formatter";
        try {
            formatterName = getDependencyProjectName(formatterArtifact);
        } catch (ProjectBuildingException e) {
            getLog().warn("Failed extracting formatter name, falling back to default value. Cause: " + e.getMessage()+ ". Switch to debug level for full stack trace");
            getLog().debug(e);
        }
        
		File settingsDir = new File(project.getBasedir(), ".settings");
		settingsDir.mkdir();
		
		OutputStream uiPrefs = null;
		try {
			uiPrefs = buildContext.newFileOutputStream(new File(settingsDir, Constants.FN_UI_PREFS));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(uiPrefs));
			writer.write("eclipse.preferences.version=1\n");
			writer.write("formatter_profile=_" + formatterName + "\n");
			writer.write("formatter_settings_version=1\n");
			writer.close();
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(uiPrefs);
		}
		
		OutputStream corePrefs = null;
		InputStream artifactPrefs = null;
		try {
			corePrefs = buildContext.newFileOutputStream(new File(settingsDir, Constants.FN_CORE_PREFS));
			artifactPrefs = new FileInputStream(getPrefs(formatterArtifact)); 
			IOUtils.copy( artifactPrefs, corePrefs);
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(corePrefs);
			IOUtils.closeQuietly(artifactPrefs);
		}
		
		if ( commitTemplate != null ) {
		    getLog().info("Writing Mylyn team preferences");
		    writeSettingsFile(new File(settingsDir, Constants.FN_MYLYN_TEAM_PREFS), 
		            "commit.comment.template=" +commitTemplate);
		}
		
		if ( RepositoryConfig.isValid(repository) ) {
		    getLog().info("Writing Mylyn tasks preferences");
		    writeSettingsFile(new File(settingsDir, Constants.FN_MYLYN_TASKS_PREFS), 
		            "project.repository.kind=" + repository.getKind(),
		            "project.repository.url=" + repository.getUrl());
		}
	}

    private String getDependencyProjectName(Artifact formatterArtifact) throws ProjectBuildingException {

        ProjectBuildingRequest currentRequest = session.getProjectBuildingRequest();
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(currentRequest);
        buildingRequest.setProject(null);
        MavenProject mavenProject = projectBuilder.build(formatterArtifact, buildingRequest).getProject();
        
        return mavenProject.getName();
    }

    private String toGavString(Artifact formatterArtifact) {

        return formatterArtifact.getGroupId() + ":" + formatterArtifact.getArtifactId() + ":"
                + formatterArtifact.getVersion();
    }
    
    private File getPrefs(Artifact formatterArtifact) {
        
        // m2e workspace build, useful mostly for testing
        if ( formatterArtifact.getFile().isDirectory() ) {
            return new File(formatterArtifact.getFile(), Constants.FN_CORE_PREFS);
        }

        return formatterArtifact.getFile();
    }  
    
    private void writeSettingsFile(File file, String... lines) throws MojoExecutionException {
        
        StringBuilder sb = new StringBuilder();
        sb.append("eclipse.preferences.version=1\n");
        for ( String line : lines ) {
            sb.append(line).append('\n');
        }
        
        try ( OutputStream teamPrefs = buildContext.newFileOutputStream(file); 
                InputStream in = new ByteArrayInputStream(sb.toString().getBytes())) {
            IOUtils.copy(in, teamPrefs);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
