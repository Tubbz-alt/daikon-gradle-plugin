package com.sri.gradle.daikon.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sri.gradle.daikon.Constants;
import com.sri.gradle.daikon.utils.Filefinder;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

@SuppressWarnings("UnstableApiUsage")
public class DaikonEvidence extends AbstractNamedTask {
  private final DirectoryProperty outputDir;
  private final Property<String> testDriverPackage;

  public DaikonEvidence(){
    this.outputDir = getProject().getObjects().directoryProperty(); // unchecked warning
    this.testDriverPackage = getProject().getObjects().property(String.class); // unchecked warning
  }

  @TaskAction public void daikonEvidence(){
    final File daikonOutputDir = getOutputDir().getAsFile().get();
    final List<File> allTxtFiles = Filefinder.findTextFiles(daikonOutputDir.toPath());
    Optional<Path> invsFile = allTxtFiles.stream()
        .map(File::toPath)
        .filter(Files::exists)
        .filter(f -> f.getFileName().toString().endsWith("inv.txt")).findAny();

    if (!invsFile.isPresent()){
      getLogger().warn("Skipping evidence file generation. Unable to find .inv.txt file");
      return;
    }

    final Path actualInvsFile = invsFile.get();
    final Path daikonEvidenceFile = getProject()
        .getProjectDir()
        .toPath()
        .resolve(Constants.DAIKON_DETAILS_FILE_NAME);

    final String matchKey = getTestDriverPackage().get();

    final Map<String, String> processedRecords = new IdentityHashMap<>();
    processedRecords.put("ACTIVITY", "DYNAMIC_ANALYSIS");
    processedRecords.put("AGENT", "DAIKON");
    processedRecords.put("DAIKON_OUT", daikonOutputDir.toString());
    processedRecords.put("TEST_DRIVER_PKG", matchKey);
    processedRecords.put("CORES", String.valueOf(Runtime.getRuntime().availableProcessors()));

    // This will return Long.MAX_VALUE if there is no preset limit
    long maxMemory = Runtime.getRuntime().maxMemory();
    processedRecords.put("JVM_MEMORY_LIMIT_IN_BYTES", (maxMemory == Long.MAX_VALUE ? "no limit" : String.valueOf(maxMemory)));
    processedRecords.put("MEMORY_AVAILABLE_TO_JVM_IN_BYTES", String.valueOf(Runtime.getRuntime().totalMemory()));

    final ReadWriteDaikonDetails evidenceWriter = new ReadWriteDaikonDetails(
        actualInvsFile,
        daikonEvidenceFile,
        matchKey);

    try {
      processedRecords.putAll(evidenceWriter.processLineByLine());
      evidenceWriter.writeToOutput(processedRecords);
      getLogger().debug(processedRecords.size() + " records extracted.");
    } catch (IOException ioe){
      throw new GradleException("Unable to process " + actualInvsFile, ioe);
    }

    getLogger().quiet("Successfully generated evidence file: " + daikonEvidenceFile.getFileName());
  }

  @OutputDirectory public DirectoryProperty getOutputDir() {
    return this.outputDir;
  }

  @Input public Property<String> getTestDriverPackage() {
    return this.testDriverPackage;
  }

  @Override protected String getTaskName() {
    return Constants.DAIKON_EVIDENCE_TASK;
  }

  @Override protected String getTaskDescription() {
    return Constants.DAIKON_EVIDENCE_TASK_DESCRIPTION;
  }

  static class ReadWriteDaikonDetails {
    private final Path inputFile;
    private final Path outFile;
    private final String matchKey;

    ReadWriteDaikonDetails(Path inputFile, Path outFile, String matchKey){
      this.inputFile = Objects.requireNonNull(inputFile);
      this.outFile = outFile;
      this.matchKey = matchKey;
    }

    Map<String, String> processLineByLine() throws IOException {
      Preconditions.checkArgument(Files.exists(inputFile));

      final List<String> lines = new LinkedList<>();
      try (Scanner scanner =  new Scanner(inputFile, Constants.ENCODING.name())){
        while (scanner.hasNextLine()){
          lines.add(scanner.nextLine());
        }
      }

      int idx = 0;
      final Set<String> testsExplored = new HashSet<>();
      final Set<String> classesExplored = new HashSet<>();
      final List<String> invDetected = new LinkedList<>();
      while (idx < lines.size()){
        if (lines.get(idx).contains(Constants.DAIKON_SPLITTER)){
          idx++;
          String ppName = lines.get(idx);
          // search for class name
          if (ppName.contains(":::")){
            String target = ppName.substring(0, ppName.lastIndexOf(":::"));
            if (target.contains("(")){
              target = target.substring(0, target.indexOf("("));
              target = target.substring(0, target.lastIndexOf("."));
            }

            if (Constants.EXPECTED_JUNIT4_NAME_REGEX.asPredicate().test(target)){
              final String unitTest = ppName.substring(0, ppName.lastIndexOf(":::"));
              testsExplored.add(unitTest);
            } else if (!"org.junit.Assert".equals(target)){
              classesExplored.add(target);
            }
          }
          idx++;
          // search for invariants associated with ppName
          while (idx < lines.size() && !lines.get(idx).contains(Constants.DAIKON_SPLITTER)){
            if (lines.get(idx).startsWith(matchKey) || lines.get(idx).startsWith("this.")){
              final String className = lines.get(idx).substring(0, lines.get(idx).lastIndexOf("."));
              if (!Constants.EXPECTED_JUNIT4_NAME_REGEX.asPredicate().test(className)){
                invDetected.add(lines.get(idx));
              }
            }

            idx++;
          }
        } else {
          idx++;
        }
      }

      final Map<String, String> details = new IdentityHashMap<>();
      details.put("TESTS_COUNT", String.valueOf(testsExplored.size()));
      details.put("CLASSES_COUNT", String.valueOf(classesExplored.size()));
      details.put("INVARIANT_COUNT", String.valueOf(invDetected.size()));
      details.put("PP_COUNT", String.valueOf(testsExplored.size() + classesExplored.size()));

      return ImmutableMap.copyOf(details);
    }

    void writeToOutput(Map<String, String> otherRecord) throws IOException {

      final Map<String, Map<String, String>> jsonDoc = new HashMap<>();
      jsonDoc.put("DETAILS", otherRecord);

      if (Files.exists(outFile)){
        Files.delete(outFile);
      }

      final Gson gson = new GsonBuilder().setPrettyPrinting().create();
      try (Writer writer = Files.newBufferedWriter(outFile)) {
        gson.toJson(jsonDoc, writer);
      }

    }
  }
}