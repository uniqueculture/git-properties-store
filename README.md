# Java Properties Git Store

Basic Git-based storage for Java Properties.

# Dependencies
Mainly the implementation depends on JGit

    <!-- JGit -->
    <dependency>
         <groupId>org.eclipse.jgit</groupId>
         <artifactId>org.eclipse.jgit</artifactId>
         <version>5.1.3.201810200350-r</version>
    </dependency>
    
    <!-- Logging -->
    <dependency>
         <groupId>org.slf4j</groupId>
         <artifactId>log4j-over-slf4j</artifactId>
         <version>1.7.25</version>
    </dependency>

# Usage

    // Init
    GitPropertiesStore store = new GitPropertiesStore(System.getProperty("java.io.tmpdir") + File.separator + GitPropertiesStoreTest.class.getSimpleName());
    
    // Store
    Properties testProps = new Properties();
    testProps.setProperty("hello", "world");
    store.store("test.properties", testProps);
    
    // Load
    Properties storedProps = store.get("test.properties");