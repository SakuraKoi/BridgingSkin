package ldcr.BridgingSkin;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

public class FileUtils {
    public static String readFile(final File file) throws IOException {
	final FileInputStream fin = new FileInputStream(file);
	return readFile(fin);
    }
    public static String readFile(final InputStream stream) throws IOException {
	final ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	final byte[] buffer = new byte[4096];
	int bytesRead;
	while ((bytesRead = stream.read(buffer)) >= 0) {
	    outStream.write(buffer, 0, bytesRead);
	}
	stream.close();
	return outStream.toString();
    }
    public static void copyFile(final File from,final File to) throws IOException
    {
	final FileInputStream inStream = new FileInputStream(from);
	final FileOutputStream outStream = new FileOutputStream(to);
	final byte[] buffer = new byte[4096];
	int bytesRead;
	while ((bytesRead = inStream.read(buffer)) >= 0) {
	    outStream.write(buffer, 0, bytesRead);
	}
	outStream.flush();
	inStream.close();
	outStream.close();
    }
    public static void writeFile(final File file,final String data) throws IOException
    {
	final FileWriter writer = new FileWriter(file);
	writer.write(data);
	writer.flush();
	writer.close();
    }

    public static void writeFile(final File configFile, final InputStream stream) throws IOException {
	writeFile(configFile,readFile(stream));
    }
}
