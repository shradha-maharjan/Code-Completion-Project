package datactrlflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import base.MainBaseClass;
import datactrlflow.diff_match_patch.Diff;
import util.UtilFile;

public class MainValidateMaskedMethods extends MainBaseClass implements GlobalInfo {
   String inputDir = null, outputDir = null;
   boolean log = false;

   public MainValidateMaskedMethods(String inputDir, String outputDir) {
      this.inputDir = inputDir;
      this.outputDir = outputDir;
   }

   public MainValidateMaskedMethods(String inputDir, String outputDir, boolean flag) {
      super(flag);
      this.inputDir = inputDir;
      this.outputDir = outputDir;
   }

   void highlightDifferences(List<String> lines1, List<String> lines2, //
         ArrayList<List<String>> callNameMaskedTokens, String diffFileName) throws Exception {
      diff_match_patch dmp = new diff_match_patch();
      List<String> resultsDiff = new ArrayList<String>();
      boolean isEq = true;

      for (int i = 0; i < Math.min(lines1.size(), lines2.size()); i++) {
         String replaced = lines2.get(i).replaceAll("\\[MASK\\]", "-----"); // replace(lines2.get(i), "[MASK]");

         String[] tokens1 = lines1.get(i).split("\\s+");
         String[] tokens2 = replaced.split("\\s+");

         if (log) {
            System.out.println("[DBG] " + "Line " + (i + 1) + ":");
         }
         List<String> delTokenListPerFuncLine = new ArrayList<String>();

         for (int j = 0; j < Math.min(tokens1.length, tokens2.length); j++) {
            if (!tokens1[j].equals(tokens2[j])) {

               if (log) {
                  System.out.println("[DBG] - Difference: " + tokens1[j] + " -> " + tokens2[j]);
               }

               // System.out.println("[DBG] \t" + dmp.diff_main(tokens1[j], tokens2[j]));
               Iterator<Diff> iterator = dmp.diff_main(tokens1[j], tokens2[j], false).iterator();
               while (iterator.hasNext()) {
                  diff_match_patch.Diff diff = (diff_match_patch.Diff) iterator.next();
                  if (/*diff.operation.equals(diff_match_patch.Operation.INSERT) ||*/ //
                  diff.operation.equals(diff_match_patch.Operation.DELETE)) {

                     if (log) {
                        System.out.println("\t[DBG] " + diff.text);
                     }
                     delTokenListPerFuncLine.add(diff.text);
                     resultsDiff.add(diff.text);
                  }
               }
            }
         }
         // ** compare with `delTokenListPerFuncLine`.
         List<String> callNamesMasked = callNameMaskedTokens.get(i);
         Collections.sort(delTokenListPerFuncLine);
         Collections.sort(callNamesMasked);
         if ((callNamesMasked.size() != delTokenListPerFuncLine.size()) || //
               (callNamesMasked.equals(delTokenListPerFuncLine) == false)) {
            isEq = false;
            break;
         }
      }
      System.out.println(String.format("[DBG] %-40s: %d, %d", "# of file lines for diff: ", lines1.size(), lines2.size()));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-40s: %d", "# of `call seq` groups: ", callNameMaskedTokens.size()));
      System.out.println(String.format("[DBG] %-40s: %b", "Do mask the target variables correctly? ", isEq));
      System.out.println("[DBG] ------------------------------------------------------");
      UtilFile.saveToFile(this.outputDir + "/" + diffFileName, resultsDiff);
   }

   void highlightDifferences(List<String> lines1, List<String> lines2, List<String> varMaskedTokens, String diffFileName) throws Exception {
      diff_match_patch dmp = new diff_match_patch();
      List<String> resultsDiff = new ArrayList<String>();

      for (int i = 0; i < Math.min(lines1.size(), lines2.size()); i++) {
         String replaced = lines2.get(i).replaceAll("\\[MASK\\]", "-----"); // replace(lines2.get(i), "[MASK]");

         String[] tokens1 = lines1.get(i).split("\\s+");
         String[] tokens2 = replaced.split("\\s+");

         if (log) {
            System.out.println("[DBG] " + "Line " + (i + 1) + ":");
         }
         resultsDiff.add("Line " + (i + 1) + ":");
         // if (i == 12)
         // System.out.println("[DBG] " + lines1.get(i));

         for (int j = 0; j < Math.min(tokens1.length, tokens2.length); j++) {
            if (!tokens1[j].equals(tokens2[j])) {

               if (log) {
                  System.out.println("[DBG] - Difference: " + tokens1[j] + " -> " + tokens2[j]);
               }
               // System.out.println("[DBG] \t" + dmp.diff_main(tokens1[j], tokens2[j]));
               Iterator<Diff> iterator = dmp.diff_main(tokens1[j], tokens2[j], false).iterator();
               while (iterator.hasNext()) {
                  diff_match_patch.Diff diff = (diff_match_patch.Diff) iterator.next();
                  if (diff.operation.equals(diff_match_patch.Operation.INSERT) || //
                        diff.operation.equals(diff_match_patch.Operation.DELETE)) {

                     if (log) {
                        System.out.println("\t[DBG] " + diff);
                     }
                     resultsDiff.add(diff.toString());
                  }
               }
            }
         }
      }
      List<String> deletedTokens = extractDeleteWordsV2(resultsDiff);

      if (log) {
         for (String theDelToken : deletedTokens) {
            System.out.println("[DBG] DEL: " + theDelToken);
         }
         int width = 20;
         if (deletedTokens.size() != varMaskedTokens.size()) {
            throw new RuntimeException(String.format("[ERR] Data Integrity Error!!! %d %d", deletedTokens.size(), varMaskedTokens.size()));
         }
         for (int i = 0; i < Math.min(deletedTokens.size(), varMaskedTokens.size()); i++) {
            String l1 = deletedTokens.get(i);
            String l2 = varMaskedTokens.get(i);
            System.out.printf("%-" + width + "s %s%n", " " + l1, " " + l2);
         }
      }
      System.out.println(String.format("[DBG] %-40s: %d, %d", "# of file lines for diff: ", lines1.size(), lines2.size()));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-40s: %d", "# of deleted tokens: ", deletedTokens.size()));
      System.out.println(String.format("[DBG] %-40s: %b", "Mask the target var(or call) correctly? ", deletedTokens.equals(varMaskedTokens)));
      System.out.println("[DBG] ------------------------------------------------------");

      UtilFile.saveToFile(this.outputDir + "/" + diffFileName, resultsDiff);
   }

