package com.coolwall.app;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


/*
    This unit test tests the connection to the database
    of the main class. It initialises the main server class
    and ensures that a connection to the database is made.
*/
public class Connection extends TestCase {
    private CoolWallBackEnd toTest;
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public Connection( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        TestSuite suite = new TestSuite();
        suite.addTest(new Connection("testConnection"));
        return suite;
    }

    /*
        Initialise the server
    */
    protected void setUp() {
        toTest = new CoolWallBackEnd();
        try {
            Thread.sleep(5000); // Wait for spark to start
        }
        catch(InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void testConnection() {
        assertNotNull(toTest.getConnection());
    }
}
