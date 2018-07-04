package fit.plugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.Date;

import org.apache.commons.io.IOUtils;

import fit.FileRunner;

/**
 * FileRunner which uses a specified charset to read and write files.
 */
public class FitFileRunner extends FileRunner {
	private Charset srcCs;
	private Charset outCs;
	public FitFileRunner(Charset srcCs, Charset outCs) {
		this.srcCs = srcCs;
		this.outCs = outCs;
	}

	@Override
	public void args(String[] argv) throws IOException {
		if (argv.length != 2) {
			System.err.println("usage: java fit.FileRunner input-file output-file");
			System.exit(-1);
		}
		File in = new File(argv[0]);
		File out = new File(argv[1]);
		fixture.summary.put("input file", in.getAbsolutePath());
		fixture.summary.put("input update", new Date(in.lastModified()));
		fixture.summary.put("output file", out.getAbsolutePath());
		input = read(in);
		output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(out), outCs)));
	}

	@Override
	protected String read(File input) throws IOException {
		try (InputStream in = new FileInputStream(input)) {
			String s = IOUtils.toString(in, srcCs);
			return s;
		}
	}

}
