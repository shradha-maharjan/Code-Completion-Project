package finetune;

import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.jdt.core.dom.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class AllASTVisitor extends ASTVisitor {
    private final FileWriter writer;
    private final int predOffset;
    private String highestPriorityNode = null;
    private int highestPriority = Integer.MAX_VALUE;

    private String singleNodeContent = null;
    private String singleNodeType = null;
    private int nodeVisitCount = 0;

    private static final Map<String, Integer> nodePriority = new HashMap<>();

    static {

        nodePriority.put("MethodInvocation", 1);
        nodePriority.put("MethodDeclaration", 1);
        nodePriority.put("FieldAccess", 2);
        nodePriority.put("VariableDeclarationFragment", 3);
        nodePriority.put("Assignment", 4);
        nodePriority.put("InfixExpression", 5);
        nodePriority.put("ConditionalExpression", 6);
        nodePriority.put("IfStatement", 7);
        nodePriority.put("ReturnStatement", 8);
        nodePriority.put("VariableDeclarationExpression", 9);
        nodePriority.put("AnnotationTypeDeclaration", 10);
        nodePriority.put("AnnotationTypeMemberDeclaration", 11);
        nodePriority.put("ClassInstanceCreation", 12);
        nodePriority.put("CatchClause", 13);
        nodePriority.put("SimpleName", 14);
        nodePriority.put("WhileStatement", 15);
        nodePriority.put("DoStatement", 15);
        nodePriority.put("ForStatement", 15);
        nodePriority.put("EnhancedForStatement", 15);
        nodePriority.put("SwitchStatement", 16);
        nodePriority.put("SwitchCase", 16);
        nodePriority.put("ArrayAccess", 17);
        nodePriority.put("ArrayCreation", 17);
        nodePriority.put("ArrayInitializer", 17);
        nodePriority.put("FieldDeclaration", 18);
        nodePriority.put("LambdaExpression", 19);
        nodePriority.put("ThisExpression", 20);
        nodePriority.put("ThrowStatement", 21);
        nodePriority.put("TryStatement", 21);
        nodePriority.put("Block", 22);
        nodePriority.put("BlockComment", 22);
        nodePriority.put("LineComment", 22);
        nodePriority.put("BooleanLiteral", 23);
        nodePriority.put("CharacterLiteral", 23);
        nodePriority.put("NumberLiteral", 23);
        nodePriority.put("TextBlock", 24);
        nodePriority.put("TextElement", 24);
        nodePriority.put("NormalAnnotation", 25);
        nodePriority.put("MarkerAnnotation", 25);
        nodePriority.put("TypeDeclaration", 26);
        nodePriority.put("TypeDeclarationStatement", 26);
        nodePriority.put("TypeLiteral", 27);
        nodePriority.put("TypeMethodReference", 27);
        nodePriority.put("ParenthesizedExpression", 28);
        nodePriority.put("QualifiedName", 29);
        nodePriority.put("PrimitiveType", 30);
        nodePriority.put("IntersectionType", 30);
        nodePriority.put("UnionType", 30);
        nodePriority.put("InstanceofExpression", 31);
        nodePriority.put("PatternInstanceofExpression", 31);
        nodePriority.put("PrefixExpression", 32);
        nodePriority.put("PostfixExpression", 32);
        nodePriority.put("GuardedPattern", 33);
        nodePriority.put("ImportDeclaration", 34);
        nodePriority.put("LabeledStatement", 35);
        nodePriority.put("ConstructorInvocation", 36);
        nodePriority.put("CreationReference", 36);
        nodePriority.put("MemberRef", 37);
        nodePriority.put("MemberValuePair", 38);
        nodePriority.put("ModuleDeclaration", 39);
        nodePriority.put("ModuleModifier", 40);
        nodePriority.put("NameQualifiedType", 41);
        nodePriority.put("NullLiteral", 42);
        nodePriority.put("NullPattern", 42);
        nodePriority.put("OpensDirective", 43);
        nodePriority.put("ProvidesDirective", 44);
        nodePriority.put("RequiresDirective", 45);
        nodePriority.put("TagElement", 46);
        nodePriority.put("TagProperty", 46);
        nodePriority.put("TextElement", 47);
        nodePriority.put("EnumDeclaration", 48);
        nodePriority.put("EnumConstantDeclaration", 48);
        nodePriority.put("Dimension", 49);
        nodePriority.put("EmptyStatement", 50);
        nodePriority.put("SuperConstructorInvocation", 51);
        nodePriority.put("SuperFieldAccess", 51);
        nodePriority.put("SuperMethodInvocation", 51);
        nodePriority.put("SuperMethodReference", 52);
        nodePriority.put("UsesDirective", 53);
        nodePriority.put("YieldStatement", 54);
        nodePriority.put("ExpressionMethodReference", 55);
        nodePriority.put("ExpressionStatement", 56);
        nodePriority.put("Initializer", 57);
        nodePriority.put("JavaDocTextElement", 58);
        nodePriority.put("Annotation", 59);
    }

    // private void log(String message) {
    //     try {
    //         writer.write(message + "\n");
    //         writer.flush();
    //     } catch (IOException e) {
    //         e.printStackTrace();
    //     }
    // }

    public AllASTVisitor(FileWriter writer, int predOffset) {
        this.writer = writer;
        this.predOffset = predOffset;
    }

    private void resetSingleNodeTracking() {
        singleNodeContent = null;
        singleNodeType = null;
        nodeVisitCount = 0;
    }

    private void trackSingleNode(String nodeType, String content) {
        nodeVisitCount++;
        singleNodeType = nodeType;
        singleNodeContent = content;
    }

    private boolean isMatchingPred(ASTNode node) {
        return predOffset == node.getStartPosition();
    }

    private void updateHighestPriorityNode(String nodeType, String content) {
        Integer priority = nodePriority.get(nodeType);
        if (priority == null) {
            System.out.println("[DEBUG] Priority for NodeType: " + nodeType + " is null. Skipping.");
            return;
        }
        if (highestPriorityNode == null || priority < highestPriority) {
            highestPriority = priority;
            highestPriorityNode = "[DBG] " + nodeType + ": " + content;
            System.out.println("[DEBUG] Updated Highest Priority Node: " + highestPriorityNode);
        } else {
            System.out.println("[DEBUG] Skipping Node: " + nodeType + " (Priority: " + priority + ")");
        }
    }

    public void logHighestPriorityNode() throws IOException {
        if (nodeVisitCount == 1) {
            // If only one node is visited, log and write it directly.
            writer.write("[DBG] " + singleNodeType + ": " + singleNodeContent + "\n");
            System.out.println("[DBG] Single Node Logged: " + singleNodeType + ": " + singleNodeContent);
        } else if (highestPriorityNode != null) {
            // Fall back to priority-based logging if multiple nodes are visited.
            writer.write(highestPriorityNode + "\n");
            System.out.println("[DBG] Highest Priority Node Logged: " + highestPriorityNode);
        } else {
            writer.write("[DBG] No nodes visited for this PRED.\n");
            System.out.println("[DBG] No nodes visited for this PRED.");
        }
        writer.write("[DBG] ------------------------------------------------------\n");
        resetSingleNodeTracking();
        resetPriorityTracking();
    }

    private void resetPriorityTracking() {
        highestPriorityNode = null;
        highestPriority = Integer.MAX_VALUE;
    }
    
    
//     private List<String> nodeTypes = new ArrayList<>();
//     //private List<int[]> loopBounds; 

//     public AllASTVisitor(FileWriter writer, int predOffset){//}, List<int[]> loopBounds) {
//         this.writer = writer;
//         this.predOffset = predOffset;
//         //this.loopBounds = loopBounds;
//     }

//     private void log(String message) {
//         try {
//             writer.write(message + "\n");
//             writer.flush(); 
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//     private boolean isMatchingPred(ASTNode node) {
//         return predOffset == node.getStartPosition();
//     }

//     public void writeSortedNodeTypes() throws IOException {
//       Collections.sort(nodeTypes);  
//       for (String nodeType : nodeTypes) {
//           writer.write("\t=> " + nodeType + "\n");
//           System.out.println("\t=> " + nodeType);
//       }

//       nodeTypes.clear();  
//    }

//     private boolean isWithinLoopBounds(ASTNode node) {
//         int start = node.getStartPosition();
//         for (int[] bounds : loopBounds) {
//             if (start >= bounds[0] && start <= bounds[1]) {
//                 return true;
//             }
//         }
//         return false;
// }
    @Override
    public boolean visit(MethodDeclaration node) {
        SimpleName methodName = node.getName();
        if (isMatchingPred(methodName)) {
            trackSingleNode("MethodDeclaration" , methodName.toString());
            updateHighestPriorityNode("MethodDeclaration" , methodName.toString());
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        if (isMatchingPred(node)) {
            trackSingleNode("VariableDeclarationFragment" , node.getName().toString());
            updateHighestPriorityNode("VariableDeclarationFragment" , node.getName().toString());
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationExpression node) {
        if (isMatchingPred(node)) {
            trackSingleNode("VariableDeclarationExpression" , node.toString());
            updateHighestPriorityNode("VariableDeclarationExpression" , node.toString());
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodInvocation node) {
        System.out.println("[DEBUG] Visiting MethodInvocation: " + node.getName());
        if (isMatchingPred(node)) {//&& isWithinLoopBounds(node)) {
            trackSingleNode("MethodInvocation" , node.getName().toString());
            updateHighestPriorityNode("MethodInvocation" , node.getName().toString());
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(FieldAccess node) {
        if (isMatchingPred(node)) {
            String fieldAccessContent = node.getName().toString();
            trackSingleNode("FieldAccess", fieldAccessContent);
            updateHighestPriorityNode("FieldAccess", fieldAccessContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(IfStatement node) {
        if (isMatchingPred(node)) {
            String ifStatementContent = node.toString();
            trackSingleNode("IfStatement", ifStatementContent);
            updateHighestPriorityNode("IfStatement", ifStatementContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(ReturnStatement node) {
        if (isMatchingPred(node)) {
            String returnStatementContent = node.toString();
            trackSingleNode("ReturnStatement", returnStatementContent);
            updateHighestPriorityNode("ReturnStatement", returnStatementContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(SimpleName node) {
        if (isMatchingPred(node)) {
            String simpleNameContent = node.toString();
            trackSingleNode("SimpleName", simpleNameContent);
            updateHighestPriorityNode("SimpleName", simpleNameContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(ThisExpression node) {
        if (isMatchingPred(node)) {
            String thisExpressionContent = node.toString();
            trackSingleNode("ThisExpression", thisExpressionContent);
            updateHighestPriorityNode("ThisExpression", thisExpressionContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(AnnotationTypeDeclaration node) {
        if (isMatchingPred(node)) {
            String annotationTypeDeclarationContent = node.getName().toString();
            trackSingleNode("AnnotationTypeDeclaration", annotationTypeDeclarationContent);
            updateHighestPriorityNode("AnnotationTypeDeclaration", annotationTypeDeclarationContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(AnnotationTypeMemberDeclaration node) {
        if (isMatchingPred(node)) {
            String annotationTypeMemberDeclarationContent = node.getName().toString();
            trackSingleNode("AnnotationTypeMemberDeclaration", annotationTypeMemberDeclarationContent);
            updateHighestPriorityNode("AnnotationTypeMemberDeclaration", annotationTypeMemberDeclarationContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(AnonymousClassDeclaration node) {
        if (isMatchingPred(node)) {
            String anonymousClassDeclarationContent = node.toString();
            trackSingleNode("AnonymousClassDeclaration", anonymousClassDeclarationContent);
            updateHighestPriorityNode("AnonymousClassDeclaration", anonymousClassDeclarationContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(ArrayAccess node) {
        if (isMatchingPred(node)) {
            String arrayAccessContent = node.toString();
            trackSingleNode("ArrayAccess", arrayAccessContent);
            updateHighestPriorityNode("ArrayAccess", arrayAccessContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(ArrayCreation node) {
        if (isMatchingPred(node)) {
            String arrayCreationContent = node.toString();
            trackSingleNode("ArrayCreation", arrayCreationContent);
            updateHighestPriorityNode("ArrayCreation", arrayCreationContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(ArrayInitializer node) {
        if (isMatchingPred(node)) {
            String arrayInitializerContent = node.toString();
            trackSingleNode("ArrayInitializer", arrayInitializerContent);
            updateHighestPriorityNode("ArrayInitializer", arrayInitializerContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(ArrayType node) {
        if (isMatchingPred(node)) {
            String arrayTypeContent = node.toString();
            trackSingleNode("ArrayType", arrayTypeContent);
            updateHighestPriorityNode("ArrayType", arrayTypeContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(AssertStatement node) {
        if (isMatchingPred(node)) {
            String assertStatementContent = node.toString();
            trackSingleNode("AssertStatement", assertStatementContent);
            updateHighestPriorityNode("AssertStatement", assertStatementContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(Assignment node) {
        if (isMatchingPred(node)) {
            String assignmentContent = node.getLeftHandSide().toString() + " = " + node.getRightHandSide().toString();
            trackSingleNode("Assignment", assignmentContent);
            updateHighestPriorityNode("Assignment", assignmentContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(Block node) {
        if (isMatchingPred(node)) {
            String blockContent = node.toString();
            trackSingleNode("Block", blockContent);
            updateHighestPriorityNode("Block", blockContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(BlockComment node) {
        if (isMatchingPred(node)) {
            String blockCommentContent = node.toString();
            trackSingleNode("BlockComment", blockCommentContent);
            updateHighestPriorityNode("BlockComment", blockCommentContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(BooleanLiteral node) {
        if (isMatchingPred(node)) {
            String booleanLiteralContent = node.toString();
            trackSingleNode("BooleanLiteral", booleanLiteralContent);
            updateHighestPriorityNode("BooleanLiteral", booleanLiteralContent);
        }
        return super.visit(node);
    }
    
    @Override
    public boolean visit(BreakStatement node) {
        if (isMatchingPred(node)) {
            String breakStatementContent = node.toString();
            trackSingleNode("BreakStatement", breakStatementContent);
            updateHighestPriorityNode("BreakStatement", breakStatementContent);
        }
        return super.visit(node);
    }    

    @Override
    public boolean visit(CastExpression node) {
        if (isMatchingPred(node)) {
            String castExpressionContent = "(" + node.getType().toString() + ") " + node.getExpression().toString();
            trackSingleNode("CastExpression", castExpressionContent);
            updateHighestPriorityNode("CastExpression", castExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(CatchClause node) {
        if (isMatchingPred(node)) {
            String catchClauseContent = node.getException().toString();
            trackSingleNode("CatchClause", catchClauseContent);
            updateHighestPriorityNode("CatchClause", catchClauseContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(CharacterLiteral node) {
        if (isMatchingPred(node)) {
            String characterLiteralContent = String.valueOf(node.charValue());
            trackSingleNode("CharacterLiteral", characterLiteralContent);
            updateHighestPriorityNode("CharacterLiteral", characterLiteralContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ClassInstanceCreation node) {
        if (isMatchingPred(node)) {
            String classInstanceContent = node.toString();
            trackSingleNode("ClassInstanceCreation", classInstanceContent);
            updateHighestPriorityNode("ClassInstanceCreation", classInstanceContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ConditionalExpression node) {
        if (isMatchingPred(node)) {
            String conditionalExpressionContent = node.getExpression().toString();
            trackSingleNode("ConditionalExpression", conditionalExpressionContent);
            updateHighestPriorityNode("ConditionalExpression", conditionalExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        if (isMatchingPred(node)) {
            String constructorInvocationContent = node.toString();
            trackSingleNode("ConstructorInvocation", constructorInvocationContent);
            updateHighestPriorityNode("ConstructorInvocation", constructorInvocationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ContinueStatement node) {
        if (isMatchingPred(node)) {
            String continueStatementContent = node.toString();
            trackSingleNode("ContinueStatement", continueStatementContent);
            updateHighestPriorityNode("ContinueStatement", continueStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(CreationReference node) {
        if (isMatchingPred(node)) {
            String creationReferenceContent = node.toString();
            trackSingleNode("CreationReference", creationReferenceContent);
            updateHighestPriorityNode("CreationReference", creationReferenceContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(Dimension node) {
        if (isMatchingPred(node)) {
            String dimensionContent = node.toString();
            trackSingleNode("Dimension", dimensionContent);
            updateHighestPriorityNode("Dimension", dimensionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(DoStatement node) {
        if (isMatchingPred(node)) {
            String doStatementContent = node.toString();
            trackSingleNode("DoStatement", doStatementContent);
            updateHighestPriorityNode("DoStatement", doStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(EmptyStatement node) {
        if (isMatchingPred(node)) {
            String emptyStatementContent = node.toString();
            trackSingleNode("EmptyStatement", emptyStatementContent);
            updateHighestPriorityNode("EmptyStatement", emptyStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        if (isMatchingPred(node)) {
            String enhancedForStatementContent = node.toString();
            trackSingleNode("EnhancedForStatement", enhancedForStatementContent);
            updateHighestPriorityNode("EnhancedForStatement", enhancedForStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(EnumConstantDeclaration node) {
        if (isMatchingPred(node)) {
            String enumConstantDeclarationContent = node.toString();
            trackSingleNode("EnumConstantDeclaration", enumConstantDeclarationContent);
            updateHighestPriorityNode("EnumConstantDeclaration", enumConstantDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(EnumDeclaration node) {
        if (isMatchingPred(node)) {
            String enumDeclarationContent = node.getName().toString();
            trackSingleNode("EnumDeclaration", enumDeclarationContent);
            updateHighestPriorityNode("EnumDeclaration", enumDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ExpressionMethodReference node) {
        if (isMatchingPred(node)) {
            String expressionMethodReferenceContent = node.toString();
            trackSingleNode("ExpressionMethodReference", expressionMethodReferenceContent);
            updateHighestPriorityNode("ExpressionMethodReference", expressionMethodReferenceContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        if (isMatchingPred(node)) {
            String expressionStatementContent = node.toString();
            trackSingleNode("ExpressionStatement", expressionStatementContent);
            updateHighestPriorityNode("ExpressionStatement", expressionStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(FieldDeclaration node) {
        if (isMatchingPred(node)) {
            String fieldDeclarationContent = node.toString();
            trackSingleNode("FieldDeclaration", fieldDeclarationContent);
            updateHighestPriorityNode("FieldDeclaration", fieldDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ForStatement node) {
        if (isMatchingPred(node)) {
            String forStatementContent = node.toString();
            trackSingleNode("ForStatement", forStatementContent);
            updateHighestPriorityNode("ForStatement", forStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(GuardedPattern node) {
        if (isMatchingPred(node)) {
            String guardedPatternContent = node.toString();
            trackSingleNode("GuardedPattern", guardedPatternContent);
            updateHighestPriorityNode("GuardedPattern", guardedPatternContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ImportDeclaration node) {
        if (isMatchingPred(node)) {
            String importDeclarationContent = node.toString();
            trackSingleNode("ImportDeclaration", importDeclarationContent);
            updateHighestPriorityNode("ImportDeclaration", importDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(InfixExpression node) {
        if (isMatchingPred(node)) {
            String infixExpressionContent = node.toString();
            trackSingleNode("InfixExpression", infixExpressionContent);
            updateHighestPriorityNode("InfixExpression", infixExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(Initializer node) {
        if (isMatchingPred(node)) {
            String initializerContent = node.toString();
            trackSingleNode("Initializer", initializerContent);
            updateHighestPriorityNode("Initializer", initializerContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(InstanceofExpression node) {
        if (isMatchingPred(node)) {
            String instanceofExpressionContent = node.toString();
            trackSingleNode("InstanceofExpression", instanceofExpressionContent);
            updateHighestPriorityNode("InstanceofExpression", instanceofExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(IntersectionType node) {
        if (isMatchingPred(node)) {
            String intersectionTypeContent = node.toString();
            trackSingleNode("IntersectionType", intersectionTypeContent);
            updateHighestPriorityNode("IntersectionType", intersectionTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(JavaDocTextElement node) {
        if (isMatchingPred(node)) {
            String javaDocTextElementContent = node.toString();
            trackSingleNode("JavaDocTextElement", javaDocTextElementContent);
            updateHighestPriorityNode("JavaDocTextElement", javaDocTextElementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(LabeledStatement node) {
        if (isMatchingPred(node)) {
            String labeledStatementContent = node.toString();
            trackSingleNode("LabeledStatement", labeledStatementContent);
            updateHighestPriorityNode("LabeledStatement", labeledStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(LambdaExpression node) {
        if (isMatchingPred(node)) {
            String lambdaExpressionContent = node.toString();
            trackSingleNode("LambdaExpression", lambdaExpressionContent);
            updateHighestPriorityNode("LambdaExpression", lambdaExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(LineComment node) {
        if (isMatchingPred(node)) {
            String lineCommentContent = node.toString();
            trackSingleNode("LineComment", lineCommentContent);
            updateHighestPriorityNode("LineComment", lineCommentContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MarkerAnnotation node) {
        if (isMatchingPred(node)) {
            String markerAnnotationContent = node.toString();
            trackSingleNode("MarkerAnnotation", markerAnnotationContent);
            updateHighestPriorityNode("MarkerAnnotation", markerAnnotationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MemberRef node) {
        if (isMatchingPred(node)) {
            String memberRefContent = node.toString();
            trackSingleNode("MemberRef", memberRefContent);
            updateHighestPriorityNode("MemberRef", memberRefContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MemberValuePair node) {
        if (isMatchingPred(node)) {
            String memberValuePairContent = node.toString();
            trackSingleNode("MemberValuePair", memberValuePairContent);
            updateHighestPriorityNode("MemberValuePair", memberValuePairContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodRef node) {
        if (isMatchingPred(node)) {
            String methodRefContent = node.toString();
            trackSingleNode("MethodRef", methodRefContent);
            updateHighestPriorityNode("MethodRef", methodRefContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(MethodRefParameter node) {
        if (isMatchingPred(node)) {
            String methodRefParameterContent = node.toString();
            trackSingleNode("MethodRefParameter", methodRefParameterContent);
            updateHighestPriorityNode("MethodRefParameter", methodRefParameterContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(Modifier node) {
        if (isMatchingPred(node)) {
            String modifierContent = node.toString();
            trackSingleNode("Modifier", modifierContent);
            updateHighestPriorityNode("Modifier", modifierContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ModuleDeclaration node) {
        if (isMatchingPred(node)) {
            String moduleDeclarationContent = node.toString();
            trackSingleNode("ModuleDeclaration", moduleDeclarationContent);
            updateHighestPriorityNode("ModuleDeclaration", moduleDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ModuleModifier node) {
        if (isMatchingPred(node)) {
            String moduleModifierContent = node.toString();
            trackSingleNode("ModuleModifier", moduleModifierContent);
            updateHighestPriorityNode("ModuleModifier", moduleModifierContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(NameQualifiedType node) {
        if (isMatchingPred(node)) {
            String nameQualifiedTypeContent = node.toString();
            trackSingleNode("NameQualifiedType", nameQualifiedTypeContent);
            updateHighestPriorityNode("NameQualifiedType", nameQualifiedTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(NormalAnnotation node) {
        if (isMatchingPred(node)) {
            String normalAnnotationContent = node.toString();
            trackSingleNode("NormalAnnotation", normalAnnotationContent);
            updateHighestPriorityNode("NormalAnnotation", normalAnnotationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(NullLiteral node) {
        if (isMatchingPred(node)) {
            String nullLiteralContent = node.toString();
            trackSingleNode("NullLiteral", nullLiteralContent);
            updateHighestPriorityNode("NullLiteral", nullLiteralContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(NullPattern node) {
        if (isMatchingPred(node)) {
            String nullPatternContent = node.toString();
            trackSingleNode("NullLiteral", nullPatternContent);
            updateHighestPriorityNode("[DBG] NullPattern: ", nullPatternContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(NumberLiteral node) {
        if (isMatchingPred(node)) {
            String numberLiteralContent = node.toString();
            trackSingleNode("NumberLiteral", numberLiteralContent);
            updateHighestPriorityNode("NumberLiteral", numberLiteralContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ParameterizedType node) {
        if (isMatchingPred(node)) {
            String parameterizedTypeContent = node.toString();
            trackSingleNode("ParameterizedType", parameterizedTypeContent);
            updateHighestPriorityNode("ParameterizedType", parameterizedTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ParenthesizedExpression node) {
        if (isMatchingPred(node)) {
            String parenthesizedExpressionContent = node.toString();
            trackSingleNode("ParenthesizedExpression", parenthesizedExpressionContent);
            updateHighestPriorityNode("ParenthesizedExpression", parenthesizedExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PostfixExpression node) {
        if (isMatchingPred(node)) {
            String postfixExpressionContent = node.toString();
            trackSingleNode("PostfixExpression", postfixExpressionContent);
            updateHighestPriorityNode("PostfixExpression", postfixExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PrefixExpression node) {
        if (isMatchingPred(node)) {
            String prefixExpressionContent = node.toString();
            trackSingleNode("PrefixExpression", prefixExpressionContent);
            updateHighestPriorityNode("PrefixExpression", prefixExpressionContent);
        }
        return super.visit(node);
    }
    @Override
    public boolean visit(OpensDirective node) {
        if (isMatchingPred(node)) {
            String opensDirectiveContent = node.toString();
            trackSingleNode("OpensDirective", opensDirectiveContent);
            updateHighestPriorityNode("OpensDirective", opensDirectiveContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PackageDeclaration node) {
        if (isMatchingPred(node)) {
            String packageDeclarationContent = node.toString();
            trackSingleNode("PackageDeclaration", packageDeclarationContent);
            updateHighestPriorityNode("PackageDeclaration", packageDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PatternInstanceofExpression node) {
        if (isMatchingPred(node)) {
            String patternInstanceofExpressionContent = node.toString();
            trackSingleNode("PatternInstanceofExpression", patternInstanceofExpressionContent);
            updateHighestPriorityNode("PatternInstanceofExpression", patternInstanceofExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ProvidesDirective node) {
        if (isMatchingPred(node)) {
            String providesDirectiveContent = node.toString();
            trackSingleNode("ProvidesDirective", providesDirectiveContent);
            updateHighestPriorityNode("ProvidesDirective", providesDirectiveContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(PrimitiveType node) {
        if (isMatchingPred(node)) {
            String primitiveTypeContent = node.toString();
            trackSingleNode("PrimitiveType", primitiveTypeContent);
            updateHighestPriorityNode("PrimitiveType", primitiveTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedName node) {
        if (isMatchingPred(node)) {
            String qualifiedNameContent = node.toString();
            trackSingleNode("QualifiedName", qualifiedNameContent);
            updateHighestPriorityNode("QualifiedName", qualifiedNameContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(QualifiedType node) {
        if (isMatchingPred(node)) {
            String qualifiedTypeContent = node.toString();
            trackSingleNode("QualifiedType", qualifiedTypeContent);
            updateHighestPriorityNode("QualifiedType", qualifiedTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ModuleQualifiedName node) {
        if (isMatchingPred(node)) {
            String moduleQualifiedNameContent = node.toString();
            trackSingleNode("ModuleQualifiedName", moduleQualifiedNameContent);
            updateHighestPriorityNode("ModuleQualifiedName", moduleQualifiedNameContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(RequiresDirective node) {
        if (isMatchingPred(node)) {
            String requiresDirectiveContent = node.toString();
            trackSingleNode("RequiresDirective", requiresDirectiveContent);
            updateHighestPriorityNode("RequiresDirective", requiresDirectiveContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(RecordPattern node) {
        if (isMatchingPred(node)) {
            String recordPatternContent = node.toString();
            trackSingleNode("RecordPattern", recordPatternContent);
            updateHighestPriorityNode("RecordPattern", recordPatternContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SimpleType node) {
        if (isMatchingPred(node)) {
            String simpleTypeContent = node.toString();
            trackSingleNode("SimpleType", simpleTypeContent);
            updateHighestPriorityNode("SimpleType", simpleTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SingleMemberAnnotation node) {
        if (isMatchingPred(node)) {
            String singleMemberAnnotationContent = node.toString();
            trackSingleNode("SingleMemberAnnotation", singleMemberAnnotationContent);
            updateHighestPriorityNode("SingleMemberAnnotation", singleMemberAnnotationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        if (isMatchingPred(node)) {
            String singleVariableDeclarationContent = node.toString();
            trackSingleNode("SingleVariableDeclaration", singleVariableDeclarationContent);
            updateHighestPriorityNode("SingleVariableDeclaration", singleVariableDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(StringLiteral node) {
        if (isMatchingPred(node)) {
            String stringLiteralContent = node.getLiteralValue();
            trackSingleNode("StringLiteral", stringLiteralContent);
            updateHighestPriorityNode("StringLiteral", stringLiteralContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        if (isMatchingPred(node)) {
            String superConstructorInvocationContent = node.toString();
            trackSingleNode("SuperConstructorInvocation", superConstructorInvocationContent);
            updateHighestPriorityNode("SuperConstructorInvocation", superConstructorInvocationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SuperFieldAccess node) {
        if (isMatchingPred(node)) {
            String superFieldAccessContent = node.toString();
            trackSingleNode("SuperFieldAccess", superFieldAccessContent);
            updateHighestPriorityNode("SuperFieldAccess", superFieldAccessContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SuperMethodInvocation node) {
        if (isMatchingPred(node)) {
            String superMethodInvocationContent = node.toString();
            trackSingleNode("SuperMethodInvocation", superMethodInvocationContent);
            updateHighestPriorityNode("SuperMethodInvocation", superMethodInvocationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SuperMethodReference node) {
        if (isMatchingPred(node)) {
            String superMethodReferenceContent = node.toString();
            trackSingleNode("SuperMethodReference", superMethodReferenceContent);
            updateHighestPriorityNode("SuperMethodReference", superMethodReferenceContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SwitchCase node) {
        if (isMatchingPred(node)) {
            String switchCaseContent = node.toString();
            trackSingleNode("SwitchCase", switchCaseContent);
            updateHighestPriorityNode("SwitchCase", switchCaseContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SwitchExpression node) {
        if (isMatchingPred(node)) {
            String switchExpressionContent = node.toString();
            trackSingleNode("SwitchExpression", switchExpressionContent);
            updateHighestPriorityNode("SwitchExpression", switchExpressionContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SwitchStatement node) {
        if (isMatchingPred(node)) {
            String switchStatementContent = node.toString();
            trackSingleNode("SwitchStatement", switchStatementContent);
            updateHighestPriorityNode("SwitchStatement", switchStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(SynchronizedStatement node) {
        if (isMatchingPred(node)) {
            String synchronizedStatementContent = node.toString();
            trackSingleNode("SynchronizedStatement", synchronizedStatementContent);
            updateHighestPriorityNode("SynchronizedStatement", synchronizedStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TagElement node) {
        if (isMatchingPred(node)) {
            String tagElementContent = node.toString();
            trackSingleNode("TagElement", tagElementContent);
            updateHighestPriorityNode("TagElement", tagElementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TagProperty node) {
        if (isMatchingPred(node)) {
            String tagPropertyContent = node.toString();
            trackSingleNode("TagProperty", tagPropertyContent);
            updateHighestPriorityNode("TagProperty", tagPropertyContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TextBlock node) {
        if (isMatchingPred(node)) {
            String textBlockContent = node.toString();
            trackSingleNode("TextBlock", textBlockContent);
            updateHighestPriorityNode("TextBlock", textBlockContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TextElement node) {
        if (isMatchingPred(node)) {
            String textElementContent = node.toString();
            trackSingleNode("TextElement", textElementContent);
            updateHighestPriorityNode("TextElement", textElementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(ThrowStatement node) {
        if (isMatchingPred(node)) {
            String throwStatementContent = node.toString();
            trackSingleNode("ThrowStatement", throwStatementContent);
            updateHighestPriorityNode("ThrowStatement", throwStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TryStatement node) {
        if (isMatchingPred(node)) {
            String tryStatementContent = node.toString();
            trackSingleNode("TryStatement", tryStatementContent);
            updateHighestPriorityNode("TryStatement", tryStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        if (isMatchingPred(node)) {
            String typeDeclarationContent = node.toString();
            trackSingleNode("TypeDeclaration", typeDeclarationContent);
            updateHighestPriorityNode("TypeDeclaration", typeDeclarationContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        if (isMatchingPred(node)) {
            String typeDeclarationStatementContent = node.toString();
            trackSingleNode("TypeDeclarationStatement", typeDeclarationStatementContent);
            updateHighestPriorityNode("TypeDeclarationStatement", typeDeclarationStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeLiteral node) {
        if (isMatchingPred(node)) {
            String typeLiteralContent = node.toString();
            trackSingleNode("TypeLiteral", typeLiteralContent);
            updateHighestPriorityNode("TypeLiteral", typeLiteralContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeMethodReference node) {
        if (isMatchingPred(node)) {
            String typeMethodReferenceContent = node.toString();
            trackSingleNode("TypeMethodReference", typeMethodReferenceContent);
            updateHighestPriorityNode("TypeMethodReference", typeMethodReferenceContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TypeParameter node) {
        if (isMatchingPred(node)) {
            String typeParameterContent = node.toString();
            trackSingleNode("TypeParameter", typeParameterContent);
            updateHighestPriorityNode("TypeParameter", typeParameterContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(TypePattern node) {
        if (isMatchingPred(node)) {
            String typePatternContent = node.toString();
            trackSingleNode("TypePattern", typePatternContent);
            updateHighestPriorityNode("TypePattern", typePatternContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(UnionType node) {
        if (isMatchingPred(node)) {
            String unionTypeContent = node.toString();
            trackSingleNode("UnionType", unionTypeContent);
            updateHighestPriorityNode("UnionType", unionTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(UsesDirective node) {
        if (isMatchingPred(node)) {
            String usesDirectiveContent = node.toString();
            trackSingleNode("UsesDirective", usesDirectiveContent);
            updateHighestPriorityNode("UsesDirective", usesDirectiveContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        if (isMatchingPred(node)) {
            String variableDeclarationStatementContent = node.toString();
            trackSingleNode("VariableDeclarationStatement", variableDeclarationStatementContent);
            updateHighestPriorityNode("VariableDeclarationStatement", variableDeclarationStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(WhileStatement node) {
        if (isMatchingPred(node)) {
            String whileStatementContent = node.toString();
            trackSingleNode("WhileStatement", whileStatementContent);
            updateHighestPriorityNode("WhileStatement", whileStatementContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(WildcardType node) {
        if (isMatchingPred(node)) {
            String wildcardTypeContent = node.toString();
            trackSingleNode("WildcardType", wildcardTypeContent);
            updateHighestPriorityNode("WildcardType", wildcardTypeContent);
        }
        return super.visit(node);
    }

    @Override
    public boolean visit(YieldStatement node) {
        if (isMatchingPred(node)) {
            String yieldStatementContent = node.toString();
            trackSingleNode("YieldStatement", yieldStatementContent);
            updateHighestPriorityNode("YieldStatement", yieldStatementContent);
        }
        return super.visit(node);
    }


}


// import java.io.FileWriter;
// import java.io.IOException;
// import org.eclipse.jdt.core.dom.*;

// public class AllASTVisitor extends ASTVisitor {
//     private FileWriter writer;

//     // Constructor to accept a FileWriter for output
//     public AllASTVisitor(FileWriter writer) {
//         this.writer = writer;
//     }
    
//     private void updateHighestPriorityNode(String message) {
// //        // Write to console
// //        try {
// //            System.out.println(message);
// //        } catch (Exception e) {
// //            e.printStackTrace();
// //        }

//         // Write to the output file
//         try {
//             writer.write(message + "\n");
//             writer.flush(); // Ensure the message is written immediately
//         } catch (IOException e) {
//             e.printStackTrace();
//         }
//     }

//    public boolean visit(AnnotationTypeDeclaration node) {
//       log("\t=> AnnotationTypeDeclaration");
//       return true;
//    }

//    public boolean visit(AnnotationTypeMemberDeclaration node) {
//       log("\t=> AnnotationTypeMemberDeclaration");
//       return true;
//    }

//    public boolean visit(AnonymousClassDeclaration node) {
//       log("\t=> AnonymousClassDeclaration");
//       return true;
//    }

//    public boolean visit(ArrayAccess node) {
//       log("\t=> ArrayAccess");
//       return true;
//    }

//    public boolean visit(ArrayCreation node) {
//       log("\t=> ArrayCreation");
//       return true;
//    }

//    public boolean visit(ArrayInitializer node) {
//       log("\t=> ArrayInitializer");
//       return true;
//    }

//    public boolean visit(ArrayType node) {
//       log("\t=> ArrayType");
//       return true;
//    }

//    public boolean visit(AssertStatement node) {
//       log("\t=> AssertStatement");
//       return true;
//    }

//    public boolean visit(Assignment node) {
//       log("\t=> Assignment");
//       return true;
//    }

//    public boolean visit(Block node) {
//       log("\t=> Block");
//       return true;
//    }

//    public boolean visit(BlockComment node) {
//       log("\t=> BlockComment");
//       return true;
//    }

//    public boolean visit(BooleanLiteral node) {
//       log("\t=> BooleanLiteral");
//       return true;
//    }

//    public boolean visit(BreakStatement node) {
//       log("\t=> BreakStatement");
//       return true;
//    }

//    public boolean visit(CaseDefaultExpression node) {
//       log("\t=> CaseDefaultExpression");
//       return true;
//    }

//    public boolean visit(CastExpression node) {
//       log("\t=> CastExpression");
//       return true;
//    }

//    public boolean visit(CatchClause node) {
//       log("\t=> CatchClause");
//       return true;
//    }

//    public boolean visit(CharacterLiteral node) {
//       log("\t=> CharacterLiteral");
//       return true;
//    }

//    public boolean visit(ClassInstanceCreation node) {
//       log("\t=> ClassInstanceCreation");
//       return true;
//    }

//    public boolean visit(CompilationUnit node) {
//       log("\t=> CompilationUnit");
//       return true;
//    }

//    public boolean visit(ConditionalExpression node) {
//       log("\t=> ConditionalExpression");
//       return true;
//    }

//    public boolean visit(ConstructorInvocation node) {
//       log("\t=> ConstructorInvocation");
//       return true;
//    }

//    public boolean visit(ContinueStatement node) {
//       log("\t=> ContinueStatement");
//       return true;
//    }

//    public boolean visit(CreationReference node) {
//       log("\t=> CreationReference");
//       return true;
//    }

//    public boolean visit(Dimension node) {
//       log("\t=> Dimension");
//       return true;
//    }

//    public boolean visit(DoStatement node) {
//       log("\t=> DoStatement");
//       return true;
//    }

//    public boolean visit(EmptyStatement node) {
//       log("\t=> EmptyStatement");
//       return true;
//    }

//    public boolean visit(EnhancedForStatement node) {
//       log("\t=> EnhancedForStatement");
//       return true;
//    }

//    public boolean visit(EnumConstantDeclaration node) {
//       log("\t=> EnumConstantDeclaration");
//       return true;
//    }

//    public boolean visit(EnumDeclaration node) {
//       log("\t=> EnumDeclaration");
//       return true;
//    }

//    public boolean visit(ExportsDirective node) {
//       log("\t=> ExportsDirective");
//       return true;
//    }

//    public boolean visit(ExpressionMethodReference node) {
//       log("\t=> ExpressionMethodReference");
//       return true;
//    }

//    public boolean visit(ExpressionStatement node) {
//       log("\t=> ExpressionStatement");
//       return true;
//    }

//    public boolean visit(FieldAccess node) {
//       log("\t=> FieldAccess");
//       return true;
//    }

//    public boolean visit(FieldDeclaration node) {
//       log("\t=> FieldDeclaration");
//       return true;
//    }

//    public boolean visit(ForStatement node) {
//       log("\t=> ForStatement");
//       return true;
//    }

//    public boolean visit(GuardedPattern node) {
//       log("\t=> GuardedPattern");
//       return true;
//    }

//    public boolean visit(IfStatement node) {
//       log("\t=> IfStatement");
//       return true;
//    }

//    public boolean visit(ImportDeclaration node) {
//       log("\t=> ImportDeclaration");
//       return true;
//    }

//    public boolean visit(InfixExpression node) {
//       log("\t=> InfixExpression");
//       return true;
//    }

//    public boolean visit(Initializer node) {
//       log("\t=> Initializer");
//       return true;
//    }

//    public boolean visit(InstanceofExpression node) {
//       log("\t=> InstanceofExpression");
//       return true;
//    }

//    public boolean visit(IntersectionType node) {
//       log("\t=> IntersectionType");
//       return true;
//    }

//    public boolean visit(JavaDocTextElement node) {
//       log("\t=> JavaDocTextElement");
//       return true;
//    }

//    public boolean visit(LabeledStatement node) {
//       log("\t=> LabeledStatement");
//       return true;
//    }

//    public boolean visit(LambdaExpression node) {
//       log("\t=> LambdaExpression");
//       return true;
//    }

//    public boolean visit(LineComment node) {
//       log("\t=> LineComment");
//       return true;
//    }

//    public boolean visit(MarkerAnnotation node) {
//       log("\t=> MarkerAnnotation");
//       return true;
//    }

//    public boolean visit(MemberRef node) {
//       log("\t=> MemberRef");
//       return true;
//    }

//    public boolean visit(MemberValuePair node) {
//       log("\t=> MemberValuePair");
//       return true;
//    }

//    public boolean visit(MethodRef node) {
//       log("\t=> MethodRef");
//       return true;
//    }

//    public boolean visit(MethodRefParameter node) {
//       log("\t=> MethodRefParameter");
//       return true;
//    }

//    public boolean visit(MethodDeclaration node) {
//       log("\t=> MethodDeclaration");
//       return true;
//    }

//    public boolean visit(MethodInvocation node) {
//       log("\t=> MethodInvocation");
//       return true;
//    }

//    public boolean visit(Modifier node) {
//       log("\t=> Modifier");
//       return true;
//    }

//    public boolean visit(ModuleDeclaration node) {
//       log("\t=> ModuleDeclaration");
//       return true;
//    }

//    public boolean visit(ModuleModifier node) {
//       log("\t=> ModuleModifier");
//       return true;
//    }

//    public boolean visit(NameQualifiedType node) {
//       log("\t=> NameQualifiedType");
//       return true;
//    }

//    public boolean visit(NormalAnnotation node) {
//       log("\t=> NormalAnnotation");
//       return true;
//    }

//    public boolean visit(NullLiteral node) {
//       log("\t=> NullLiteral");
//       return true;
//    }

//    public boolean visit(NullPattern node) {
//       log("\t=> NullPattern");
//       return true;
//    }

//    public boolean visit(NumberLiteral node) {
//       log("\t=> NumberLiteral");
//       return true;
//    }

//    public boolean visit(OpensDirective node) {
//       log("\t=> OpensDirective");
//       return true;
//    }

//    public boolean visit(PackageDeclaration node) {
//       log("\t=> PackageDeclaration");
//       return true;
//    }

//    public boolean visit(ParameterizedType node) {
//       log("\t=> ParameterizedType");
//       return true;
//    }

//    public boolean visit(ParenthesizedExpression node) {
//       log("\t=> ParenthesizedExpression");
//       return true;
//    }

//    public boolean visit(PatternInstanceofExpression node) {
//       log("\t=> PatternInstanceofExpression");
//       return true;
//    }

//    public boolean visit(PostfixExpression node) {
//       log("\t=> PostfixExpression");
//       return true;
//    }

//    public boolean visit(PrefixExpression node) {
//       log("\t=> PrefixExpression");
//       return true;
//    }

//    public boolean visit(ProvidesDirective node) {
//       log("\t=> ProvidesDirective");
//       return true;
//    }

//    public boolean visit(PrimitiveType node) {
//       log("\t=> PrimitiveType");
//       return true;
//    }

//    public boolean visit(QualifiedName node) {
//       log("\t=> QualifiedName");
//       return true;
//    }

//    public boolean visit(QualifiedType node) {
//       log("\t=> QualifiedType");
//       return true;
//    }

//    public boolean visit(ModuleQualifiedName node) {
//       log("\t=> ModuleQualifiedName");
//       return true;
//    }

//    public boolean visit(RequiresDirective node) {
//       log("\t=> RequiresDirective");
//       return true;
//    }

//    public boolean visit(RecordDeclaration node) {
//       log("\t=> RecordDeclaration");
//       return true;
//    }

//    public boolean visit(RecordPattern node) {
//       log("\t=> RecordPattern");
//       return true;
//    }

//    public boolean visit(ReturnStatement node) {
//       log("\t=> ReturnStatement");
//       return true;
//    }

//    // public boolean visit(SimpleName node) {
//    // log("\t=> SimpleName");
//    // return true;
//    // }

//    public boolean visit(SimpleType node) {
//       log("\t=> SimpleType");
//       return true;
//    }

//    public boolean visit(SingleMemberAnnotation node) {
//       log("\t=> SingleMemberAnnotation");
//       return true;
//    }

//    public boolean visit(SingleVariableDeclaration node) {
//       log("\t=> SingleVariableDeclaration");
//       return true;
//    }

//    public boolean visit(StringLiteral node) {
//       log("\t=> StringLiteral");
//       return true;
//    }

//    public boolean visit(SuperConstructorInvocation node) {
//       log("\t=> SuperConstructorInvocation");
//       return true;
//    }

//    public boolean visit(SuperFieldAccess node) {
//       log("\t=> SuperFieldAccess");
//       return true;
//    }

//    public boolean visit(SuperMethodInvocation node) {
//       log("\t=> SuperMethodInvocation");
//       return true;
//    }

//    public boolean visit(SuperMethodReference node) {
//       log("\t=> SuperMethodReference");
//       return true;
//    }

//    public boolean visit(SwitchCase node) {
//       log("\t=> SwitchCase");
//       return true;
//    }

//    public boolean visit(SwitchExpression node) {
//       log("\t=> SwitchExpression");
//       return true;
//    }

//    public boolean visit(SwitchStatement node) {
//       log("\t=> SwitchStatement");
//       return true;
//    }

//    public boolean visit(SynchronizedStatement node) {
//       log("\t=> SynchronizedStatement");
//       return true;
//    }

//    public boolean visit(TagElement node) {
//       log("\t=> TagElement");
//       return true;
//    }

//    public boolean visit(TagProperty node) {
//       log("\t=> TagProperty");
//       return true;
//    }

//    public boolean visit(TextBlock node) {
//       log("\t=> TextBlock");
//       return true;
//    }

//    public boolean visit(TextElement node) {
//       log("\t=> TextElement");
//       return true;
//    }

//    public boolean visit(ThisExpression node) {
//       log("\t=> ThisExpression");
//       return true;
//    }

//    public boolean visit(ThrowStatement node) {
//       log("\t=> ThrowStatement");
//       return true;
//    }

//    public boolean visit(TryStatement node) {
//       log("\t=> TryStatement");
//       return true;
//    }

//    public boolean visit(TypeDeclaration node) {
//       log("\t=> TypeDeclaration");
//       return true;
//    }

//    public boolean visit(TypeDeclarationStatement node) {
//       log("\t=> TypeDeclarationStatement");
//       return true;
//    }

//    public boolean visit(TypeLiteral node) {
//       log("\t=> TypeLiteral");
//       return true;
//    }

//    public boolean visit(TypeMethodReference node) {
//       log("\t=> TypeMethodReference");
//       return true;
//    }

//    public boolean visit(TypeParameter node) {
//       log("\t=> TypeParameter");
//       return true;
//    }

//    public boolean visit(TypePattern node) {
//       log("\t=> TypePattern");
//       return true;
//    }

//    public boolean visit(UnionType node) {
//       log("\t=> UnionType");
//       return true;
//    }

//    public boolean visit(UsesDirective node) {
//       log("\t=> UsesDirective");
//       return true;
//    }

//    public boolean visit(VariableDeclarationExpression node) {
//       log("\t=> VariableDeclarationExpression");
//       return true;
//    }

//    public boolean visit(VariableDeclarationStatement node) {
//       log("\t=> VariableDeclarationStatement");
//       return true;
//    }

//    public boolean visit(VariableDeclarationFragment node) {
//       log("\t=> VariableDeclarationFragment");
//       return true;
//    }

//    public boolean visit(WhileStatement node) {
//       log("\t=> WhileStatement");
//       return true;
//    }

//    public boolean visit(WildcardType node) {
//       log("\t=> WildcardType");
//       return true;
//    }

//    public boolean visit(YieldStatement node) {
//       log("\t=> YieldStatement");
//       return true;
//    }
// }