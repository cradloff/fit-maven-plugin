package fit.plugin;

import java.io.BufferedWriter;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.codehaus.plexus.classworlds.realm.ClassRealm;

import fit.Counts;
import fit.Fixture;
import fit.Fixture.RunTime;
import fit.Parse;
import fit.Summary;

/**
 * Goal which executes all Fit Tests.
 */
@Mojo(name="run", defaultPhase=LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public class FitMojo extends AbstractMojo {
	private static final String STYLESHEET = "fixture.css";

	/** Location of source directory. */
	@Parameter(defaultValue = "src/test/fit")
	private File sourceDirectory;

	/** Location of output directory. */
	@Parameter(defaultValue = "${project.build.directory}/fit")
	private File outputDirectory;

	/** specify whether sourceIncludes is case sensitive. */
	@Parameter(defaultValue = "true")
	private boolean caseSensitive;

	/** Pattern for Fit Tests as CSV */
	@Parameter(defaultValue = "*.html")
	private String sourceIncludes;

	/** ignore failures? */
	@Parameter(defaultValue = "false")
	private boolean ignoreFailures;

	/** encoding of input files */
	@Parameter(defaultValue="${project.build.sourceEncoding}")
	private String sourceEncoding;

	/** encoding of output files */
	@Parameter(defaultValue="${project.reporting.outputEncoding}")
	private String outputEncoding;

	@Parameter(defaultValue="${project.testClasspathElements}", required=true)
	private List<String> classpathElements;

	private int countFiles;
	private int countDirectories;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info(String.format("Executing fit tests (%s) in %s", sourceIncludes, sourceDirectory.getPath()));
		getLog().info(String.format("Writing results to %s", outputDirectory.getPath()));
		try {
			getLog().info(String.format("Current working directory: %s", new File(".").getCanonicalPath()));
		} catch (IOException e) {
			throw new MojoExecutionException("error executing fit tests", e);
		}
		ensureExists(outputDirectory);

		if (! sourceDirectory.exists()) {
			throw new MojoFailureException(String.format("source directory %s does not exist!", sourceDirectory.getPath()));
		}
		File summaryFile = new File(outputDirectory, "summary.html");
		Charset cs = Charsets.toCharset(outputEncoding);
		try (OutputStream fos = new FileOutputStream(summaryFile);
				Writer writer = new BufferedWriter(new OutputStreamWriter(fos, cs));
				PrintWriter out = new PrintWriter(writer, true)) {
			setupClassloader();

			// copy stylesheet
			File stylesheet = new File(sourceDirectory, STYLESHEET);
			if (stylesheet.exists()) {
				try (InputStream is = new FileInputStream(stylesheet);
					OutputStream os = new FileOutputStream(outputPath(stylesheet).toFile())) {
					IOUtils.copy(is, os);
				}
			}

			Counts counts = process(out, cs);

			getLog().info("Result: " + counts);
			if ((counts.exceptions > 0 || counts.wrong > 0)
					&& ! ignoreFailures) {
				throw new MojoFailureException("fit tests failed: " + counts);
			}
		} catch (IOException | ParseException e) {
			throw new MojoExecutionException("error executing fit tests", e);
		}
	}

	@SuppressWarnings("unchecked")
	private Counts process(PrintWriter out, Charset cs)
			throws IOException, MojoExecutionException, ParseException {
		Summary summary = new Summary();
		summary.summary.put("run date", new Date());
		summary.summary.put("run elapsed time", summary.new RunTime());
		out.printf("<?xml version='1.0' encoding='%s'?>%n", cs.name());
		out.println("<!DOCTYPE html PUBLIC '-//W3C//DTD XHTML 1.0 Transitional//EN' 'http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd'>");
		out.println("<html xmlns='http://www.w3.org/1999/xhtml'>");
		out.println("<html>");
		out.println("<head>");
		out.printf("<meta http-equiv='Content-Type' content='text/html; charset=%s' />%n", cs.name());
		out.println("<title>Summary</title>");
		out.printf("<link rel='stylesheet' href='%s'/>%n", STYLESHEET);
		out.println("</head>");
		out.println("<body>");
		out.println("<table>");
		out.println("<caption>Summary</caption>");
		out.println("<tr class='head'><td>path</td><td>right</td><td>wrong</td><td>exceptions</td><td>run elapsed time</td><td>description</td></tr>");

		Counts counts = processDirectory(sourceDirectory, out);

		out.println("</table>");
		Parse parseSummary = new Parse("<table><caption>Summary</caption><tr class='head'><td colspan='2'>fit.Summary</td></tr></table>");
		summary.counts = counts;
		summary.summary.put("files processed", countFiles);
		summary.summary.put("directories processed", countDirectories);
		summary.doTable(parseSummary);
		parseSummary.print(out);
		out.println("</body>");
		out.println("</html>");

		return counts;
	}

	private void setupClassloader() throws MalformedURLException {
		PluginDescriptor pluginDescriptor = (PluginDescriptor) getPluginContext().get("pluginDescriptor");
		ClassRealm classRealm = pluginDescriptor.getClassRealm();
		for (URL url : getClasspathURLs()) {
			classRealm.addURL(url);
		}
	}

	private URL[] getClasspathURLs() throws MalformedURLException {
		List<URL> urls = new ArrayList<>();
		if (classpathElements != null) {
			for (String classpathElement : classpathElements) {
				urls.add(new File(classpathElement).toURI().toURL());
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	private Counts processDirectory(File directory, PrintWriter out) throws IOException, MojoExecutionException {
		ensureExists(outputPath(directory).toFile());
		countDirectories++;

		// process files
		out.printf("<tr class='highlight'><td colspan='6'>%s</td></tr>%n", directory.getPath());
		FileFilter filter = new WildcardFileFilter(sourceIncludes, caseSensitive ? IOCase.SENSITIVE : IOCase.INSENSITIVE);
		Counts counts = new Counts();
		RunTime runtime = new Fixture().new RunTime();
		for (File f : list(directory, filter)) {
			counts.tally(processFile(out, f));
			countFiles++;
		}
		printSummary(out, "<strong>Summary</strong>", "", counts, runtime);

		// process directories
		for (File f : list(directory, DirectoryFileFilter.DIRECTORY)) {
			counts.tally(processDirectory(f, out));
		}

		return counts;
	}

	private Counts processFile(PrintWriter out, File f) throws IOException {
		Charset srcCs = Charsets.toCharset(sourceEncoding);
		Charset outCs = Charsets.toCharset(outputEncoding);
		FitFileRunner fr = new FitFileRunner(srcCs, outCs);
		fr.args(new String[] { f.getPath(), outputPath(f).toString() });
		fr.process();
		fr.output.close();
		String description = "";
		int start = fr.input.indexOf("<title>");
		if (start > 0) {
			int end = fr.input.indexOf("</title>", start);
			description = fr.input.substring(start + "<title>".length(), end);
		}
		Path relative = outputDirectory.toPath().relativize(outputPath(f));
		String link = String.format("<a href='%s'>%s</a>", relative, f.getName());
		printSummary(out, link, description, fr.fixture.counts, (RunTime) fr.fixture.summary.get("run elapsed time"));

		return fr.fixture.counts;
	}

	private static final String RIGHT = "bgcolor='#cfffcf' class='number'";
	private static final String WRONG = "bgcolor='#ffcfcf' class='number'";
	private void printSummary(PrintWriter out, String name, String description, Counts counts, RunTime runtime) {
		out.printf("<tr><td>%s</td><td %s>%,d</td><td %s>%,d</td><td %s>%,d</td><td>%s</td><td>%s</td></tr>%n",
				name, RIGHT, counts.right, counts.wrong == 0 ? RIGHT : WRONG, counts.wrong,
						counts.exceptions == 0 ? RIGHT : WRONG, counts.exceptions, runtime, description);
	}

	private File[] list(File directory, FileFilter filter) {
		File[] files = directory.listFiles(filter);
		Arrays.sort(files);

		return files;
	}

	private void ensureExists(File f) {
		if (! f.exists()) {
			f.mkdirs();
		}
	}

	private Path outputPath(File f) {
		Path relative = sourceDirectory.toPath().relativize(f.toPath());
		return outputDirectory.toPath().resolve(relative);
	}
}
