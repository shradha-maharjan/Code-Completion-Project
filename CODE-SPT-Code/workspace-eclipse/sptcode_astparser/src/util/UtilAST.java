/**
 */
package util;

import java.io.IOException;
import java.util.Map;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

/**
 */
public class UtilAST {
   static final int INVALID_DOC = -1;
   static String fileContents = null;

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
}