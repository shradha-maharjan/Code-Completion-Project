package util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;

/**
 */
public class UtilAST {
   static final int INVALID_DOC = -1;
   static String fileContents = null;

   @SuppressWarnings("deprecation")
   public static ASTParser parse() {
      ASTParser parser = ASTParser.newParser(AST.JLS16);
      configParser(parser);
      return parser;
   }

   public static ASTParser parse(String javaFilePath) {
      String source = null;
      try {
         source = UtilFile.readEntireFile(javaFilePath);
      } catch (IOException e) {
         e.printStackTrace();
      }

      ASTParser parser = parse();
      parser.setUnitName(UtilFile.getShortFileName(javaFilePath));
      parser.setEnvironment(null, null, null, true);
      parser.setSource(source.toCharArray());
      parser.setSourceRange(0, source.length());
      return parser;
   }

   public static ASTParser parseSrcCode(String source, String unitName) {
      ASTParser parser = parse();
      parser.setUnitName(unitName);
      parser.setEnvironment(null, null, null, true);
      parser.setSource(source.toCharArray());
      parser.setSourceRange(0, source.length());
      return parser;
   }

   public static CompilationUnit parse(ICompilationUnit unit) {
      ASTParser parser = parse();
      parser.setSource(unit);
      return (CompilationUnit) parser.createAST(null); // parse
   }

