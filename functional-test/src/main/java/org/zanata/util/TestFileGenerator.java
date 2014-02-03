/*
 * Copyright 2013, Red Hat, Inc. and individual contributors as indicated by the
 * @author tags. See the copyright.txt file in the distribution for a full
 * listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package org.zanata.util;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import lombok.Setter;

/**
 * Create and manipulate basic text files for testing.
 *
 * @author Damian Jansen <a
 *         href="mailto:djansen@redhat.com">djansen@redhat.com</a>
 */
public class TestFileGenerator {
    // Length is maximum filename length - 4 (.xxx) - 19 (for tmp file
    // randomness)
    private static String longFileName =
            "lRRDXddgEnKzT2Wpu3VfT3Zs4pYuPXaqorA1CAtGcaZq6xydHdOghbsy"
                    + "Pu5GnbbmknPNRZ0vc7IEaiPm59CBQ9NkIH1if9Y4uHHYgjWJT8Yhs5qibcEZDNAZwLmDNHaRJhQr2Y1z3VslMFGGS"
                    + "P25eqzU1lDjejCsd26wRhT1UOkbhRRlm0ybGk8lTQgHEqT9sno1Veuw8A0StLGDfHAmCDFcUzAz9HMeuMUn9nFW";

    public TestFileGenerator() {
    }

    /**
     * Return a string with near maximum filename length
     *
     * @return String
     */
    public String longFileName() {
        return longFileName;
    }

    /**
     * Create a test file in temporary storage with content. Note that the file
     * will contain random characters from the temporary file creation process.
     *
     * @param fileName
     *            Prefix of file eg. "myTest"
     * @param suffix
     *            Suffix of file, eg. ".txt"
     * @param content
     *            Contents of the file, eg. "This is a test file"
     * @return File object for created file
     */
    public File generateTestFileWithContent(String fileName, String suffix,
            String content) {
        File tempFile = generateTestFile(fileName, suffix);
        setTestFileContent(tempFile, content);
        return tempFile;
    }

    private File generateTestFile(String fileName, String suffix) {
        File testFile;
        try {
            testFile = File.createTempFile(fileName, suffix);
        } catch (IOException ioException) {
            throw new RuntimeException("Unable to create temporary file "
                    + fileName);
        }
        testFile.deleteOnExit();
        return testFile;
    }

    private void setTestFileContent(File testFile, String testContent) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream(testFile),
                    Charset.forName("UTF-8").newEncoder());
            outputStreamWriter.write(testContent
                    .replaceAll("\n", System.getProperty("line.separator")));
            outputStreamWriter.flush();
            outputStreamWriter.close();
        } catch (IOException ioException) {
            throw new RuntimeException("Could not open file for writing "
                    + testFile.getName());
        }
    }

    /**
     * Change the size of a file to fileSize. The target file will be truncated
     * or extended as necessary.
     *
     * @param tempFile
     *            File to alter
     * @param fileSize
     *            Intended file size of resulting file
     * @throws RuntimeException
     *             if the file cannot be altered
     */
    public void forceFileSize(File tempFile, long fileSize) {
        try {
            RandomAccessFile randomAccessFile =
                    new RandomAccessFile(tempFile, "rw");
            randomAccessFile.setLength(fileSize);
        } catch (IOException e) {
            throw new RuntimeException("Unable to set the test file length");
        }
    }

    /**
     * Get the contents of the given file.
     *
     * @param testFile
     *            File to read contents from
     * @return contents of the target file
     * @throws RuntimeException
     *             if the file cannot be read
     */
    public String getTestFileContent(File testFile) {
        String fileContents;
        try {
            fileContents = FileUtils.readFileToString(testFile);
        } catch (IOException ioException) {
            throw new RuntimeException("Could not read from test file.");
        }
        return fileContents;
    }

    /**
     * Gives the name of the first listed file in a directory. Intended for
     * validating testing upload of files to an empty directory.
     *
     * @param directory
     *            Storage directory of desired file.
     * @return name of first listed file in target directory
     * @throws RuntimeException
     *             if no files are found
     */
    public String getFirstFileNameInDirectory(String directory) {
        try {
            return new File(directory).list()[0];
        } catch (ArrayIndexOutOfBoundsException arrayException) {
            throw new RuntimeException("Expected files in dir " + directory
                    + " but none found.");
        }
    }

    /**
     * Generates a zanata.xml with url default to test instance.
     *
     * @param output where to write it
     * @param projectSlug project slug
     * @param versionSlug version slug
     * @param projectType project type
     * @param locales locales
     */
    public void generateZanataXml(File output, String projectSlug, String versionSlug, String projectType, List<String> locales) {
        ZanataXml zanataXml = new ZanataXml();
        zanataXml.setProject(projectSlug);
        zanataXml.setProjectVersion(versionSlug);
        zanataXml.setProjectType(projectType);
        zanataXml.setLocales(locales);
        marshall(output, zanataXml, ZanataXml.class);
    }

    private static <T> void marshall(File output, T object, Class<T> xmlClass) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.marshal(object, output);
        }
        catch (JAXBException e) {
            throw Throwables.propagate(e);
        }
    }

    @XmlRootElement(namespace = "http://zanata.org/namespace/config/",
            name = "config")
    @Setter
    private static class ZanataXml {
        @XmlElement
        private String url = PropertiesHolder
                .getProperty(Constants.zanataInstance.value());
        @XmlElement
        private String project;
        @XmlElement(name = "project-version")
        private String projectVersion;
        @XmlElement(name = "project-type")
        private String projectType;
        @XmlElementWrapper(name="locales")
        @XmlElements(
                @XmlElement(name = "locale")
        )
        private List<String> locales;
    }

    public File openTestFile(String filename) {
        String relativePath = "functional-test/target/test-classes/"
                .concat(filename);
        File testFile = new File(relativePath);
        assert testFile.exists();
        return testFile;
    }
}
