package datactrlflow;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.SimpleName;
import org.junit.Test;

import data.DefUseModel;
import datactrlflow.MainClassifyMethodCalls.MethodData;
import util.UtilAST;
import util.UtilFile;
import visitor.CtrlFlowAnalyzer;

public class TestMainClassifyMethodCalls {
   static final String UNIT_NAME = "ParsedAndToString";
   boolean log = false, debug = false;
   MainClassifyMethodCalls main = new MainClassifyMethodCalls();

   @Test
   public void testMaskCtrlFlowData() throws Exception {
      maskCtrlFlowData();
      main.closingTime();
   }

   void maskCtrlFlowData() throws Exception {
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] ------- Mask control flow data -----------------------");
      String filePath = "output/classify/pretrain-fun-ctrl-flow-keep.txt";
      // String filePath = "output/classify/sample-ctrl5000.txt";
      String filePathOutput = "output/classify/pretrain-fun-ctrl-flow-mask.txt";
      System.out.println("[DBG] Input: " + filePath);
      /////////////////////////////////////////////////////////////////////////////////
      int index = 1, counterCtrlFlow = 0;
      String theLineOfFunction = null;
      List<String> ctrlFlowAnalList = new ArrayList<>();
      List<String> ctrlFlowfuncMaskedList = new ArrayList<>();
      ArrayList<List<String>> ctrlFlowCallNamesMaskedList = new ArrayList<>();

      ctrlFlowAnalList = Files.readAllLines(Paths.get(filePath));
      System.out.println(String.format("[DBG] %-40s: %d", "# of lines of control flow analysis", ctrlFlowAnalList.size()));

      for (int iFunLine = 0; iFunLine < ctrlFlowAnalList.size(); iFunLine++) {
         theLineOfFunction = ctrlFlowAnalList.get(iFunLine);
         if (log) {
            System.out.println("[DBG] " + theLineOfFunction);
         }
         ASTParser parser = UtilAST.parseSrcCode(theLineOfFunction, UNIT_NAME + ".java");
         CompilationUnit cu = (CompilationUnit) parser.createAST(null);
         CtrlFlowAnalyzer analyzerCF = new CtrlFlowAnalyzer(UNIT_NAME, log);
         cu.accept(analyzerCF);

         MethodData methodData = main.processLongestSequence(analyzerCF.getdefUseMap(), //
               analyzerCF.getMethodName(), index, theLineOfFunction);

         // ** Ensure that each function should contain 2 or more method invocations.
         if (methodData.maxTotalCallsAndAccesses < 2) {
            throw new RuntimeException("[ERR] Data Integrity Error!!!" + theLineOfFunction);
         }

         List<String> longestSequenceList = Arrays.stream(methodData.longestSequence.split(";")). //
               map(s -> s.replace("()", "")).collect(Collectors.toList());

         List<int[]> maskPositions = new ArrayList<>();
         List<String> ctrlFlowCallNamesMaskedListPerLine = new ArrayList<>();

         Map<IVariableBinding, DefUseModel> analyserCtrolFlowMap = analyzerCF.getdefUseMap();
         for (Entry<IVariableBinding, DefUseModel> iEntry : analyserCtrolFlowMap.entrySet()) {
            DefUseModel iVariableAnal = iEntry.getValue();

            for (MethodInvocation iMethInvoc : iVariableAnal.getMethodInvocations()) {
               // E.g., iMethInvoc - a.foo(b.bar());
               String methodName = iMethInvoc.getName().toString(); // E.g., foo
               String objectName = ((SimpleName) iMethInvoc.getExpression()).getIdentifier(); // E.g., a
               String outerMostMethInvocStr = objectName + "." + methodName; // E.g., a.foo
               String methInvocStr = iMethInvoc.toString();

               if (longestSequenceList.contains(outerMostMethInvocStr)) {
                  ctrlFlowCallNamesMaskedListPerLine.add(methodName);
                  int startPos = iMethInvoc.getName().getStartPosition();
                  int endPos = startPos + iMethInvoc.getName().getLength();
                  maskPositions.add(new int[] { startPos, endPos });

                  if (log) {
                     System.out.println(String.format("[DBG]\t offset (%d - %d) %s", startPos, endPos, methInvocStr));
                  }
               }
            }
         }

         if (ctrlFlowCallNamesMaskedListPerLine.size() == 0) {
            throw new RuntimeException("[ERR] Data Integrity Error!!!" + theLineOfFunction);
         }
         ctrlFlowCallNamesMaskedList.add(ctrlFlowCallNamesMaskedListPerLine);

         String theLineOfFunctionMasked = replaceTokensWithMask(maskPositions, theLineOfFunction);
         ctrlFlowfuncMaskedList.add(theLineOfFunctionMasked);
         if (log) {
            System.out.println("[DBG] " + theLineOfFunctionMasked);
         }
         counterCtrlFlow++;
      }

