/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.uniqueculture;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Sergei Izvorean <uniqueculture at gmail.com>
 */
public class GitPropertiesStoreTest {
    

    GitPropertiesStore store;

    @Before
    public void setUp() throws IOException, GitAPIException {
        store = new GitPropertiesStore(System.getProperty("java.io.tmpdir") + File.separator + GitPropertiesStoreTest.class.getSimpleName());
    }

    @After
    public void tearDown() throws Exception {
        store.close();
        FileUtils.deleteDirectory(new File(System.getProperty("java.io.tmpdir") + File.separator + GitPropertiesStoreTest.class.getSimpleName()));
    }

    /**
     * Test of get method, of class GitPropertiesStore.
     */
    @Test
    public void testGet() throws IOException, GitAPIException {
        // Add and test again
        Properties testProps = new Properties();
        testProps.setProperty("hello", "world");
        store.store("test.properties", testProps);

        // Read
        Properties storedProps = store.get("test.properties");
        assertEquals("world", storedProps.getProperty("hello"));
    }

    /**
     * Test of list method, of class GitPropertiesStore.
     */
    @Test
    public void testList() throws IOException, GitAPIException {
        // Create a number of prop files
        store.store("list/1.properties", new Properties());
        store.store("list/2.properties", new Properties());

        Collection<Properties> props = store.list("list/");
        assertEquals(2, props.size());
    }

    /**
     * Test of store method, of class GitPropertiesStore.
     */
    @Test
    public void testStore() throws Exception {
        Properties p = new Properties();
        p.setProperty("hello", "world");
        store.store("p.properties", p);

        Properties p1 = new Properties();
        p1.setProperty("hello", "all");
        store.store("p.properties", p1);

        int commits = 0;
        // find the HEAD
        ObjectId lastCommitId = store.getGit().getRepository().resolve(Constants.HEAD);
        // a RevWalk allows to walk over commits based on some filtering that is defined
        try (RevWalk revWalk = new RevWalk(store.getGit().getRepository())) {
            RevCommit commit = revWalk.parseCommit(lastCommitId);
            // Mark HEAD as start
            revWalk.markStart(commit);

            for (RevCommit c : revWalk) {
                commits++;
                System.out.println("Comment: " + c.getFullMessage());
            }

            revWalk.dispose();
        }
        
        assertEquals(2, commits);

        Properties p2 = store.get("p.properties");
        assertEquals("all", p2.getProperty("hello"));

    }

    /**
     * Test of delete method, of class GitPropertiesStore.
     */
    @Test
    public void testDelete() throws Exception {
    }

    /**
     * Test of close method, of class GitPropertiesStore.
     */
    @Test
    public void testClose() throws Exception {
    }

    
}
