package visitor;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.TypeDeclaration;

public class OuterMostMethodDeclFinder extends ASTVisitor {
   boolean log = true;
   int counter = 0, offsetOfBody = -1, tryBlockOffset = -1;
   int[] returnStmtOffset = null;
   int forStmtOffset = -1, ifStmtOffset = -1, closingCurlyBraceOfMethodBody = -1;
   MethodDeclaration outermostMethod = null;
   String unitName = "ParsedAndToString";
   String firstParmOuterMostMethod = null;
   int[] firstParamPosition = new int[2]; // [0] for offset, [1] for length
   int[] bgnEndIfOffsets = null;

   public OuterMostMethodDeclFinder() {
      this.counter = 0;
   }

   public OuterMostMethodDeclFinder(String unitName) {
      this.counter = 0;
      this.unitName = unitName;
   }

   @Override
   public boolean visit(MethodDeclaration node) {
      ASTNode parent = node.getParent();
      while (parent != null && !(parent instanceof TypeDeclaration)) {
         parent = parent.getParent();
      }

      if (parent instanceof TypeDeclaration) {
         TypeDeclaration typeDeclaration = (TypeDeclaration) parent;
         if (unitName.equals(typeDeclaration.getName().getIdentifier())) {
            if (outermostMethod == null) {
               // ** Outermost method
               outermostMethod = node; // Set the first found method
               this.counter++;
               if (node.getBody() == null)
                  throw new RuntimeException("[ERR] DATA ERROR !!! " + node);

               // ** Offset
               this.offsetOfBody = node.getBody().getStartPosition();

               // ** Offset of closing curly brace of the body
               Block body = node.getBody();
               if (body != null) {
                  int startOffset = body.getStartPosition();
                  int bodyLength = body.getLength();
                  int endOffset = startOffset + bodyLength - 2;
                  this.closingCurlyBraceOfMethodBody = endOffset;
               }

               // ** Parameter
               List<?> parameters = node.parameters();
               if (!parameters.isEmpty()) {
                  SingleVariableDeclaration firstParam = (SingleVariableDeclaration) parameters.get(0);
                  firstParmOuterMostMethod = firstParam.getName().getIdentifier();
                  firstParamPosition[0] = firstParam.getName().getStartPosition(); // Offset
                  firstParamPosition[1] = firstParam.getName().getLength(); // Length
               }

               // ** Return statement
               ReturnStatementOffsetFinder returnStmtOffsetFinder = new ReturnStatementOffsetFinder();
               node.accept(returnStmtOffsetFinder);
               List<int[]> returnBgnEndOffsets = returnStmtOffsetFinder.getReturnBgnEndOffsets();
               if (returnBgnEndOffsets.size() > 0) {
                  this.returnStmtOffset = returnBgnEndOffsets.get(0);
               }

               // ** Try statement
               TryCatchOffsetFinder tryCatchOffsetFinder = new TryCatchOffsetFinder();
               node.accept(tryCatchOffsetFinder);
               List<Integer> tryBlockOffsets = tryCatchOffsetFinder.getTryBlockOffsets();
               if (tryBlockOffsets.size() > 0) {
                  this.tryBlockOffset = tryBlockOffsets.get(0);
               }

               // ** For statement
               ForLoopBodyOffsetFinder forFinder = new ForLoopBodyOffsetFinder();
               node.accept(forFinder);
               List<Integer> offsetsForStmt = forFinder.getForLoopBodyOffsets();
               if (offsetsForStmt.size() > 0) {
                  this.forStmtOffset = offsetsForStmt.get(0);
               }

               // ** If statement
               IfStmtBodyOffsetFinder ifFinder = new IfStmtBodyOffsetFinder();
               node.accept(ifFinder);

               List<Integer> offsetsIfStmt = ifFinder.getThenBlockOffsets();
               List<int[]> bgnEndIfOffsets = ifFinder.getBgnEndIfOffsets();

               if (offsetsIfStmt.size() > 0) {
                  this.ifStmtOffset = offsetsIfStmt.get(0);
               }
               if (bgnEndIfOffsets.size() > 0) {
                  this.bgnEndIfOffsets = bgnEndIfOffsets.get(0);
               }
            }
         }
      }
      return true;
   }

