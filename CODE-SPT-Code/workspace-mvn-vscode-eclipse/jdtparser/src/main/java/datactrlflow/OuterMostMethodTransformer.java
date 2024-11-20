package datactrlflow;

import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;

import util.UtilAST;
import visitor.OuterMostMethodDeclFinder;

public class OuterMostMethodTransformer {
   private static final String UNIT_NAME = "ParsedAndToString";

   String varNameInsert = "___pkivar___";
   String methodCallName = "___pkimethod___";
   String firstMethodParm = null;
   int[] firstParamPosition = new int[2]; // [0] for offset, [1] for length
   int counterOtherCases = 0, methodsWithParm = 0, methodsWithReturnStmt = 0;
   int methodsWithTryBlock = 0, methodsWithForStmt = 0, methodsWithIfStmt = 0;
   int methodsWithBody = 0;

   public String modifyOuterMostMethod(int index, String theLineOfFunction) {
      ASTParser parser = UtilAST.parseSrcCode(theLineOfFunction, UNIT_NAME + ".java");
      CompilationUnit cu = (CompilationUnit) parser.createAST(null);
      OuterMostMethodDeclFinder finder = new OuterMostMethodDeclFinder(UNIT_NAME);
      cu.accept(finder);

      int offsetBody = finder.getOffsetOfBody();

      if (offsetBody == -1 || finder.getOutermostMethod() == null) {
         throw new RuntimeException("[ERR] Data Error!!! " + theLineOfFunction);
      }

      this.firstMethodParm = finder.getFirstParmOuterMostMethod();
      this.firstParamPosition = finder.getFirstParamPosition();
      int[] returnStmtOffset = finder.getReturnStmtOffset();
      int tryBlockOffset = finder.getTryBlockOffset();
      int forStmtOffset = finder.getForStmtOffset();
      int ifStmtOffset = finder.getIfStmtOffset();
      int[] bgnEndIfOffsets = finder.getBgnEndIfOffsets();
      int closingCurlyBraceOfMethodBody = finder.getClosingCurlyBraceOfMethodBody();

      String varType = "int";
      String varName = String.format("%s", varNameInsert);
      String varDecl = String.format("%s %s;", varType, varName);
      String methodCall = String.format("%s%d(%s);", methodCallName, index, varName);
      String modifiedMethod = null;

      if (firstMethodParm != null) {
         String methodToBeInsert = String.format("%s%d(%s);", methodCallName, index, firstMethodParm);
         String frontPartOfMethod = theLineOfFunction.substring(0, offsetBody + 1);
         String backPartOfMethod = theLineOfFunction.substring(offsetBody + 1);
         modifiedMethod = frontPartOfMethod + methodToBeInsert + backPartOfMethod;
         // System.out.println("[DBG] " + updatedMethod);
         methodsWithParm++;
      }
      else if (returnStmtOffset != null) {
         // modifiedMethod = updateMethodBody(theLineOfFunction, offsetBody, returnStmtOffset, varDecl, methodCall);
         int bgn = returnStmtOffset[0];
         int end = returnStmtOffset[1];
         String methodBeforeTarget = theLineOfFunction.substring(0, bgn);
         String methodAfterTarget = theLineOfFunction.substring(end);
         String returnStmt = theLineOfFunction.substring(bgn, end);
         modifiedMethod = methodBeforeTarget + "{ " + methodCall + " " + returnStmt + " }" + methodAfterTarget;

         // variable definition
         String frontPartOfMethod = modifiedMethod.substring(0, offsetBody + 1);
         String backPartOfMethod = modifiedMethod.substring(offsetBody + 1);
         modifiedMethod = frontPartOfMethod + varDecl + backPartOfMethod;

         // System.out.println("[DBG] /*RT*/ " + modifiedMethod);
         methodsWithReturnStmt++;
      }
      else if (tryBlockOffset != -1) {
         modifiedMethod = updateMethodBody(theLineOfFunction, offsetBody, tryBlockOffset, varDecl, methodCall);
         methodsWithTryBlock++;
      }
      else if (forStmtOffset != -1) {
         modifiedMethod = updateMethodBody(theLineOfFunction, offsetBody, forStmtOffset, varDecl, methodCall);
         methodsWithForStmt++;
      }
      else if (ifStmtOffset != -1) {
         modifiedMethod = updateMethodBody(theLineOfFunction, offsetBody, ifStmtOffset, varDecl, methodCall);
         methodsWithIfStmt++;
      }
      else if (bgnEndIfOffsets != null) {
         int bgn = bgnEndIfOffsets[0];
         int end = bgnEndIfOffsets[1];
         String methodBeforeTarget = theLineOfFunction.substring(0, bgn);
         String methodAfterTarget = theLineOfFunction.substring(end);
         String stmtIf = theLineOfFunction.substring(bgn, end);
         modifiedMethod = methodBeforeTarget + "{ " + methodCall + " " + stmtIf + " }" + methodAfterTarget;

         // variable definition
         String frontPartOfMethod = modifiedMethod.substring(0, offsetBody + 1);
         String backPartOfMethod = modifiedMethod.substring(offsetBody + 1);
         modifiedMethod = frontPartOfMethod + varDecl + backPartOfMethod;
         methodsWithIfStmt++;
      }
      else if (closingCurlyBraceOfMethodBody != -1) {
         modifiedMethod = updateMethodBody(theLineOfFunction, offsetBody, closingCurlyBraceOfMethodBody, varDecl, methodCall);
         methodsWithBody++;
      }
      else {
         counterOtherCases++;
      }

      return modifiedMethod;
   }

   private String updateMethodBody(String theLineOfFunction, int offsetBody, //
         int offsetOfProgElem, String varDecl, String methodCall) {
      // method call
      String frontPartOfMethod = theLineOfFunction.substring(0, offsetOfProgElem + 1);
      String backPartOfMethod = theLineOfFunction.substring(offsetOfProgElem + 1);
      String updatedMethod = frontPartOfMethod + methodCall + backPartOfMethod;
      // variable definition
      frontPartOfMethod = updatedMethod.substring(0, offsetBody + 1);
      backPartOfMethod = updatedMethod.substring(offsetBody + 1);
      updatedMethod = frontPartOfMethod + varDecl + backPartOfMethod;
      return updatedMethod;
   }

   public String getFirstMethodParm() {
      return firstMethodParm;
   }

   public int[] getFirstParamPosition() {
      return firstParamPosition;
   }

   public int getCounterOtherCases() {
      return counterOtherCases;
   }

   public int getMethodsWithParm() {
      return methodsWithParm;
   }

   public int getMethodsWithReturnStmt() {
      return methodsWithReturnStmt;
   }

   public int getMethodsWithTryBlock() {
      return methodsWithTryBlock;
   }

   public int getMethodsWithForStmt() {
      return methodsWithForStmt;
   }

   public int getMethodsWithIfStmt() {
      return methodsWithIfStmt;
   }

   public int getMethodsWithBody() {
      return methodsWithBody;
   }

   public String getVarNameInsert() {
      return varNameInsert;
   }

   public String getMethodCallName() {
      return methodCallName;
   }
}
