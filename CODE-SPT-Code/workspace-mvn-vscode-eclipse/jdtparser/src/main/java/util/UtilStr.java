package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UtilStr {
   public static String escapeSpecialRegexChars(String str) {
      String[] specialChars = { "\\", "^", "$", ".", "|", "?", "*", "+", "(", ")", "[", "{", "]", "}" };
      for (String specialChar : specialChars) {
         str = str.replace(specialChar, "\\" + specialChar);
      }
      return str;
   }

   public static String rmWHSpaces(String s) {
      return s.replaceAll("\\s+", "");
   }

   public static String rmComments(String s) {
      s = rmBlockComments(s);

      return rmSingleComments(s);
   }
   
   public static String rmnewlinecharacters(String s) {
	   s = s.replace("\n", "").replace("\r", "").replace("\t", "");
	   return s;
   }

   public static String rmSingleComments(String s) {
	   // String pattern = "(?<!:)//[^\n]*";//"(?<!https:|http:)//.*";
	   // return s.replaceAll(pattern, "");
      //String pattern = "(\"[^\"]*\"|'[^']*')|(?<!:)//[^\n]*";
      String pattern = "//.*|(\\\"(?:(?<!\\\\\\\\)(?:\\\\\\\\\\\\\\\\)*\\\\\\\\\\\"|[^\\r\\n" + //
                  "\\\"])*\\\")";
      //"(\"(\\\\\"|[^\"])*\")|(?<!:)//[^\n]*";
      return s.replaceAll(pattern, "$1");
   }

//    public static String rmSingleComments(String s) {
//       // Pattern to match string literals or comments
//       String pattern = "\"([^\"]|\\\\\")*\"|(?<!(http:|https:))//.*"; //"\"([^\"]|\\\\\")*\"|(?<!:|http|https)//[^\n\r]*";
//       // Create a pattern object
//       Pattern p = Pattern.compile(pattern);
//       // Create a matcher object
//       Matcher m = p.matcher(s);
//       StringBuffer sb = new StringBuffer();
  
//       while (m.find()) {
//          if (m.group().startsWith("\"")) {
//              m.appendReplacement(sb, Matcher.quoteReplacement(m.group()));
//          } else {
//              m.appendReplacement(sb, "");
//          }
//      }
//      m.appendTail(sb);
//      return sb.toString();
//   }  
  
   public static String rmBlockComments(String s) {
      Pattern patternComments = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
      Matcher matcherComments = patternComments.matcher(s);
      s = matcherComments.replaceAll("");
      return s;
   }

   public static String generalizeStr(String s) {
      Pattern patternGenStr = Pattern.compile("([bruf]*)(\"\"\"|'''|\"|')(?:(?!\\2)(?:\\\\.|[^\\\\]))*\\2");
      Matcher matcher = patternGenStr.matcher(s);
      return matcher.replaceAll("\"__STR__CONST__\"");
   }
}
