package com.walmart.otto.shards;

import com.walmart.otto.Constants;
import com.walmart.otto.configurator.Configurator;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Pattern;

public class ShardCreator {

  private Configurator configurator;
  private int extraTestsInShard = 0;

  public ShardCreator(Configurator configurator) {
    this.configurator = configurator;
  }

  public List<String> getShards(List<String> testCases) {
    Stack<String> stack = new Stack<>();
    stack.addAll(testCases);

    int numberOfShards = configurator.getNumShards();
    int numTestsInShards = 0;

    if (numberOfShards != -1) {
      extraTestsInShard = testCases.size() % numberOfShards;
      numTestsInShards = testCases.size() / numberOfShards;
    } else {
      numberOfShards = testCases.size();
      numTestsInShards = 1;
    }
    List<String> list = new ArrayList<>();

    for (int ii = 1; ii <= numberOfShards; ii++) {
      String test = addToShard(stack, numTestsInShards);

      if (!test.isEmpty()) {
        list.add(test);
      }
    }
    return list;
  }

  private String addToShard(Stack<String> stack, int numTestsInShards) {
    StringBuilder stringBuilder = new StringBuilder();

    if (stack.isEmpty()) {
      return "";
    }

    for (int i = 0; i < numTestsInShards; i++) {
      addStringToBuilder(stack.pop(), stringBuilder);
    }

    if (extraTestsInShard > 0) {
      addStringToBuilder(stack.pop(), stringBuilder);
      extraTestsInShard--;
    }

    return stringBuilder.toString();
  }

  public List<String> getConfigurableShards(List<String> testCases) {
    if (configurator.getNumShards() != -1 || configurator.getShardDuration() == -1) {
      return new ArrayList<>();
    }

    Map<String, String> shardMap = createShardMap();
    List<String> shards = new ArrayList<>();
    List<String> singleShards = new ArrayList<>();

    while (shardMap.size() > 0) {
      String testCase = createConfigurableShard(shardMap, testCases).toString();

      if (!testCase.isEmpty()) {
        shards.add(testCase);
      }
    }
    for (String test : testCases) {
      if (!doesContain(shards, test)) {
        singleShards.add(test);
      }
    }

    shards.addAll(getShards(singleShards));

    return shards;
  }

  private boolean doesContain(List<String> shards, String testCase) {
    for (String line : shards) {
      if (line.contains(testCase)) {
        return true;
      }
    }
    return false;
  }

  public StringBuilder createConfigurableShard(
      Map<String, String> shardMap, List<String> testCases) {
    StringBuilder stringBuilder = new StringBuilder();

    int shardLengthSec = configurator.getShardDuration();

    Set<Entry<String, String>> entrySet = shardMap.entrySet();
    Iterator<Entry<String, String>> it = entrySet.iterator();

    while (it.hasNext()) {
      Entry<String, String> testCase = it.next();

      int testCaseTime = Integer.parseInt(testCase.getValue());

      if (shardLengthSec - testCaseTime > 0) {
        if (testCases.contains(testCase.getKey())) {
          addStringToBuilder(testCase.getKey(), stringBuilder);
          shardLengthSec = shardLengthSec - testCaseTime;
        }

        it.remove();
      } else if (shardLengthSec == configurator.getShardDuration()
          && testCaseTime >= configurator.getShardDuration()) {
        if (testCases.contains(testCase.getKey())) {
          addStringToBuilder(testCase.getKey(), stringBuilder);
        }
        it.remove();
        return stringBuilder;
      }
    }
    return stringBuilder;
  }

  public Map<String, String> createShardMap() {
    Path testTimeFile = Paths.get(Constants.TEST_TIME_FILE);
    try (BufferedReader br = Files.newBufferedReader(testTimeFile)) {
      Map<String, String> shardMap = new HashMap<>();
      String line;
      while ((line = br.readLine()) != null) {
        String[] testCase = line.split(Pattern.quote(" "));
        shardMap.put(testCase[0], testCase[1]);
      }
      return shardMap;
    } catch (IOException e) {
      System.err.println("Unable to read flank.tests file: " + e.getMessage());
      return Collections.emptyMap();
    }
  }

  private void addStringToBuilder(String stringToAdd, StringBuilder stringBuilder) {
    stringBuilder.append("class ");
    stringBuilder.append(stringToAdd);
    stringBuilder.append(",");
  }

  public static String generateTestCaseName(String className, String testName) {
    return "class " + className + "#" + testName;
  }
}