   void highlightDifferences(List<String> lines1, List<String> lines2) throws Exception {
      diff_match_patch dmp = new diff_match_patch();
      List<String> resultsDiff = new ArrayList<String>();

      for (int i = 0; i < Math.min(lines1.size(), lines2.size()); i++) {
         String replaced = lines2.get(i).replaceAll("\\[MASK\\]", "-----"); // replace(lines2.get(i), "[MASK]");

         String[] tokens1 = lines1.get(i).split("\\s+");
         String[] tokens2 = replaced.split("\\s+");

         System.out.println("[DBG] " + "Line " + (i + 1) + ":");
         resultsDiff.add("Line " + (i + 1) + ":");
         // if (i == 12)
         // System.out.println("[DBG] " + lines1.get(i));

         for (int j = 0; j < Math.min(tokens1.length, tokens2.length); j++) {
            if (!tokens1[j].equals(tokens2[j])) {

               if (log) {
                  System.out.println("[DBG] - Difference: " + tokens1[j] + " -> " + tokens2[j]);
               }
               // System.out.println("[DBG] \t" + dmp.diff_main(tokens1[j], tokens2[j]));
               Iterator<Diff> iterator = dmp.diff_main(tokens1[j], tokens2[j], false).iterator();
               while (iterator.hasNext()) {
                  diff_match_patch.Diff diff = (diff_match_patch.Diff) iterator.next();
                  if (diff.operation.equals(diff_match_patch.Operation.INSERT) || //
                        diff.operation.equals(diff_match_patch.Operation.DELETE)) {
                     System.out.println("\t[DBG] " + diff);
                     resultsDiff.add(diff.toString());
                  }
               }
            }
         }
      }
      List<String> deletedTokens = extractDeleteWordsV2(resultsDiff);
      for (String theDelToken : deletedTokens) {
         System.out.println("[DBG] DEL: " + theDelToken);
      }
      System.out.println(String.format("[DBG] %-40s: %d, %d", "# of file lines for diff: ", lines1.size(), lines2.size()));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-40s: %d", "# of deleted tokens: ", deletedTokens.size()));
      System.out.println("[DBG] ------------------------------------------------------");

      UtilFile.saveToFile(this.outputDir + "/" + OUTPUT_DIFF_DATA_FLOW, resultsDiff);
   }

   List<String> extractDeleteWordsV2(List<String> lines) {
      List<String> deleteWords = new ArrayList<>();
      Set<String> uniqueWordsInLine = new LinkedHashSet<>();
      boolean insideLine = false; //

      for (String line : lines) {
         line = line.trim();
         if (line.startsWith("Line ")) {
            insideLine = true;
            uniqueWordsInLine.clear();
         }
         else if (insideLine && line.startsWith("Diff(DELETE,\"")) {
            int start = line.indexOf("\"") + 1;
            int end = line.lastIndexOf("\"");
            if (start > 0 && end > start) {
               String word = line.substring(start, end);
               if (uniqueWordsInLine.add(word)) {
                  deleteWords.add(word);
               }
            }
         }
      }
      return deleteWords;
   }

   List<String> extractDeleteWords(List<String> lines) {
      Set<String> deleteWords = new LinkedHashSet<>(); //

      for (String line : lines) {
         line = line.trim();
         if (line.startsWith("Diff(DELETE,\"")) {
            int start = line.indexOf("\"") + 1;
            int end = line.lastIndexOf("\"");
            if (start > 0 && end > start) {
               String word = line.substring(start, end);
               deleteWords.add(word);
            }
         }
      }
      return new ArrayList<>(deleteWords);
   }

   @Deprecated
   String replace(String text, String pattern) {
      String updatedText = null;
      diff_match_patch dmp = new diff_match_patch();
      int startPosition = 0;
      int matchIndex = dmp.match_main(text, pattern, startPosition);
      if (matchIndex != -1) {
         // System.out.println("Pattern found at index: " + matchIndex);
         updatedText = text.substring(0, matchIndex) + " " + text.substring(matchIndex + pattern.length());
         // System.out.println("[DBG] ORG: " + text);
         // System.out.println("[DBG] CHG: " + updatedText);
      }
      else {
         System.out.println("Pattern not found.");
      }
      return updatedText;
   }

   public static void main(String[] args) throws Exception {
      if (args.length != 2) {
         System.err.println("Usage: java YourProgram <inputDir> <outputDir>");
         System.exit(1); //
      }
      MainValidateMaskedMethods main = new MainValidateMaskedMethods(args[0], args[1]);
      main.inputDir = args[0];
      main.outputDir = args[1];
      try {
         List<String> lines1 = Files.readAllLines(Paths.get(args[0] + "/" + INPUT_DATA_FLOW_DIFF1));
         List<String> lines2 = Files.readAllLines(Paths.get(args[1] + "/" + INPUT_DATA_FLOW_DIFF2));

         main.highlightDifferences(lines1, lines2);
         main.closingTime();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
}
