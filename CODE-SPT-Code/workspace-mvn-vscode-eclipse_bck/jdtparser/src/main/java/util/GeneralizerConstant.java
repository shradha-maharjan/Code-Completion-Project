package util;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;

public class GeneralizerConstant {
   public static void main(String[] args) {
      GeneralizerConstant main = new GeneralizerConstant();
      main.generalizerConstant();
   }

   private int indexStr, indexChar;
   private int lenStr, lenChar;

   private void generalizerConstant() {
      String contents = "class A { \n" //
            + "void m1() {\n" //
            + "  m2(\"THIS IS A CONSTANT.\");\n" //
            + "  m3('C');\n" //
            + "} }";

      String unitName = "A.java";
      
      System.out.println("[DBG] ORG: ");
      System.out.println(contents);
      System.out.println("[DBG] ------------------------------------------------------");
      // Step 1
      getOffsetLetConstant(contents, unitName);
      String genStr = replace(contents, indexStr, lenStr, "\"___STR_CONST__\"");
      
      // Step 2
      getOffsetLetConstant(genStr, unitName);
      System.out.println("[DBG] ------------------------------------------------------");
      System.out.println("[DBG] GEN: ");
      System.out.println(replace(genStr, indexChar, lenChar, "''"));
   }

   private void getOffsetLetConstant(String contents, String unitName) {
      ASTParser parser = UtilAST.parseSrcCode(contents, unitName);

      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
      cu.accept(new ASTVisitor() {
         public boolean visit(MethodDeclaration node) {
            System.out.println("[DBG] " + node.getName());

            node.accept(new ASTVisitor() {

               public boolean visit(StringLiteral node) {
                  System.out.println("[DBG] \t" + node);
                  System.out.println("[DBG] \t" + node.getStartPosition() + ", " + node.getLength());

                  indexStr = node.getStartPosition();
                  lenStr = node.getLength();
                  return true;
               }

               public boolean visit(CharacterLiteral node) {
                  System.out.println("[DBG] \t" + node);
                  System.out.println("[DBG] \t" + node.getStartPosition() + ", " + node.getLength());
                  indexChar = node.getStartPosition();
                  lenChar = node.getLength();
                  return true;
               }
            });
            return true;
         }
      });
   }

   String replace(String s, int index, int leng, String repla) {
      String rep = s.substring(0, index) + repla + s.substring(index + leng);
      return rep;
   }
}
