package datactrlflow;

import org.eclipse.jdt.core.dom.*;

import base.MainBaseClass;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import util.CheckParsing;
import util.UtilAST;
import util.UtilFile;
import visitor.CtrlFlowAnalyzer;
import visitor.DataFlowAnalyzer;
import data.DefUseModel;

public class MainClassifyMethodCalls extends MainBaseClass implements GlobalInfo {
   static final String UNIT_NAME = "ParsedAndToString";
   String INPUT_DIR = null, OUTPUT_DIR = null;

   static List<String> resultsWithControlFlow = new ArrayList<String>();
   boolean log = false, debug = false;
   // control flow analysis
   List<String> ctrlFlowAnalList, ctrlFlowfuncMaskedList;
   ArrayList<List<String>> ctrlFlowCallNamesMaskedList;
   // data flow analysis
   List<String> dataFlowAnalList, dataFlowfuncMaskedList, dataFlowVarsMasked;
   // other functions for data flow analysis
   List<String> otherDataFlowAnalList, otherModifiedDataFlowAnalList, otherDataFlowfuncMaskedList, otherDataFlowVarsMasked;

   private static final String MASK_TOKEN = "[MASK]";

   public static void main(String[] args) throws Exception {
      if (args.length != 2) {
         System.out.println("Usage: java YourClassName <inputDirectory> <outputDirectory>");
         return;
      }

      MainClassifyMethodCalls main = new MainClassifyMethodCalls();
      main.INPUT_DIR = args[0];
      main.OUTPUT_DIR = args[1];
      System.out.println("[DBG] Input Dir  : " + args[0]);
      System.out.println("[DBG] Output Dir : " + args[1]);
      final int numExecAnal = 1;
      for (int i = 0; i < numExecAnal; i++) {
         System.out.println("[DBG] Input File : " + main.INPUT_DIR + "/" + INPUT_FILE_PATH);
         // Step 1. Classify
         main.dataClassifier(main.INPUT_DIR + "/" + INPUT_FILE_PATH);
      }
      // Step 2-a. Mask control flow
      main.maskCtrlFlowData();
      main.validateCtrlFlowData();
      // Step 2-b. Mask data flow
      main.maskDataFlowData();
      main.validateDataFlowData();
      // Step 2-c. Mask other functions.
      main.maskOtherDataFlowData();
      main.validateOtherDataFlowData();
      main.closingTime();
   }