   private static void configParser(ASTParser parser) {
      parser.setResolveBindings(true);
      parser.setKind(ASTParser.K_COMPILATION_UNIT);
      parser.setBindingsRecovery(true);
      Map<String, String> options = JavaCore.getOptions();
      options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
      options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);
      options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
      parser.setCompilerOptions(options);
   }

   public static boolean contains(ICompilationUnit iUnit, String typeName) {
      boolean rst = false;
      try {
         IType[] types = iUnit.getAllTypes();
         for (IType iType : types) {
            String iTypeName = iType.getElementName();
            if (typeName.equals(iTypeName)) {
               rst = true;
               break;
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
      return rst;
   }

   public static CheckParsing checkParsable(String contents, String givenClassName) {
      return UtilASTJavaParser.checkParsable(contents, givenClassName);
   }

   static StringBuilder classNameResult = new StringBuilder();

   public static boolean checkClassName(String code, String givenClassName) {
      classNameResult.setLength(0);

      ASTParser parser = parseSrcCode(code, givenClassName + ".java");
      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
      cu.accept(new ASTVisitor() {
         public boolean visit(TypeDeclaration node) {
            String className = node.getName().toString();
            classNameResult.append(className);
            return true;
         }
      });

      if (givenClassName.equals(classNameResult.toString())) {
         return true;
      }

      return false;
   }

   // ###
   // searchMethod
   // ###

   static TreeMap<Integer, String> mapLineMethod = new TreeMap<Integer, String>();
   public static int counterLeftRightContextNotTwo = 0;
   public static int counterLeftRightContextNotTwo_BAD_METHODS = 0;

   public static LeftRightContext searchMethod(String leftContext, String rightContext, String targetSeq, //
         String srcPath, Long line4Method, CheckParsing checkParsable) throws IOException {

      mapLineMethod.clear();
      String contents = UtilFile.readEntireFile(srcPath);
      org.eclipse.jdt.core.dom.ASTParser parser = UtilAST.parseSrcCode(contents, UtilFile.getShortFileName(srcPath));
      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
      cu.accept(new ASTVisitor() {
         public boolean visit(MethodDeclaration node) {
            try {
               // Debug
               // if (node.toString().contains("int getMinOffset(){")) {
               // System.out.print("");
               // }
               int startPosition = node.getStartPosition();
               int lineNumberAtOffset = UtilFile.getLineNumberAtOffset(srcPath, startPosition);
               mapLineMethod.put(lineNumberAtOffset, node.toString());

            } catch (IOException e) {
               e.printStackTrace();
            }
            return true;
         }
      });
      Integer resultKey = null;
      String searchedMethod = null;
      try {
         resultKey = mapLineMethod.floorKey(line4Method.intValue());
         searchedMethod = mapLineMethod.get(resultKey);
      } catch (java.lang.NullPointerException e) {
         e.printStackTrace();
      }
      String rmWhiteSpaces = UtilStr.rmWHSpaces(UtilStr.rmComments(searchedMethod));
      String rmLeft = UtilStr.rmWHSpaces(UtilStr.rmComments(leftContext));
      String rmRight = UtilStr.rmWHSpaces(UtilStr.rmComments(rightContext));
      String rmTarget = UtilStr.rmWHSpaces(UtilStr.rmComments(targetSeq));

      boolean c1 = rmWhiteSpaces.contains(rmLeft);
      boolean c2 = rmWhiteSpaces.contains(rmRight);
      boolean c3 = rmWhiteSpaces.contains(rmTarget);

      switch (checkParsable) {
      case UNPARSED:
      case PARSE_FAILURE: {
         if (c2 && c3) {
            // Check 1.
            String unitName = "ClassWrapperABCDE";
            String tmpClass = "class " + unitName + " {" + searchedMethod + "}";
            String methodStr = UtilASTJavaParser.getMethod(tmpClass);
            String methodStrRmBlkCmt = UtilStr.rmBlockComments(methodStr);
            //String methodStrRmSngCmt = UtilStr.rmSingleComments(methodStrRmBlkCmt);
            //System.out.println("methodStrRmSngCmt "+methodStrRmSngCmt);
//            String methodStrGen = UtilStr.generalizeStr(methodStrRmSngCmt);
//            String methodStrRmNewline = UtilStr.rmnewlinecharacters(methodStrGen);
            //System.out.println("methodStrRmNewline "+methodStrRmSngCmt);
            String[] leftRightContext = methodStrRmBlkCmt.split(UtilStr.escapeSpecialRegexChars(targetSeq));
            List<String> results = new ArrayList<String>();
            for (int i = 0; i < leftRightContext.length; i++) {
               results.add(leftRightContext[i]);
               //System.out.println(results);
            }
            if (results.size() != 2) {
               counterLeftRightContextNotTwo++;
               System.out.println("[DBG] counterLeftRightContextNotTwo: " + results.size());
            }
            return new LeftRightContext(results.get(0), targetSeq, results.get(1));// searchedMethod;
         }
         break;
      }
      case BAD_METHODS: {
         if (c1 && c2 && c3) {
            // Check 1.
            String[] leftRightContext = searchedMethod.split(UtilStr.escapeSpecialRegexChars(targetSeq));
            if (leftRightContext.length != 2) {
               counterLeftRightContextNotTwo_BAD_METHODS++;
               System.out.println("[DBG] counterLeftRightContextNotTwo_BAD_METHODS: " + leftRightContext.length);
            }
            return new LeftRightContext(leftRightContext[0], targetSeq, leftRightContext[1]);// searchedMethod;
         }
         break;
      }
      default:
      }
      // -----------------------------------------------------------------------
      return null;
   }
}


///**
// */
//package util;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.TreeMap;
//
//import org.eclipse.jdt.core.ICompilationUnit;
//import org.eclipse.jdt.core.IType;
//import org.eclipse.jdt.core.JavaCore;
//import org.eclipse.jdt.core.dom.AST;
//import org.eclipse.jdt.core.dom.ASTParser;
//import org.eclipse.jdt.core.dom.ASTVisitor;
//import org.eclipse.jdt.core.dom.CompilationUnit;
//import org.eclipse.jdt.core.dom.MethodDeclaration;
//import org.eclipse.jdt.core.dom.TypeDeclaration;
//
///**
// */
//public class UtilAST {
//   static final int INVALID_DOC = -1;
//   static String fileContents = null;
//
//   @SuppressWarnings("deprecation")
//   public static ASTParser parse() {
//      ASTParser parser = ASTParser.newParser(AST.JLS16);
//      configParser(parser);
//      return parser;
//   }
//
//   public static ASTParser parse(String javaFilePath) {
//      String source = null;
//      try {
//         source = UtilFile.readEntireFile(javaFilePath);
//      } catch (IOException e) {
//         e.printStackTrace();
//      }
//
//      ASTParser parser = parse();
//      parser.setUnitName(UtilFile.getShortFileName(javaFilePath));
//      parser.setEnvironment(null, null, null, true);
//      parser.setSource(source.toCharArray());
//      parser.setSourceRange(0, source.length());
//      return parser;
//   }
//
//   public static ASTParser parseSrcCode(String source, String unitName) {
//      ASTParser parser = parse();
//      parser.setUnitName(unitName);
//      parser.setEnvironment(null, null, null, true);
//      parser.setSource(source.toCharArray());
//      parser.setSourceRange(0, source.length());
//      return parser;
//   }
//
//   public static CompilationUnit parse(ICompilationUnit unit) {
//      ASTParser parser = parse();
//      parser.setSource(unit);
//      return (CompilationUnit) parser.createAST(null); // parse
//   }
//
//   private static void configParser(ASTParser parser) {
//      parser.setResolveBindings(true);
//      parser.setKind(ASTParser.K_COMPILATION_UNIT);
//      parser.setBindingsRecovery(true);
//      Map<String, String> options = JavaCore.getOptions();
//      options.put(JavaCore.COMPILER_COMPLIANCE, JavaCore.VERSION_11);
//      options.put(JavaCore.COMPILER_CODEGEN_TARGET_PLATFORM, JavaCore.VERSION_11);
//      options.put(JavaCore.COMPILER_SOURCE, JavaCore.VERSION_11);
//      parser.setCompilerOptions(options);
//   }
//
//   public static boolean contains(ICompilationUnit iUnit, String typeName) {
//      boolean rst = false;
//      try {
//         IType[] types = iUnit.getAllTypes();
//         for (IType iType : types) {
//            String iTypeName = iType.getElementName();
//            if (typeName.equals(iTypeName)) {
//               rst = true;
//               break;
//            }
//         }
//      } catch (Exception e) {
//         e.printStackTrace();
//      }
//      return rst;
//   }
//
//   public static CheckParsing checkParsable(String contents, String givenClassName) {
//      return UtilASTJavaParser.checkParsable(contents, givenClassName);
//   }
//
//   static StringBuilder classNameResult = new StringBuilder();
//
//   public static boolean checkClassName(String code, String givenClassName) {
//      classNameResult.setLength(0);
//
//      ASTParser parser = parseSrcCode(code, givenClassName + ".java");
//      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//      cu.accept(new ASTVisitor() {
//         public boolean visit(TypeDeclaration node) {
//            String className = node.getName().toString();
//            classNameResult.append(className);
//            return true;
//         }
//      });
//
//      if (givenClassName.equals(classNameResult.toString())) {
//         return true;
//      }
//
//      return false;
//   }
//
//   // ###
//   // searchMethod
//   // ###
//
//   static TreeMap<Integer, String> mapLineMethod = new TreeMap<Integer, String>();
//   public static int counterLeftRightContextNotTwo = 0;
//   public static int counterLeftRightContextNotTwo_BAD_METHODS = 0;
//
//   public static LeftRightContext searchMethod(String leftContext, String rightContext, String targetSeq, //
//         String srcPath, Long line4Method, CheckParsing checkParsable) throws IOException {
//
//      mapLineMethod.clear();
//      //System.out.println("[DBG] Reading file: " + srcPath);
//      String contents = UtilFile.readEntireFile(srcPath);
//      if (contents == null) {
//          System.out.println("[ERR] Could not read file: " + srcPath);
//          return null;
//      }
//      //System.out.println("[DBG] File contents length: " + contents.length());
//      org.eclipse.jdt.core.dom.ASTParser parser = UtilAST.parseSrcCode(contents, UtilFile.getShortFileName(srcPath));
//      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
//      cu.accept(new ASTVisitor() {
//         public boolean visit(MethodDeclaration node) {
//            try {
//               // Debug
//               // if (node.toString().contains("int getMinOffset(){")) {
//               // System.out.print("");
//               // }
//               int startPosition = node.getStartPosition();
//               int lineNumberAtOffset = UtilFile.getLineNumberAtOffset(srcPath, startPosition);
//               mapLineMethod.put(lineNumberAtOffset, node.toString());
//            } catch (IOException e) {
//               e.printStackTrace();
//            }
//            return true;
//         }
//      });
//      Integer resultKey = null;
//      String searchedMethod = null;
//      try {
//         resultKey = mapLineMethod.floorKey(line4Method.intValue());
//         searchedMethod = mapLineMethod.get(resultKey);
//         //System.out.println("[DBG] Searched method at line: " + resultKey);
//      } catch (java.lang.NullPointerException e) {
//         e.printStackTrace();
//      }
//      
//      if (searchedMethod == null) {
//          System.out.println("[ERR] No method found for line: " + line4Method);
//          return null;
//      }
//
//      //System.out.println("[DBG] Searched method content length: " + searchedMethod.length());
//      String rmWhiteSpaces = UtilStr.rmWHSpaces(UtilStr.rmnewlinecharacters(UtilStr.rmComments(searchedMethod)));
//      String rmLeft = UtilStr.rmWHSpaces(UtilStr.rmnewlinecharacters(UtilStr.rmComments(leftContext)));
//      String rmRight = UtilStr.rmWHSpaces(UtilStr.rmnewlinecharacters(UtilStr.rmComments(rightContext)));
//      String rmTarget = UtilStr.rmWHSpaces(UtilStr.rmnewlinecharacters(UtilStr.rmComments(targetSeq)));
//
//      boolean c1 = rmWhiteSpaces.contains(rmLeft);
//      boolean c2 = rmWhiteSpaces.contains(rmRight);
//      boolean c3 = rmWhiteSpaces.contains(rmTarget);
//
//      switch (checkParsable) {
//      case UNPARSED:
//      case PARSE_FAILURE: {
//         if (c2 && c3) {
//            // Check 1.
//            String unitName = "ClassWrapperABCDE";
//            String tmpClass = "class " + unitName + " {" + searchedMethod + "}";
//            String methodStr = UtilASTJavaParser.getMethod(tmpClass);
//            String methodStrRmBlkCmt = UtilStr.rmBlockComments(methodStr);
//            String methodStrRmSngCmt = UtilStr.rmSingleComments(methodStrRmBlkCmt);
//            String methodStrGen = UtilStr.generalizeStr(methodStrRmSngCmt);
//            String methodStrRmNewline = UtilStr.rmnewlinecharacters(methodStrGen);
//            System.out.println("methodStrRmNewline: " + methodStrRmNewline);
//            String[] leftRightContext = methodStrRmNewline.split(UtilStr.escapeSpecialRegexChars(targetSeq));
//            System.out.println("leftRightContext: " + leftRightContext);
//            List<String> results = new ArrayList<String>();
//            for (int i = 0; i < leftRightContext.length; i++) {
//               results.add(leftRightContext[i]);
//            }
//            if (results.size() != 2) {
//               counterLeftRightContextNotTwo++;
//               System.out.println("[DBG] counterLeftRightContextNotTwo: " + results.size());
//            }
//            return new LeftRightContext(results.get(0), targetSeq, results.get(1));// searchedMethod;
//         }
//         break;
//      }
//      case BAD_METHODS: {
//         if (c1 && c2 && c3) {
//            // Check 1.
//            String[] leftRightContext = searchedMethod.split(UtilStr.escapeSpecialRegexChars(targetSeq));
//            if (leftRightContext.length != 2) {
//               counterLeftRightContextNotTwo_BAD_METHODS++;
//               System.out.println("[DBG] counterLeftRightContextNotTwo_BAD_METHODS: " + leftRightContext.length);
//            }
//            return new LeftRightContext(leftRightContext[0], targetSeq, leftRightContext[1]);// searchedMethod;
//         }
//         break;
//      }
//      default:
//      }
//      // -----------------------------------------------------------------------
//      return null;
//   }
//}

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