      MainValidateMaskedMethods validate = new MainValidateMaskedMethods(null, "output/classify/", false);
      validate.highlightDifferences(ctrlFlowAnalList, ctrlFlowfuncMaskedList, ctrlFlowCallNamesMaskedList, "diff.txt");

      UtilFile.saveToFile(filePathOutput, ctrlFlowfuncMaskedList);
      System.out.println(String.format("[DBG] %-40s: %d", "# of control flow analysis", counterCtrlFlow));
   }

   String replaceTokensWithMask(List<int[]> maskPositions, String input) {
      StringBuilder sourceBuilder = new StringBuilder(input);
      maskPositions.sort((a, b) -> Integer.compare(b[0], a[0]));

      for (int[] pos : maskPositions) {
         int start = pos[0];
         int end = pos[1];

         if (start < end && end <= sourceBuilder.length()) {
            // System.out.println("Applying mask from " + start + " to " + end);
            sourceBuilder.replace(start, end, "[MASK]");
         }
      }
      return sourceBuilder.toString();
   }

   // @Test
   public void testMaskCtrlFlowDataV2() {
      String input = "void foo() {m1(); m2();}";
      int offset1 = 12; // 229;
      int length1 = 2; // 36; // 265
      int offset2 = 18; // 266;
      int length2 = 2; // 37;

      List<int[]> maskPositions = new ArrayList<>();
      maskPositions.add(new int[] { offset1, offset1 + length1 });
      maskPositions.add(new int[] { offset2, offset2 + length2 });

      System.out.println("[DBG] " + input);
      String maskedInput = applyMask(input, maskPositions);
      System.out.println("[DBG] " + maskedInput);
      System.out.println("[DBG] ------------------------------------------------------");
   }

   // @Test
   public void testMaskCtrlFlowDataV3() {
      // maskPositions.sort((a, b) -> Integer.compare(b[0], a[0]));
      String input = "class ParsedAndToString { @Implementation(minSdk=O_MR1) public Point getStableDisplaySize() throws RemoteException { "//
            + "DisplayInfo defaultDisplayInfo=mDm.getDisplayInfo(Display.DEFAULT_DISPLAY); "//
            + "return new Point(defaultDisplayInfo.getNaturalWidth(),defaultDisplayInfo.getNaturalHeight()); } }";

      int offset1 = 229;
      int length1 = 15; // 265
      int offset2 = 266;
      int length2 = 16;

      List<int[]> maskPositions = new ArrayList<>();
      maskPositions.add(new int[] { offset1, offset1 + length1 });
      maskPositions.add(new int[] { offset2, offset2 + length2 });

      System.out.println("[DBG] " + input);
      String maskedInput = applyMask(input, maskPositions);
      System.out.println("[DBG] " + maskedInput);
      System.out.println("[DBG] ------------------------------------------------------");
   }

   private String applyMask(String input, List<int[]> maskPositions) {
      boolean log = false;
      StringBuilder buf = new StringBuilder();

      // maskPositions.sort((a, b) -> Integer.compare(b[0], a[0]));

      for (int i = 0; i < input.length(); i++) {
         boolean isMasked = false;

         for (int[] range : maskPositions) {
            int start = range[0];
            int end = range[1];

            if (i >= start && i < end) {
               isMasked = true;
               break;
            }
         }

         if (isMasked) {
            if (log)
               System.out.print("[MASK]");

            buf.append("[MASK]");
            // *** [Important] Skip remaining characters in the current mask range ***
            for (int[] range : maskPositions) {
               if (i == range[0]) {
                  i = range[1] - 1; // [Important] *** Move to the last character of the mask ***
                  break;
               }
            }
         }
         else {
            if (log)
               System.out.print(input.charAt(i));
            buf.append(input.charAt(i));
         }
      }
      if (log)
         System.out.println();
      return buf.toString();
   }

   // @Test
   public void testMaskCtrlFlowDataV4() {
      String input = "void foo() {m1(); m1();}";
      int offset1 = 12; // 229;
      int length1 = 2; // 36; // 265
      int offset2 = 18; // 266;
      int length2 = 2; // 37;

      List<int[]> maskPositions = new ArrayList<>();
      maskPositions.add(new int[] { offset1, offset1 + length1 });
      maskPositions.add(new int[] { offset2, offset2 + length2 });

      List<Integer> offsets = new ArrayList<>();
      offsets.add(12);
      offsets.add(18);
      String targetToken = "m1";
      String mask = "__MASK__";
      String maskedInput = replaceTokensWithMask(input, offsets, targetToken, mask);

      System.out.println("[DBG] " + input);
      System.out.println("[DBG] " + maskedInput);
      System.out.println("[DBG] ------------------------------------------------------");

      maskedInput = replaceTokensWithMask(maskPositions, input);
      System.out.println("[DBG] " + input);
      System.out.println("[DBG] " + maskedInput);
      System.out.println("[DBG] ------------------------------------------------------");
   }

   // @Test
   public void testMaskCtrlFlowDataV5() {
      String input = "class ParsedAndToString { @Implementation(minSdk=O_MR1) public Point getStableDisplaySize() throws RemoteException { "//
            + "DisplayInfo defaultDisplayInfo=mDm.getDisplayInfo(Display.DEFAULT_DISPLAY); "//
            + "return new Point(defaultDisplayInfo.getNaturalWidth(),defaultDisplayInfo.getNaturalHeight()); } }";

      int offset1 = 229;
      int length1 = 15;
      int offset2 = 266;
      int length2 = 16;

      List<int[]> maskPositions = new ArrayList<>();
      maskPositions.add(new int[] { offset1, offset1 + length1 });
      maskPositions.add(new int[] { offset2, offset2 + length2 });

      String maskedInput = "";

      System.out.println("[DBG] " + input);
      System.out.println("[DBG] " + maskedInput);
      System.out.println("[DBG] ------------------------------------------------------");

      maskedInput = replaceTokensWithMask(maskPositions, input);
      System.out.println("[DBG] " + input);
      System.out.println("[DBG] " + maskedInput);
      System.out.println("[DBG] ------------------------------------------------------");
   }

   String replaceTokensWithMask(String theLineOfFunction, List<Integer> offsets, String targetToken, String mask) {
      StringBuilder result = new StringBuilder(theLineOfFunction);
      offsets.sort((a, b) -> b - a); //

      for (int offset : offsets) {
         if (offset >= 0 && offset <= result.length() - targetToken.length()) {
            if (result.substring(offset, offset + targetToken.length()).equals(targetToken)) {
               result.replace(offset, offset + targetToken.length(), mask);
            }
            else {
               throw new RuntimeException("[ERR] Target token mismatch at offset " + offset + " in: " + theLineOfFunction);
            }
         }
         else {
            throw new RuntimeException("[ERR] Invalid offset: " + offset + " for string: " + theLineOfFunction);
         }
      }

      return result.toString();
   }

}

//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