   public MethodDeclaration getOutermostMethod() {
      return outermostMethod;
   }

   public int getOffsetOfBody() {
      return offsetOfBody;
   }

   public String getFirstParmOuterMostMethod() {
      return firstParmOuterMostMethod;
   }

   public int[] getFirstParamPosition() {
      return firstParamPosition;
   }

   public int[] getReturnStmtOffset() {
      return returnStmtOffset;
   }

   public int getTryBlockOffset() {
      return tryBlockOffset;
   }

   public int getForStmtOffset() {
      return forStmtOffset;
   }

   public int getIfStmtOffset() {
      return ifStmtOffset;
   }

   public int[] getBgnEndIfOffsets() {
      return bgnEndIfOffsets;
   }

   public int getClosingCurlyBraceOfMethodBody() {
      return closingCurlyBraceOfMethodBody;
   }

   public int getCounter() {
      return counter;
   }

   @Override
   public boolean visit(LambdaExpression node) {
      return false;
   }

   @Override
   public boolean visit(AnonymousClassDeclaration node) {
      return false;
   }

   private static class ReturnStatementOffsetFinder extends ASTVisitor {
      // private final List<Integer> returnOffsets = new ArrayList<>();
      private final List<int[]> returnBgnEndOffsets = new ArrayList<>();

      @Override
      public boolean visit(ReturnStatement node) {
         int startOffset = node.getStartPosition() - 1;
         int endOffset = startOffset + node.getLength() + 1;
         returnBgnEndOffsets.add(new int[] { startOffset, endOffset });

         // returnOffsets.add(node.getStartPosition() - 1);
         return super.visit(node); // Continue visiting children
      }

      public List<int[]> getReturnBgnEndOffsets() {
         return returnBgnEndOffsets;
      }
   }

   private static class TryCatchOffsetFinder extends ASTVisitor {
      private final List<Integer> tryBlockOffsets = new ArrayList<>();

      @Override
      public boolean visit(TryStatement node) {
         Block tryBlock = node.getBody();
         if (tryBlock != null) {
            tryBlockOffsets.add(tryBlock.getStartPosition());
         }
         return super.visit(node); // Continue visiting children
      }

      public List<Integer> getTryBlockOffsets() {
         return tryBlockOffsets;
      }
   }

   private static class ForLoopBodyOffsetFinder extends ASTVisitor {
      private final List<Integer> forLoopBodyOffsets = new ArrayList<>();

      @Override
      public boolean visit(EnhancedForStatement node) {
         // Retrieve the body of the for loop
         Statement forLoopBody = node.getBody();
         if (forLoopBody != null) {
            forLoopBodyOffsets.add(forLoopBody.getStartPosition());
         }
         return super.visit(node); // Continue visiting children
      }

      public List<Integer> getForLoopBodyOffsets() {
         return forLoopBodyOffsets;
      }
   }

   private static class IfStmtBodyOffsetFinder extends ASTVisitor {
      private final List<Integer> thenBlockOffsets = new ArrayList<>();
      private final List<int[]> bgnEndIfOffsets = new ArrayList<>();

      @Override
      public boolean visit(IfStatement node) {
         // Retrieve the body of the "then" block
         Statement thenStatement = node.getThenStatement();
         if (thenStatement != null) {
            if (thenStatement instanceof Block) {
               thenBlockOffsets.add(thenStatement.getStartPosition());
            }
            else {
               int startOffset = thenStatement.getStartPosition();
               int endOffset = startOffset + thenStatement.getLength();
               bgnEndIfOffsets.add(new int[] { startOffset, endOffset });
            }
         }
         return super.visit(node); // Continue visiting children
      }

      public List<Integer> getThenBlockOffsets() {
         return thenBlockOffsets;
      }

      public List<int[]> getBgnEndIfOffsets() { // Updated method name
         return bgnEndIfOffsets;
      }
   }
}
