package com.sri.gradle.tasks;

import com.sri.gradle.Options;
import com.sri.gradle.internal.Chicory;
import com.sri.gradle.internal.Daikon;
import com.sri.gradle.internal.DynComp;
import com.sri.gradle.utils.Filefinder;
import com.sri.gradle.utils.ImmutableStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class TaskExecutorImpl implements TaskExecutor {
  static final String TEST_DRIVER = "TestDriver";

  private final List<Throwable> encounteredErrors;
  private final List<TaskBuilderImpl> workBuilders;

  public TaskExecutorImpl(){
    this.encounteredErrors = new LinkedList<>();
    this.workBuilders = new LinkedList<>();
  }

  @Override public void addError(Throwable cause) {
    Optional.ofNullable(cause).ifPresent(this.encounteredErrors::add);
  }

  @Override public TaskBuilder runDaikonOn(File testClassesDir) {
    final TaskBuilderImpl builder = new TaskBuilderImpl(testClassesDir.toPath(), this);
    workBuilders.add(builder);
    return builder;
  }

  @Override public void execute() throws TaskConfigurationError {

    // Blow up if we encountered errors.
    if (!encounteredErrors.isEmpty()) {
      throw new TaskConfigurationError(encounteredErrors);
    }

    for (TaskBuilderImpl each : workBuilders){
      // a work builder configures a work executor
      // so now we apply this configuration
      applyBuiltConfiguration(each);
    }
  }

  private static void applyBuiltConfiguration(TaskBuilderImpl each) {
    final Path classesDir = each.getTestClassesDir();
    final Path outputDir = each.getOutputDir();
    final List<URL> classpath = each.getClasspath();

    final List<File>    allTestClasses  = Filefinder.findJavaClasses(classesDir);
    final List<String>  allQualifiedClasses = getFullyQualifiedNames(allTestClasses);

    String mainClass  = allQualifiedClasses.stream()
        .filter(f -> f.endsWith(TEST_DRIVER))
        .findFirst().orElse(null);

    if(mainClass == null){
      System.out.println("Not main class for DynComp operation");
      return;
    }

    mainClass = mainClass.replace(".class", "");

    final String prefix = mainClass.substring(mainClass.lastIndexOf('.') + 1);

    executeDynComp(mainClass, allQualifiedClasses, classpath, outputDir);
    executeChicory(mainClass, prefix, allQualifiedClasses, classpath, outputDir);
    executeDaikon(mainClass, prefix, classpath, outputDir);
  }

  private static void executeDaikon(String mainClass, String namePrefix, List<URL> classpath, Path outputDir) {
    final Daikon daikon = new Daikon()
        .setClasspath(classpath)
        .setWorkingDirectory(outputDir)
        .setDtraceFile(outputDir, namePrefix + ".dtrace.gz")
        .setStandardOutput(namePrefix + ".inv.gz");
    final List<String> output = daikon.execute();
    if (!output.isEmpty()){
      output.forEach(System.out::println);
    }
  }

  private static void executeChicory(String mainClass, String namePrefix, List<String> allQualifiedClasses,
      List<URL> classpath, Path outputDir) {
    final Chicory chicory = new Chicory()
        .setClasspath(classpath)
        .setMainClass(mainClass)
        .selectedClasses(allQualifiedClasses)
        .setOutputDirectory(outputDir)
        .setComparabilityFile(outputDir, namePrefix + ".decls-DynComp")
        .setWorkingDirectory(outputDir);

    final List<String> output = chicory.execute();
    if (!output.isEmpty()){
      output.forEach(System.out::println);
    }
  }

  private static void executeDynComp(String mainClass, List<String> allQualifiedClasses,
      List<URL> classpath, Path outputDir) {
    final DynComp dynComp = new DynComp()
        .setClasspath(classpath)
        .setMainClass(mainClass)
        .selectedClasses(allQualifiedClasses)
        .setOutputDirectory(outputDir)
        .setWorkingDirectory(outputDir);
    final List<String> output = dynComp.execute();
    if (!output.isEmpty()){
      output.forEach(System.out::println);
    }
  }


  public static List<String> getFullyQualifiedNames(List<File> javaFiles){
    return ImmutableStream.listCopyOf(javaFiles.stream().map(f -> {
      try {
        final String canonicalPath = f.getCanonicalPath();
        final String deletingPrefix = canonicalPath
            .substring(0, f.getCanonicalPath().indexOf(Options.PROJECT_TEST_CLASS_DIR.value())) + Options.PROJECT_TEST_CLASS_DIR.value()
            + "/";

        return canonicalPath.replace(deletingPrefix, "")
            .replaceAll(".class","")
            .replaceAll("/",".");
      } catch (IOException ignored){}
      return null;
    }).filter(Objects::nonNull));

  }

}
