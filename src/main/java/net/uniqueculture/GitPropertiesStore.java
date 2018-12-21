/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.uniqueculture;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Sergei Izvorean <uniqueculture at gmail.com>
 */
public class GitPropertiesStore implements AutoCloseable {
    
    private static final Logger logger = LoggerFactory.getLogger(GitPropertiesStore.class);

    /**
     * Git client
     */
    final Git git;
    
    /**
     * Base directory; local
     */
    final File baseDir;

    public GitPropertiesStore(String basePath) throws IOException, GitAPIException {
        this.baseDir = new File(basePath);
        // Either init or use existing repository
        this.git = Git.init().setDirectory(baseDir).call();
    }

    /**
     * Get properties from a single file
     * 
     * @param relativeFilePath Relative to the base path file path
     * @return Properties stored in the file, {@code null} if unable to read 
     */
    public Properties get(String relativeFilePath) {
        logger.trace("Getting properties from {}", relativeFilePath);

        File file = new File(baseDir, relativeFilePath);
        if (!file.canRead()) {
            logger.warn("Properties file {} either does not exists or is unreadable.", relativeFilePath);
            return null;
        }

        Properties props = new Properties();
        try {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                props.load(inputStream);
            }
            return props;
        } catch (IOException ex) {
            logger.warn("Exception reading properties file " + relativeFilePath, ex);
        }

        return null;
    }

    /**
     * Collection of properties within a directory
     * 
     * @param relativeDirPath Relative to the base path directory path
     * @return Collection of properties within a directory, {@code null} if unable to read
     */
    public Collection<Properties> list(String relativeDirPath) {
        Map<String, Properties> props = listFiles(relativeDirPath);
        if (props == null) {
            return null;
        }

        return props.values();
    }
    
    /**
     * Map of file names and properties within them
     * 
     * @param relativeDirPath Relative to the base path directory path
     * @return Map of file names and properties within them, {@code null} if unable to read
     */
    public Map<String, Properties> listFiles(String relativeDirPath) {
        File file = new File(baseDir, relativeDirPath);
        if (!file.canRead()) {
            logger.warn("Properties directory {} either does not exists or is unreadable.", relativeDirPath);
            return null;
        }

        Map<String, Properties> props;
        if (file.isFile()) {
            logger.debug("Path {} is a file. List of 1 will be returned", relativeDirPath);
            // File path; return a list of one
            props = new HashMap<>(1);
            props.put(relativeDirPath, get(relativeDirPath));
        } else {
            // List all files
            File[] files = file.listFiles();
            logger.debug("Found {} files in {}", files.length, relativeDirPath);

            props = new HashMap<>(files.length);
            for (File f : files) {
                if (f.isFile()) {
                    props.put(f.getName(), get(relativeDirPath + File.separator + f.getName()));
                }
            }
        }

        return props;
    }

    /**
     * Store properties into a file and commit to git
     * 
     * @param relativeFilePath
     * @param properties
     * @throws IOException
     * @throws GitAPIException 
     */
    public void store(String relativeFilePath, Properties properties) throws IOException, GitAPIException {
        File file = new File(baseDir, relativeFilePath);
        boolean add = false;
        if (!file.exists()) {
            // Create an empty file
            add = true;

            // Create the directories if needed
            String parentDir = (new File(relativeFilePath)).getParent();
            if (parentDir != null) {
                try {
                    Files.createDirectories(Paths.get(baseDir.getPath(), parentDir));
                    logger.debug("Created parent directories {}", parentDir);
                } catch (IOException ex) {
                    logger.error("Exception creating parent directories for relative path " + relativeFilePath, ex);
                }
            }

            // Create the file
            if (!file.createNewFile()) {
                logger.error("Unable to create an empty file {} to store properties", relativeFilePath);
                return;
            }
        }

        // Write the properties to a file
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            properties.store(outputStream, null);
            logger.debug("Storred properties in {}", relativeFilePath);
        }
        
        // Stage in Git
        if (add) {
            git.add().addFilepattern(file.getAbsolutePath()).call();
        }

        // Commit to the repository
        git.commit().setAll(true).setMessage("Commit store of " + relativeFilePath).call();
        logger.debug("Committed propeties changes in {}", relativeFilePath);
    }

    /**
     * Delete properties file and commit to git
     * 
     * @param relativeFilePath Relative to base path file path
     * @return Properties stored within the deleted file, {@code null} if unable to delete
     * @throws GitAPIException 
     */
    public Properties delete(String relativeFilePath) throws GitAPIException {
        File file = new File(baseDir, relativeFilePath);
        if (!file.canWrite()) {
            logger.error("Properties file {} is not writable. Cannot delete.", relativeFilePath);
            return null;
        }

        if (file.isDirectory()) {
            logger.error("Properties path {} is a directory. Cannot delete.", relativeFilePath);
            return null;
        }

        Properties props = get(relativeFilePath);

        // Delete the file
        if (file.delete()) {
            git.commit().setAll(true).setMessage("Commit delete of " + relativeFilePath).call();
        }

        return props;
    }
    
    
    protected Git getGit() {
        return git;
    }
    
    /**
     * Close the JGit client
     * 
     * @throws Exception 
     */
    @Override
    public void close() throws Exception {
        if (this.git != null) {
            this.git.close();
        }
    }


}