   void dataClassifier(String inputFile) throws Exception {
      int counterLine = 0, counterCasesWithMultiFuncInLine = 0;
      int counterFieldAccCall = 0, counterMethodCall = 0, counterMethodAndFieldCall = 0;
      int index = 1, counterDefUseAnalysis = 0;

      String[] inputFunctions = UtilFile.readFileArray(inputFile);
      System.out.println(String.format("[DBG] %-40s: %d", "# of function to be analyzed", inputFunctions.length));

      List<String> ctrlFlowAnalList = new ArrayList<String>();
      List<String> ctrFlowAnalCallSeq = new ArrayList<String>();

      List<String> dataFlowAnalList = new ArrayList<String>();
      List<String> resultsOthers = new ArrayList<String>();

      for (String theLineOfFunction : inputFunctions) {
         if (theLineOfFunction.startsWith("// [DBG]"))
            continue;

         counterLine++;
         ASTParser parser = UtilAST.parseSrcCode(theLineOfFunction, UNIT_NAME + ".java");
         CompilationUnit cu = (CompilationUnit) parser.createAST(null);
         CtrlFlowAnalyzer defUseVisitor = new CtrlFlowAnalyzer(UNIT_NAME, log);
         cu.accept(defUseVisitor);

         if (defUseVisitor.getCountMethodDeclVisit() != 1) {
            counterCasesWithMultiFuncInLine++;
            if (log) {
               System.out.println("[DBG] ****************************************");
               System.out.println(String.format("[WRN] # of found methods: %d, line: %d, %s", //
                     defUseVisitor.getCountMethodDeclVisit(), counterLine, theLineOfFunction));
               System.out.println("[DBG] ****************************************");
            }
         }

         if (defUseVisitor.getdefUseMap().size() != 0) {
            counterDefUseAnalysis++;
         }
         MethodData methodData = processLongestSequence(defUseVisitor.getdefUseMap(), //
               defUseVisitor.getMethodName(), index, theLineOfFunction);

         // Step 1. Control flow analysis
         if (methodData.maxTotalCallsAndAccesses >= 2) {
            // ** a function at least should have 2 or more method invocations.
            ctrFlowAnalCallSeq.add(methodData.longestSequence);
            ctrlFlowAnalList.add(theLineOfFunction);
         }
         // Step 2. Data flow analysis
         else {
            if (debug && theLineOfFunction.contains("class ParsedAndToString { public PackageResourceTable newResourceTable( String packageName")) {
               System.out.println("[DBG] " + theLineOfFunction);
            }
            DataFlowAnalyzer dataFlowAnalyzer = new DataFlowAnalyzer(cu);
            cu.accept(dataFlowAnalyzer);
            Map<IBinding, Integer> counterVarUsage = dataFlowAnalyzer.getCounterVarUsage();
            Entry<IBinding, Integer> maxEntry = findMaxEntry(counterVarUsage);
            // In case the binding is null, e.g., lambda expression.
            // *** Need to skip when maxEntry.getValue() is equal to '0', meaning only variable definition exists.
            if (checkDataIntegrityDataFlowAnalysis(maxEntry)) {
               dataFlowAnalList.add(theLineOfFunction);
            }
            // Step 3. Extra analysis
            else {
               resultsOthers.add(theLineOfFunction);
            }
         }

         if (methodData.maxFieldAccesses > 0) {
            counterFieldAccCall++;
         }
         if (methodData.maxMethodCalls > 0) {
            counterMethodCall++;
         }
         if (methodData.maxTotalCallsAndAccesses > 0) {
            counterMethodAndFieldCall++;
         }
         resultsWithControlFlow.add(methodData.maxTotalCallsAndAccesses + ":::" + theLineOfFunction);
         index++;
      }
      if (log) {
         for (String theCountControlFlow : resultsWithControlFlow) {
            System.out.println("[DBG] " + theCountControlFlow);
         }
      }

      System.out.println(String.format("[DBG] %-40s: %d", "# of def use analysis", counterDefUseAnalysis));
      System.out.println(String.format("[DBG] %-40s: %d", "# of cases with multi func in line", counterCasesWithMultiFuncInLine));
      System.out.println(String.format("[DBG] %-40s: %d", "# of method calls", counterMethodCall));
      System.out.println(String.format("[DBG] %-40s: %d", "# of field access calls", counterFieldAccCall));
      System.out.println(String.format("[DBG] %-40s: %d", "# of `method` and `field access` calls", counterMethodAndFieldCall));
      System.out.println(String.format("[DBG] %s", "------------------------------------------------------"));
      System.out.println(String.format("[DBG] %-40s: %d", "# of function analyzed", counterLine));
      System.out.println(String.format("[DBG] %-40s: %d", "# of call sequences (>2)", ctrFlowAnalCallSeq.size()));
      System.out.println(String.format("[DBG] %s", "------------------------------------------------------"));
      System.out.println(String.format("[DBG] %-40s: %d", "# of functions with ctr flow (>2)", ctrlFlowAnalList.size()));
      System.out.println(String.format("[DBG] %-40s: %d", "# of functions with data flow", dataFlowAnalList.size()));
      System.out.println(String.format("[DBG] %-40s: %d", "# of others to be analyzed next", resultsOthers.size()));

      this.ctrlFlowAnalList = ctrlFlowAnalList;
      this.dataFlowAnalList = dataFlowAnalList;
      this.otherDataFlowAnalList = resultsOthers;

      UtilFile.saveToFile(OUTPUT_DIR + "/" + OUTPUT_CTRL_FLOW_METHODS, ctrlFlowAnalList);
      UtilFile.saveToFile(OUTPUT_DIR + "/" + OUTPUT_DATA_FLOW_METHODS, dataFlowAnalList);
      UtilFile.saveToFile(OUTPUT_DIR + "/" + OUTPUT_OTHER_METHODS, resultsOthers);
   }

   void maskCtrlFlowData() throws Exception {
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] ------- Mask control flow data -----------------------");
      timeNow(ZonedDateTime.now(), "Current Time: ");

      ctrlFlowfuncMaskedList = new ArrayList<>();
      ctrlFlowCallNamesMaskedList = new ArrayList<>();
      int index = 1, counterCtrlFlow = 0;

