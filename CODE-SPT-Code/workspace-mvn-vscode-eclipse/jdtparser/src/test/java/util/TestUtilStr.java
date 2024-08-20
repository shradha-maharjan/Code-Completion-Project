package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class TestUtilStr {
   public void testRmBlockComment_Case0() {
      String input = "/* abc */ void m1() { m2();}";
      String result = UtilStr.rmBlockComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmBlockComment_Case1() {
      String input = "/* abc */ \n" //
            + "void m1() { \n" //
            + "m2();" //
            + "}";
      String result = UtilStr.rmBlockComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmBlockComment_Case2() {
      String input = "/* abc */ \n" //
            + "void m1( /* int val */ ) { \n" //
            + "m2();" //
            + "}";
      String result = UtilStr.rmBlockComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmBlockComment_Case3() {
      String input = "/* abc */ \n" //
            + "void m1( /* // int val */ ) { \n" //
            + "m2();" //
            + "}";
      String result = UtilStr.rmBlockComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmBlockComment_Case4() {
      String input = "abc */ \n" //
            + "void m1( /* // int val */ ) { \n" //
            + "m2();" //
            + "}";
      String result = UtilStr.rmBlockComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmSingleComment_Case0() {
      String input = "// abc";
      String result = UtilStr.rmSingleComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmSingleComment_Case1() {
      String input = "def; // abc";
      String result = UtilStr.rmSingleComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmSingleComment_Case2() {
      String input = "m1(); // comments for m1 \n m2(); // comments for m2";
      String result = UtilStr.rmSingleComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testRmSingleComment_Case3() {
      String input = "m1(); // comments for m1     m2(); // comments for m2";
      String result = UtilStr.rmSingleComments(input);
      System.out.println("[DBG] " + result);
   }

   public void testGeneralizeStr_Case0() {
      String input = "m1(\"abc\"); // comments for m1 \"abc\"  \nm2(); // comments for m2";
      String result = UtilStr.generalizeStr(input);
      System.out.println("[DBG] " + result);

   }

   // public static String generalizeStr(String s) {
   // // Pattern patternGenStr = Pattern.compile("([bruf]*)(\"\"\"|'''|\"|')(?:(?!\\2)(?:\\\\.|[^\\\\]))*\\2");
   // // Matcher matcher = patternGenStr.matcher(s);
   // // return matcher.replaceAll("__STR__CONST__");
   //
   // // Pattern patternGenStr = Pattern.compile("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*')");
   // // Matcher matcher = patternGenStr.matcher(s);
   // // return matcher.replaceAll("__STR__CONST__");
   //
   // // Pattern patternGenStr = Pattern.compile("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'|\"\"\"(?:\\\\.|[^\"\\\\]|\\n)*?\"\"\"|'''(?:\\\\.|[^'\\\\]|\\n)*?''')");
   // // Matcher matcher = patternGenStr.matcher(s);
   // // return matcher.replaceAll("__STR__CONST__");
   // Pattern patternGenStr = Pattern.compile("\"(?:\\\\.|[^\"])*\"");
   // Matcher matcher = patternGenStr.matcher(s);
   // return matcher.replaceAll("__STR__CONST__");
   // }

   public static String generalizeStr(String s) {
      Pattern patternGenStr = Pattern.compile("\"(?:\\\\.|[^\"])*\"");
      Matcher matcher = patternGenStr.matcher(s);
      return matcher.replaceAll("__STR__CONST__");
   }

   public static String generalizeChar(String s) {
      Pattern patternGenChar = Pattern.compile("'[^'\\\\]*(?:\\\\.[^'\\\\]*)*'");
      Matcher matcher = patternGenChar.matcher(s);
      return matcher.replaceAll("__CHAR__CONST__");
   }

   // @Test
   public void testGeneralizeStr_Case1() {
      String input = "/* ' */  \" 'some' \";";
      String expected = "/* ' */ __STR__CONST__;";

      System.out.println("[DBG] INPUT   : " + input);
      String result = generalizeStr(input);
      System.out.println("[DBG] Actual  : " + result);
      System.out.println("[DBG] Expected: " + expected);
   }

   public void testGeneralizeChar_Case1() {
      String input = "/* ' */  'a' + 'b' + 'c' + \" 'some' \";";
      String expectedStr = "/* ' */  'a' + 'b' + 'c' + __STR__CONST__;";

      System.out.println("[DBG] INPUT   : " + input);
      String resultStr = generalizeStr(input);
      System.out.println("[DBG] Actual  : " + resultStr);
      System.out.println("[DBG] Expected: " + expectedStr);
      assert expectedStr.equals(resultStr) : "generalizeStr test failed";
      System.out.println("[DBG] ------------------------------------------------------");

   }

   @Test
   public void testGeneralizeChar_Case2() {
      String input = "/* ' */  'a' + 'b' + 'c' + \" 'some' \";";
      String expectedChar = "/* ' */  __CHAR__CONST__ + __CHAR__CONST__ + __CHAR__CONST__ + \" 'some' \";";

      System.out.println("[DBG] INPUT   : " + input);
      String resultChar = generalizeChar(input);
      System.out.println("[DBG] Actual  : " + resultChar);
      System.out.println("[DBG] Expected: " + expectedChar);
      assert expectedChar.equals(resultChar) : "generalizeChar test failed";
   }

   public void testGeneralizeStr_Case2() {
      String input = "/** \n" + " * Do nothing: We hold a single internal BeanFactory and rely on callers to register beans through our public methods (or the BeanFactory's).\n" + " * @see #registerBeanDefinition\n" + " */\n" + "@Override protected final void refreshBeanFactory() throws IllegalStateException {\n" + "  if (!this.refreshed.compareAndSet(false,true)) {\n" + "    throw new IllegalStateException(\"GenericApplicationContext does not support multiple refresh attempts: just call 'refresh' once\");\n" + "  }\n" + "  this.beanFactory.setSerializationId(getId());\n" + "}";
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] " + input);
      String result = UtilStr.generalizeStr(UtilStr.rmComments(input));
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] " + result);
      System.out.println("[DBG] ------------------------------------------------------");
      //
      // result = UtilStr.rmWHSpaces(UtilStr.rmComments(UtilStr.generalizeStr(input)));
      // System.out.println("[DBG] " + result);

   }

   public void testSplit() {
      String input = "/** \n" + " * Adds an http transport implementation that can be selected by setting  {@link #HTTP_TYPE_KEY}. \n" + " */\n" + "public void registerHttpTransport(String name,Class<? extends HttpServerTransport> clazz){\n" + "  if (transportClient) {\n" + "    throw new IllegalArgumentException(\"Cannot register http transport \" + clazz.getName() + \" for transport client\");\n" + "  }\n" + "  httpTransportTypes.registerExtension(name,clazz);\n" + "}";
      String spliter = "\"Cannot register http transport \" + clazz.getName() + \" for transport client\"";
      spliter = UtilStr.escapeSpecialRegexChars(spliter);
      // String spliter = "myDocument\\.getLineStartOffset\\(myCurrentStartLogicalLine\\)";

      String[] splits = input.split(spliter);

      System.out.println("[DBG] " + splits.length);

      for (String iElem : splits) {
         System.out.println("[DBG] " + iElem);
      }
   }

}
