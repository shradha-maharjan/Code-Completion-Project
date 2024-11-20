package datactrlflow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.junit.Test;

import util.CheckParsing;
import util.UtilAST;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.management.RuntimeErrorException;

public class TestOuterMostMethodTransformerV2 {

   private static final String UNIT_NAME = "ParsedAndToString";

   private static final String MASK_TOKEN = "[MASK]";

   String varNameInsert = "myvar";
   int counterOtherCases = 0, methodsWithParm = 0, methodsWithReturnStmt = 0;
   int methodsWithTryBlock = 0, methodsWithForStmt = 0, methodsWithIfStmt = 0;
   int methodsWithBody = 0, countCorret = 0;

   @Test
   public void testFindMethodDecl() throws IOException {
      OuterMostMethodTransformer transformer = new OuterMostMethodTransformer();

      String theLineOfFunction = null;
      // String filePath = "output/classify/sameple-others500.txt";
      String filePath = "output/classify/pretrain-fun-others-arti-keep.txt";
      System.out.println("[DBG] Input: " + filePath);

      List<String> readAllLines = Files.readAllLines(Paths.get(filePath));
      List<String> funcMaskedVarList = new ArrayList<>();
      List<String> funcMaskedParmList = new ArrayList<>();
      List<Integer> numMaskPerFunc = new ArrayList<>();

      for (int iFunLine = 0; iFunLine < readAllLines.size(); iFunLine++) {

         theLineOfFunction = readAllLines.get(iFunLine);
         String modifiedOuterMostMethod = transformer.modifyOuterMostMethod(iFunLine, theLineOfFunction);

         // *** [Important Info] ***
         // We do not check if binding is null or not since JDT does not support
         // binding in other method cases which is excluded from the earlier control and data analysis.
         // Instead, we check if the modified code is syntactically valid by using the following 2 steps.
         // ************************
         // Step 1.
         CheckParsing checkParsable = UtilAST.checkParsable(modifiedOuterMostMethod, UNIT_NAME);
         if (checkParsable.equals(CheckParsing.Pass) == false) {
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
         String funcMasked = modifiedOuterMostMethod.replaceAll(varNameInsert, MASK_TOKEN);
         int numMaskKWord = funcMasked.split("\\[MASK\\]").length - 1;
         if (numMaskKWord > 1) {
            // Step 3a. Replace `variable` with [MASK]
            numMaskPerFunc.add(numMaskKWord);
            funcMaskedVarList.add(funcMasked);
         }
         else {
            // Step 3a. Replace `first method param` with [MASK]
            String firstMethodParm = transformer.getFirstMethodParm();
            String methodCallName = transformer.getMethodCallName();
            int[] firstParamPosition = transformer.getFirstParamPosition();

            funcMasked = replaceMaskeFirstParm(modifiedOuterMostMethod, methodCallName, firstMethodParm, firstParamPosition);

            if (funcMasked == null) {
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
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of Func", readAllLines.size()));
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
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of variables masked: ", funcMaskedVarList.size()));
      System.out.println(String.format("[DBG] %-" + width + "s: %d", "# of params masked: ", funcMaskedParmList.size()));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println(String.format("[DBG] %-" + width + "s: %b (%d)", "# of " + MASK_TOKEN + " in each func are same? ", allSame, fistValue));

      checkOccurrences(numMaskPerFunc);

      System.out.println(String.format("[DBG] %-" + width + "s: %b", "All data result correct?", //
            (readAllLines.size() == (transformer.getCounterOtherCases() + transformer.getMethodsWithParm() + //
                  transformer.getMethodsWithReturnStmt() + transformer.getMethodsWithTryBlock() + transformer.getMethodsWithForStmt() + //
                  transformer.getMethodsWithIfStmt() + transformer.getMethodsWithBody()))));
      System.out.println("[DBG] ------------------------------------------------------");
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

   String replaceMaskeFirstParm(String input, String methodCallName, String parameter, int[] firstParamPosition) {
      String result = null;
      String pattern = methodCallName + "\\d+\\(" + parameter + "\\)";

      Pattern regex = Pattern.compile(pattern);
      Matcher matcher = regex.matcher(input);

      if (matcher.find()) {
         int matchStart = matcher.start();
         String matchedSubstring = matcher.group();

         int openParenOffset = matchedSubstring.indexOf('(') + matchStart;
         int closeParenOffset = matchedSubstring.indexOf(')') + matchStart;

         // System.out.println("Match: " + matchedSubstring);
         // System.out.println("Offset of '(': " + openParenOffset);
         // System.out.println("Offset of ')': " + closeParenOffset);

         // ** Replace a method call
         String bgnPart = input.substring(0, openParenOffset + 1);
         String endPart = input.substring(closeParenOffset);
         String replacedMethodCall = bgnPart + MASK_TOKEN + endPart;

         // ** Replace the first parameter of the method
         bgnPart = replacedMethodCall.substring(0, firstParamPosition[0]);
         endPart = replacedMethodCall.substring(bgnPart.length() + firstParamPosition[1]);
         result = bgnPart + MASK_TOKEN + endPart;

         // System.out.println("[DBG] " + input);
         // System.out.println("[DBG] " + bgnPart + MASK_TOKEN + endPart);
         // System.out.println("Match found.");
      }
      else {
         System.out.println("[DBG] " + input);
         throw new RuntimeException("[ERR] Data Integrity Error!!!");
      }
      return result;
   }

   // @Test
   public void countOccurrence() {
      List<Integer> numMaskPerFunc = Arrays.asList(1, 1, 2, 2, 2);
      checkOccurrences(numMaskPerFunc);
   }

   // @Test
   public void check() {
      String parameter = "param";
      String methodCallName = "___pkimethod___";
      String input = "class ParsedAndToString { @CanIgnoreReturnValue @Deprecated @Override public V forcePut( K key, V value){___pkimethod___1(key); throw new UnsupportedOperationException(); } }";
      replaceMaskeFirstParm(input, methodCallName, parameter, null);
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