      for (int iFunLine = 0; iFunLine < this.ctrlFlowAnalList.size(); iFunLine++) {

         String theLineOfFunction = ctrlFlowAnalList.get(iFunLine);
         if (log) {
            System.out.println("[DBG] " + theLineOfFunction);
         }
         ASTParser parser = UtilAST.parseSrcCode(theLineOfFunction, UNIT_NAME + ".java");
         CompilationUnit cu = (CompilationUnit) parser.createAST(null);
         CtrlFlowAnalyzer analyzerCF = new CtrlFlowAnalyzer(UNIT_NAME, log);
         cu.accept(analyzerCF);

         MethodData methodData = processLongestSequence(analyzerCF.getdefUseMap(), //
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
      System.out.println(String.format("[DBG] %-40s: %d", "# of control flow analysis", counterCtrlFlow));
      UtilFile.saveToFile(OUTPUT_DIR + "/" + OUTPUT_CTRL_FLOW_METHODS_MASK, ctrlFlowfuncMaskedList);
   }

   private void validateCtrlFlowData() throws Exception {
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] ------- Validate control flow data -------------------");
      MainValidateMaskedMethods validate = new MainValidateMaskedMethods(INPUT_DIR, OUTPUT_DIR, false);
      validate.highlightDifferences(this.ctrlFlowAnalList, this.ctrlFlowfuncMaskedList, this.ctrlFlowCallNamesMaskedList, OUTPUT_DIFF_CTRL_FLOW);
   }

   private void maskDataFlowData() throws Exception {
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] ------- Mask data flow data --------------------------");
      timeNow(ZonedDateTime.now(), "Current Time: ");

      dataFlowfuncMaskedList = new ArrayList<String>();
      dataFlowVarsMasked = new ArrayList<String>();

