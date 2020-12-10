package com.sri.gradle.daikon;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class Constants {
  // plugin related constants
  public static final String GROUP = "Daikon";
  public static final String PLUGIN_DESCRIPTION =
      "Detection of likely program invariants using Daikon.";
  public static final String PLUGIN_EXTENSION = "runDaikon";
  public static final String PROJECT_LIB_DIR = "libs";
  public static final String PROJECT_MAIN_SRC_DIR = "src/main/java";
  public static final String PROJECT_TEST_SRC_DIR = "src/test/java";
  public static final String PROJECT_MAIN_CLASS_DIR = "classes/java/main";
  public static final String PROJECT_TEST_CLASS_DIR = "classes/java/test";

  // task related constants
  public static final String CHECK_DAIKON_TASK = "daikonCheck";
  public static final String CHECK_DAIKON_TASK_DESCRIPTION =
      "Checks if Daikon is in your project's classpath.";
  public static final String DAIKON_TASK = "runDaikon";
  public static final String DAIKON_TASK_DESCRIPTION = "Runs Daikon invariant detector";
  public static final String CODE_GEN_TASK = "generateTestDriverCode";
  public static final String CODE_GEN_TASK_DESCRIPTION =
      "Generates test driver code that Daikon can execute.";
  // Regular expression which matches expected names of JUnit test classes.
  // thx to https://github.com/sevntu-checkstyle/sevntu.checkstyle
  public static final Pattern EXPECTED_JUNIT4_NAME_REGEX =
      Pattern.compile(
          ".+Test\\d*|.+Tests\\d*|Test.+|Tests.+|.+IT|.+ITs|.+TestCase\\d*" + "|.+TestCases\\d*");
  public static final String COMPILE_TEST_DRIVER = "compileTestDriverJava";
  public static final String OWN_DRIVER = "driver";
  public static final String DAIKON_EVIDENCE_TASK = "daikonEvidence";
  public static final String DAIKON_EVIDENCE_TASK_DESCRIPTION = "Produces an evidence artifact containing the specific details of the Daikon execution.";
  public static final String DAIKON_DETAILS_FILE_NAME = "daikon-evidence.json";
  public static final Charset ENCODING = StandardCharsets.UTF_8;
  public static final String DAIKON_SPLITTER = "=====================";
  public static final String EVIDENCE_ONLY = "evidence.only";

  // tool related constants
  public static final String CHICORY_MAIN_CLASS = "daikon.Chicory";
  public static final String DAIKON_MAIN_CLASS = "daikon.Daikon";
  public static final String DYN_COMP_MAIN_CLASS = "daikon.DynComp";
  public static final String PRINT_INVARIANTS_MAIN_CLASS = "daikon.PrintInvariants";
  public static final String CHICORY_JAR_FILE = "ChicoryPremain.jar";
  public static final String DAIKON_JAR_FILE = "daikon.jar";
  public static final String DYN_COMP_PRE_MAIN_JAR_FILE = "dcomp_premain.jar";
  public static final String DYN_COMP_RT_JAR_FILE = "dcomp_rt.jar";
  public static final String TEST_DRIVER = "TestDriver";
  public static final String TEST_DRIVER_CLASSNAME = "AutoGenerated" + TEST_DRIVER;

  // general constants
  public static final String NEW_LINE = System.getProperty("line.separator");
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");
  public static final String PATH_SEPARATOR = System.getProperty("path.separator");
  public static final File USER_WORKING_DIR = new File(System.getProperty("user.dir"));
  public static final String SPACE = " ";
  public static final String DOUBLE_SPACE = SPACE + SPACE;
  public static final OutputStream QUIET_OUTPUT = new OutputStream() {
    @Override public void write(int b) throws IOException {
      // nothing;
    }
  };

  // error related constants
  public static final String BAD_DAIKON_ERROR = "Unable to run Daikon.";
  public static final String ERROR_MARKER = "Error: Could not find or load main";
  public static final String UNEXPECTED_ERROR =
      "Daikon is not installed on this machine."
          + NEW_LINE
          + "For latest release, see: https://github.com/codespecs/daikon/releases";
  public static final String DRIVER_EXIST =
      "Test driver already exists. Skipping "
          + NEW_LINE
          + "'generateTestDriverCode' task";

  public static final String SUCCESSFUL_DAIKON_EXECUTION =
      "Daikon task was successfully executed. All files generated by "
          + NEW_LINE
          + "this task would be located in the Daikon output directory "
          + NEW_LINE
          + "specified in your project's build.gradle";

  public static final String SUCCESSFUL_CODE_GENERATION =
      "Successful generation of test driver code. The Java files generated by "
          + NEW_LINE
          + "this task would be located in the Daikon output directory "
          + NEW_LINE
          + "specified in your project's build.gradle";

  public static final String DAIKON_FILES_EXIST =
      "Daikon-generated files already exist. These files can be found "
          + NEW_LINE
          + "in the Daikon output directory specified in your project's "
          + NEW_LINE
          + "build.gradle";

  public static final String DAIKON_IN_CLASSPATH =
      "Good. Daikon is in your CLASSPATH!";

  private Constants() {
    throw new Error("Cannot be instantiated");
  }
}
