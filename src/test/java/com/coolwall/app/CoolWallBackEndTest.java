package com.coolwall.app;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.framework.TestResult;

import spark.Spark;

/*
    The main test class, adds all the tests from the other
    classes to a test suite and runs them.
*/
public class CoolWallBackEndTest extends TestCase {
    private static TestSuite allTests;
    
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public CoolWallBackEndTest( String testName ) {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        allTests = new TestSuite();
        allTests.addTestSuite(Connection.class);
        allTests.addTestSuite(User.class);
        
        return allTests;
    }

    /*
        Specify which test to run
    */
    public void runTest() {
        allTests.run(new TestResult());
    }

    public void tearDown() {
        Spark.stop();
    }
}