      for (int iFunLine = 0; iFunLine < this.dataFlowAnalList.size(); iFunLine++) {
         String theLineOfFunction = this.dataFlowAnalList.get(iFunLine);
         ASTParser parser = UtilAST.parseSrcCode(theLineOfFunction, UNIT_NAME + ".java");
         CompilationUnit cu = (CompilationUnit) parser.createAST(null);
         DataFlowAnalyzer dataFlowAnalyzer = new DataFlowAnalyzer(cu);
         cu.accept(dataFlowAnalyzer);
         Map<IBinding, Integer> counterVarUsage = dataFlowAnalyzer.getCounterVarUsage();
         Entry<IBinding, Integer> maxEntry = findMaxEntry(counterVarUsage);
         // check data integrity.
         if (checkDataIntegrityDataFlowAnalysis(maxEntry) == false) {
            throw new RuntimeException("[ERR] Data Error!! - " + theLineOfFunction);
         }

         Map<IBinding, List<Integer>> offsetOfVarUsage = dataFlowAnalyzer.getOffsetOfVarUsage();
         String var = maxEntry.getKey().getName();
         dataFlowVarsMasked.add(var);

         Integer varUseCount = maxEntry.getValue();
         List<Integer> offsets = offsetOfVarUsage.get(maxEntry.getKey());
         String funcMasked = replaceTokensWithMask(theLineOfFunction, offsets, var, MASK_TOKEN);

         if (log) {
            System.out.println("[DBG] " + String.format("var(%s), count(%d), offset(%s)", var, varUseCount, offsets.toString()));
            System.out.println("[DBG] " + theLineOfFunction);
            System.out.println("[DBG] " + funcMasked);
         }
         dataFlowfuncMaskedList.add(funcMasked);
         // printStringWithOffsets(theLineOfFunction);
      }
      System.out.println(String.format("[DBG] %-40s: %d", "# of functions with data flow masked: ", dataFlowfuncMaskedList.size()));
      UtilFile.saveToFile(OUTPUT_DIR + "/" + OUTPUT_DATA_FLOW_METHODS_MASK, dataFlowfuncMaskedList);
   }

   private void validateDataFlowData() throws Exception {
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] ------- Validate data flow data ----------------------");
      MainValidateMaskedMethods validate = new MainValidateMaskedMethods(INPUT_DIR, OUTPUT_DIR, false);
      validate.highlightDifferences(this.dataFlowAnalList, this.dataFlowfuncMaskedList, this.dataFlowVarsMasked, OUTPUT_DIFF_DATA_FLOW);
   }

   private void maskOtherDataFlowData() throws Exception {
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] ------- Mask other data flow data --------------------");
      timeNow(ZonedDateTime.now(), "Current Time: ");

      OuterMostMethodTransformer transformer = new OuterMostMethodTransformer();
      otherModifiedDataFlowAnalList = new ArrayList<String>();
      otherDataFlowfuncMaskedList = new ArrayList<String>();
      otherDataFlowVarsMasked = new ArrayList<String>();

      List<String> funcMaskedVarList = new ArrayList<>();
      List<String> funcMaskedParmList = new ArrayList<>();
      List<Integer> numMaskPerFunc = new ArrayList<>();

      int countCorret = 0;
      for (int iFunLine = 0; iFunLine < this.otherDataFlowAnalList.size(); iFunLine++) {
         String theLineOfFunction = this.otherDataFlowAnalList.get(iFunLine);
         String modifiedOuterMostMethod = transformer.modifyOuterMostMethod(iFunLine, theLineOfFunction);
         otherModifiedDataFlowAnalList.add(modifiedOuterMostMethod);
         /* [Important Info]  
          * Note: We do not check whether the binding is null because JDT does not support bindings in certain  
          * cases, such as methods excluded from earlier control and data flow analyses.  
          * Instead, we ensure that the modified code is syntactically valid by following these two steps:  
          */

         // Step 1.
         CheckParsing checkParsable = UtilAST.checkParsable(modifiedOuterMostMethod, UNIT_NAME);
         if (checkParsable.equals(CheckParsing.Pass) == false) {
            // ** Wrong if you're here.
            System.out.println("[DBG] checkParsable.equals(CheckParsing.Pass) == false");
            System.out.println("[DBG] " + checkParsable);
            System.out.println("[DBG] " + theLineOfFunction);
            System.out.println("[DBG] " + modifiedOuterMostMethod);
         }
         // Step 2. ensure no problems by comparing # of tokens between org and update.
         String parseAndToStr = parseAndToStr(modifiedOuterMostMethod, UNIT_NAME);
         int count1 = countNonSpaceCharacters(parseAndToStr);
         int count2 = countNonSpaceCharacters(modifiedOuterMostMethod);
         if (count1 == count2) {
            countCorret++;
         }
         else {
            // ** Wrong if you're here.
            System.out.println("[DBG] " + theLineOfFunction);
            System.out.println("[DBG] " + modifiedOuterMostMethod);
            System.out.println("[DBG] " + parseAndToStr);
         }
         // Step 3. Replace the target variable with [MASK]
         String varNameInsert = transformer.getVarNameInsert();
         String funcMasked = modifiedOuterMostMethod.replaceAll(varNameInsert, "PKI_RPLC");
         //int numMaskKWord = funcMasked.split("\\[MASK\\]").length - 1;
         int numMaskKWord = funcMasked.split("PKI_RPLC").length - 1;
         if (numMaskKWord > 1) {
            // Step 3a. Replace `variable` with [MASK]
            funcMaskedVarList.add(funcMasked);
            numMaskPerFunc.add(numMaskKWord);
            this.otherDataFlowfuncMaskedList.add(funcMasked);
            this.otherDataFlowVarsMasked.add(varNameInsert);
         }
         else {
            // Step 3a. Replace `first method param` with [MASK]
            String firstMethodParm = transformer.getFirstMethodParm();
            String methodCallName = transformer.getMethodCallName();
            int[] firstParamPosition = transformer.getFirstParamPosition();

            funcMasked = replaceMaskFirstParm(modifiedOuterMostMethod, methodCallName, firstMethodParm, firstParamPosition);
            this.otherDataFlowfuncMaskedList.add(funcMasked);
            this.otherDataFlowVarsMasked.add(firstMethodParm);

            if (funcMasked == null) {
               // ** Wrong if you're here.
               System.out.println("[DBG] index: " + iFunLine);
               System.out.println("[DBG] " + theLineOfFunction);
               System.out.println("[DBG] " + modifiedOuterMostMethod);
               System.out.println("[DBG] " + parseAndToStr);
            }

            numMaskKWord = funcMasked.split("\\[MASK\\]").length - 1;
            if (numMaskKWord > 1) {
               numMaskPerFunc.add(numMaskKWord);
               funcMaskedParmList.add(funcMasked);
            }
         }
      }

      int fistValue = numMaskPerFunc.get(0);
      boolean allSame = numMaskPerFunc.stream().allMatch(value -> value.equals(numMaskPerFunc.get(0)));

      int width = 40;
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of Func", this.otherDataFlowAnalList.size()));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of method with Param", transformer.getMethodsWithParm()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of method with Return", transformer.getMethodsWithReturnStmt()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of method modified in Try", transformer.getMethodsWithTryBlock()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of method modified in For", transformer.getMethodsWithForStmt()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of method modified in If", transformer.getMethodsWithIfStmt()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of method modified in Body end", transformer.getMethodsWithBody()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of other cases (should be `0`)", transformer.getCounterOtherCases()));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of syntax correct: ", countCorret));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of params masked: ", funcMaskedParmList.size()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of variables masked: ", funcMaskedVarList.size()));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-" + width + "s: %b (%d)", "# of " + MASK_TOKEN + " in each func are same? ", allSame, fistValue));

      checkOccurrences(numMaskPerFunc);

      System.out.println(String.format("[DBG] %-" + width + "s: %b", "All data result correct?", //
            (this.otherDataFlowAnalList.size() == (transformer.getCounterOtherCases() + transformer.getMethodsWithParm() + //
                  transformer.getMethodsWithReturnStmt() + transformer.getMethodsWithTryBlock() + transformer.getMethodsWithForStmt() + //
                  transformer.getMethodsWithIfStmt() + transformer.getMethodsWithBody()))));
      System.out.println("[DBG] ------------------------------------------------------");

      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of func of other data flow masked: ", otherDataFlowfuncMaskedList.size()));
      UtilFile.saveToFile(OUTPUT_DIR + "/" + OUTPUT_OTHER_METHODS_MASK, otherDataFlowfuncMaskedList);
   }

   private void validateOtherDataFlowData() throws Exception {
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] ------- Validate other data flow data ----------------");
      MainValidateMaskedMethods validate = new MainValidateMaskedMethods(INPUT_DIR, OUTPUT_DIR, false);
      validate.highlightDifferences(this.otherModifiedDataFlowAnalList, this.otherDataFlowfuncMaskedList, this.otherDataFlowVarsMasked, OUTPUT_DIFF_OTHERS);
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

   String parseAndToStr(String source, String unitName) {
      ASTParser astParser = UtilAST.parseSrcCode(source, unitName);
      CompilationUnit cu = (CompilationUnit) astParser.createAST(null);
      String parsedContents = cu.toString().trim();
      return parsedContents;
   }

   int countNonSpaceCharacters(String input) {
      String str = input.replace(" ", "").replace("*", "").replace("\n", "").replace("\r", "");
      return str.length();
   }

   void checkOccurrences(List<Integer> occurrenceCollect) {
      int width = 40;
      Map<Integer, Long> occurrences = occurrenceCollect.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
      occurrences.forEach((key, value) -> System.out.println(String.format("[DBG] %-" + width + "s: `%d` occurs %d", "# of occurrence: ", key, value)));
      // System.out.println("'" + key + "' occurs " + value));
   }
   
   String replaceMaskFirstParm(String input, String methodCallName, String parameter, int[] firstParamPosition) {
      String result = null;
      String pattern = methodCallName + "\\d+\\(" + parameter + "\\)";
  
      Pattern regex = Pattern.compile(pattern);
      Matcher matcher = regex.matcher(input);
  
      if (matcher.find()) {
          int matchStart = matcher.start();
          String matchedSubstring = matcher.group();
  
          int openParenOffset = matchedSubstring.indexOf('(') + matchStart;
          int closeParenOffset = matchedSubstring.indexOf(')') + matchStart;
  
          // Display the parameter instead of replacing it
          String paramToDisplay = input.substring(firstParamPosition[0], firstParamPosition[0] + firstParamPosition[1]);
          System.out.println("Parameter Found: " + paramToDisplay);
  
          // Construct the result without modifying the input
          result = input;
  
      } else {
          System.out.println("[DBG] " + input);
          throw new RuntimeException("[ERR] Data Integrity Error!!!");
      }
      return result;
  }  

   // String replaceMaskFirstParm(String input, String methodCallName, String parameter, int[] firstParamPosition) {
   //    String result = null;
   //    String pattern = methodCallName + "\\d+\\(" + parameter + "\\)";

   //    Pattern regex = Pattern.compile(pattern);
   //    Matcher matcher = regex.matcher(input);

   //    if (matcher.find()) {
   //       int matchStart = matcher.start();
   //       String matchedSubstring = matcher.group();

   //       int openParenOffset = matchedSubstring.indexOf('(') + matchStart;
   //       int closeParenOffset = matchedSubstring.indexOf(')') + matchStart;

   //       // System.out.println("Match: " + matchedSubstring);
   //       // System.out.println("Offset of '(': " + openParenOffset);
   //       // System.out.println("Offset of ')': " + closeParenOffset);

   //       // ** Replace a method call
   //       String bgnPart = input.substring(0, openParenOffset + 1);
   //       String endPart = input.substring(closeParenOffset);
   //       String replacedMethodCall = bgnPart + MASK_TOKEN + endPart;

   //       // ** Replace the first parameter of the method
   //       bgnPart = replacedMethodCall.substring(0, firstParamPosition[0]);
   //       endPart = replacedMethodCall.substring(bgnPart.length() + firstParamPosition[1]);
   //       result = bgnPart + MASK_TOKEN + endPart;

   //       // System.out.println("[DBG] " + input);
   //       // System.out.println("[DBG] " + bgnPart + MASK_TOKEN + endPart);
   //       // System.out.println("Match found.");
   //    }
   //    else {
   //       System.out.println("[DBG] " + input);
   //       throw new RuntimeException("[ERR] Data Integrity Error!!!");
   //    }
   //    return result;
   // }

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

   void printStringWithOffsets(String input) {
      for (int i = 0; i < input.length(); i++) {
         System.out.println("Character: '" + input.charAt(i) + "' at offset: " + i);
      }
   }

   boolean checkDataIntegrityDataFlowAnalysis(Entry<IBinding, Integer> maxEntry) {
      boolean check = (maxEntry != null) && (maxEntry.getKey() != null) && (maxEntry.getValue() > 0);
      return check;
   }

   private Entry<IBinding, Integer> findMaxEntry(Map<IBinding, Integer> counterVarUsage) {
      Map.Entry<IBinding, Integer> maxEntry = null;
      for (Map.Entry<IBinding, Integer> entry : counterVarUsage.entrySet()) {
         if (maxEntry == null || entry.getValue() > maxEntry.getValue()) {
            maxEntry = entry;
         }
      }
      return maxEntry;
   }

   /***
    * Fixed version.
    */
   MethodData processLongestSequence(Map<IVariableBinding, DefUseModel> analysisDataMap, String methodName, int index, String inputLine) {
      if (log) {
         System.out.println("[DBG] method: " + methodName);
      }
      Map<String, List<String>> allMethodCallsInFunc = new LinkedHashMap<>();
      Map<String, List<String>> allFieldCallsInFunc = new LinkedHashMap<>();
      Map<String, List<String>> allMethodAndFieldCallsInFunc = new LinkedHashMap<>();
      int varIndex = 0;

      for (Entry<IVariableBinding, DefUseModel> entry : analysisDataMap.entrySet()) {
         // IVariableBinding iBinding = entry.getKey();
         DefUseModel iVariableAnal = entry.getValue();

         String variableName = "";

         if (iVariableAnal.getSingleVarDecl() != null) {
            SingleVariableDeclaration paramDecl = iVariableAnal.getSingleVarDecl();
            variableName = paramDecl.getName().getIdentifier();
         }

         if (iVariableAnal.getVarDeclFrgt() != null) {
            VariableDeclarationFragment varDecl = iVariableAnal.getVarDeclFrgt();
            variableName = varDecl.getName().getIdentifier();
         }
         String varIndexName = String.format("(%d)-%s", varIndex, variableName);

         // Step 1. Collecting method calls within the current variable.
         List<String> methodCalls = new ArrayList<String>();

         for (MethodInvocation method : iVariableAnal.getMethodInvocations()) {
            if (method.getExpression() instanceof SimpleName) {
               SimpleName variable = (SimpleName) method.getExpression();
               if (variable.getIdentifier().equals(variableName)) {
                  String methodCall = variable.getIdentifier() + "." + method.getName() + "()";
                  methodCalls.add(methodCall);
               }
            }
         }

         // Step 2. Collecting field access calls within the current variable.
         List<String> fieldCalls = new ArrayList<String>();

         for (FieldAccess fieldAccess : iVariableAnal.getFieldAccesses()) {
            String fieldAccessCall = variableName + "." + fieldAccess.getName().getIdentifier();
            fieldCalls.add(fieldAccessCall);
         }

         // Step 3-a. Collecting all calls within the current function to find longest call sequence.
         if (methodCalls.isEmpty() == false) {
            allMethodCallsInFunc.put(varIndexName, methodCalls);
            allMethodAndFieldCallsInFunc.put(varIndexName, methodCalls);
         }
         // Step 3-b. Collecting all calls within the current function to find longest call sequence.
         if (fieldCalls.isEmpty() == false) {
            allFieldCallsInFunc.put(varIndexName, fieldCalls);
            allMethodAndFieldCallsInFunc.put(varIndexName, fieldCalls);
         }
         varIndex++;
         if (log) {
            System.out.println(String.format("[DBG] %-15s %-25s %-30s", //
                  varIndexName, "mth-inv: " + iVariableAnal.getMethodInvocations().size(), //
                  "distinct mth-inv: " + (new ArrayList<>(new LinkedHashSet<>(methodCalls))).size()));
         }

      }
      String varKeyofLongestMethodFieldCallUniq = findLongestListUniqIndex(allMethodAndFieldCallsInFunc);
      List<String> methodCalls = allMethodCallsInFunc.get(varKeyofLongestMethodFieldCallUniq);
      List<String> fieldCalls = allFieldCallsInFunc.get(varKeyofLongestMethodFieldCallUniq);
      List<String> methodFieldCalls = allMethodAndFieldCallsInFunc.get(varKeyofLongestMethodFieldCallUniq);

      if (log) {
         System.out.println("[DBG] -------- Longest Method / Field Call ((Uniq)) --------");
         System.out.println("[DBG] " + methodCalls);
         System.out.println("[DBG] " + fieldCalls);
         System.out.println("[DBG] " + methodFieldCalls);
         System.out.println("[DBG] ------------------------------------------------------");
      }
      int maxMethodCalls = 0, maxFieldAccesses = 0, maxTotalCallsAndAccesses = 0;
      String seqOfMethodCalls = "";

      if (methodCalls != null && methodCalls.isEmpty() == false) {
         maxMethodCalls = methodCalls.size();
         seqOfMethodCalls = methodCalls.stream().collect(Collectors.joining(";"));
      }
      if (fieldCalls != null && fieldCalls.isEmpty() == false) {
         maxFieldAccesses = fieldCalls.size();
      }
      if (methodFieldCalls != null && methodFieldCalls.isEmpty() == false) {
         maxTotalCallsAndAccesses = methodFieldCalls.size();
      }

      return new MethodData(index, methodName, inputLine, maxMethodCalls, //
            maxFieldAccesses, maxTotalCallsAndAccesses, seqOfMethodCalls);
   }

   String findLongestListUniqIndex(Map<String, List<String>> map) {
      int maxSize = 0;
      String keyLongest = null;

      List<String> keys = new ArrayList<>(map.keySet());
      for (int i = 0; i < keys.size(); i++) {
         String key = keys.get(i);
         List<String> list = map.get(key);

         List<String> uniqueList = new ArrayList<>(new HashSet<>(list));

         if (uniqueList.size() > maxSize) {
            maxSize = uniqueList.size();
            keyLongest = key;
         }
      }
      return keyLongest; //
   }

   class MethodData {
      int index;
      String methodName;
      String inputLine;
      int maxMethodCalls;
      int maxFieldAccesses;
      int maxTotalCallsAndAccesses;
      String longestSequence;

      MethodData(int index, String methodName, String inputLine, int maxMethodCalls, //
            int maxFieldAccesses, int maxTotalCallsAndAccesses, String longestSequence) {
         this.index = index;
         this.methodName = methodName;
         this.inputLine = inputLine;
         this.maxMethodCalls = maxMethodCalls;
         this.maxFieldAccesses = maxFieldAccesses;
         this.maxTotalCallsAndAccesses = maxTotalCallsAndAccesses;
         this.longestSequence = longestSequence;
      }
   }
}
