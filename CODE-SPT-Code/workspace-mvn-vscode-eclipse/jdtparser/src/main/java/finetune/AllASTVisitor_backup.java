package finetune;

import java.io.FileWriter;
import java.io.IOException;
import org.eclipse.jdt.core.dom.*;

public class AllASTVisitor_backup extends ASTVisitor {
    private FileWriter writer;

    // Constructor to accept a FileWriter for output
    public AllASTVisitor_backup(FileWriter writer) {
        this.writer = writer;
    }
    
    private void log(String message) {
//        // Write to console
//        try {
//            System.out.println(message);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        // Write to the output file
        try {
            writer.write(message + "\n");
            writer.flush(); // Ensure the message is written immediately
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

   public boolean visit(AnnotationTypeDeclaration node) {
      log("\t=> AnnotationTypeDeclaration");
      return true;
   }

   public boolean visit(AnnotationTypeMemberDeclaration node) {
      log("\t=> AnnotationTypeMemberDeclaration");
      return true;
   }

   public boolean visit(AnonymousClassDeclaration node) {
      log("\t=> AnonymousClassDeclaration");
      return true;
   }

   public boolean visit(ArrayAccess node) {
      log("\t=> ArrayAccess");
      return true;
   }

   public boolean visit(ArrayCreation node) {
      log("\t=> ArrayCreation");
      return true;
   }

   public boolean visit(ArrayInitializer node) {
      log("\t=> ArrayInitializer");
      return true;
   }

   public boolean visit(ArrayType node) {
      log("\t=> ArrayType");
      return true;
   }

   public boolean visit(AssertStatement node) {
      log("\t=> AssertStatement");
      return true;
   }

   public boolean visit(Assignment node) {
      log("\t=> Assignment");
      return true;
   }

   public boolean visit(Block node) {
      log("\t=> Block");
      return true;
   }

   public boolean visit(BlockComment node) {
      log("\t=> BlockComment");
      return true;
   }

   public boolean visit(BooleanLiteral node) {
      log("\t=> BooleanLiteral");
      return true;
   }

   public boolean visit(BreakStatement node) {
      log("\t=> BreakStatement");
      return true;
   }

   public boolean visit(CaseDefaultExpression node) {
      log("\t=> CaseDefaultExpression");
      return true;
   }

   public boolean visit(CastExpression node) {
      log("\t=> CastExpression");
      return true;
   }

   public boolean visit(CatchClause node) {
      log("\t=> CatchClause");
      return true;
   }

   public boolean visit(CharacterLiteral node) {
      log("\t=> CharacterLiteral");
      return true;
   }

   public boolean visit(ClassInstanceCreation node) {
      log("\t=> ClassInstanceCreation");
      return true;
   }

   public boolean visit(CompilationUnit node) {
      log("\t=> CompilationUnit");
      return true;
   }

   public boolean visit(ConditionalExpression node) {
      log("\t=> ConditionalExpression");
      return true;
   }

   public boolean visit(ConstructorInvocation node) {
      log("\t=> ConstructorInvocation");
      return true;
   }

   public boolean visit(ContinueStatement node) {
      log("\t=> ContinueStatement");
      return true;
   }

   public boolean visit(CreationReference node) {
      log("\t=> CreationReference");
      return true;
   }

   public boolean visit(Dimension node) {
      log("\t=> Dimension");
      return true;
   }

   public boolean visit(DoStatement node) {
      log("\t=> DoStatement");
      return true;
   }

   public boolean visit(EmptyStatement node) {
      log("\t=> EmptyStatement");
      return true;
   }

   public boolean visit(EnhancedForStatement node) {
      log("\t=> EnhancedForStatement");
      return true;
   }

   public boolean visit(EnumConstantDeclaration node) {
      log("\t=> EnumConstantDeclaration");
      return true;
   }

   public boolean visit(EnumDeclaration node) {
      log("\t=> EnumDeclaration");
      return true;
   }

   public boolean visit(ExportsDirective node) {
      log("\t=> ExportsDirective");
      return true;
   }

   public boolean visit(ExpressionMethodReference node) {
      log("\t=> ExpressionMethodReference");
      return true;
   }

   public boolean visit(ExpressionStatement node) {
      log("\t=> ExpressionStatement");
      return true;
   }

   public boolean visit(FieldAccess node) {
      log("\t=> FieldAccess");
      return true;
   }

   public boolean visit(FieldDeclaration node) {
      log("\t=> FieldDeclaration");
      return true;
   }

   public boolean visit(ForStatement node) {
      log("\t=> ForStatement");
      return true;
   }

   public boolean visit(GuardedPattern node) {
      log("\t=> GuardedPattern");
      return true;
   }

   public boolean visit(IfStatement node) {
      log("\t=> IfStatement");
      return true;
   }

   public boolean visit(ImportDeclaration node) {
      log("\t=> ImportDeclaration");
      return true;
   }

   public boolean visit(InfixExpression node) {
      log("\t=> InfixExpression");
      return true;
   }

   public boolean visit(Initializer node) {
      log("\t=> Initializer");
      return true;
   }

   public boolean visit(InstanceofExpression node) {
      log("\t=> InstanceofExpression");
      return true;
   }

   public boolean visit(IntersectionType node) {
      log("\t=> IntersectionType");
      return true;
   }

   public boolean visit(JavaDocTextElement node) {
      log("\t=> JavaDocTextElement");
      return true;
   }

   public boolean visit(LabeledStatement node) {
      log("\t=> LabeledStatement");
      return true;
   }

   public boolean visit(LambdaExpression node) {
      log("\t=> LambdaExpression");
      return true;
   }

   public boolean visit(LineComment node) {
      log("\t=> LineComment");
      return true;
   }

   public boolean visit(MarkerAnnotation node) {
      log("\t=> MarkerAnnotation");
      return true;
   }

   public boolean visit(MemberRef node) {
      log("\t=> MemberRef");
      return true;
   }

   public boolean visit(MemberValuePair node) {
      log("\t=> MemberValuePair");
      return true;
   }

   public boolean visit(MethodRef node) {
      log("\t=> MethodRef");
      return true;
   }

   public boolean visit(MethodRefParameter node) {
      log("\t=> MethodRefParameter");
      return true;
   }

   public boolean visit(MethodDeclaration node) {
      log("\t=> MethodDeclaration");
      return true;
   }

   public boolean visit(MethodInvocation node) {
      log("\t=> MethodInvocation");
      return true;
   }

   public boolean visit(Modifier node) {
      log("\t=> Modifier");
      return true;
   }

   public boolean visit(ModuleDeclaration node) {
      log("\t=> ModuleDeclaration");
      return true;
   }

   public boolean visit(ModuleModifier node) {
      log("\t=> ModuleModifier");
      return true;
   }

   public boolean visit(NameQualifiedType node) {
      log("\t=> NameQualifiedType");
      return true;
   }

   public boolean visit(NormalAnnotation node) {
      log("\t=> NormalAnnotation");
      return true;
   }

   public boolean visit(NullLiteral node) {
      log("\t=> NullLiteral");
      return true;
   }

   public boolean visit(NullPattern node) {
      log("\t=> NullPattern");
      return true;
   }

   public boolean visit(NumberLiteral node) {
      log("\t=> NumberLiteral");
      return true;
   }

   public boolean visit(OpensDirective node) {
      log("\t=> OpensDirective");
      return true;
   }

   public boolean visit(PackageDeclaration node) {
      log("\t=> PackageDeclaration");
      return true;
   }

   public boolean visit(ParameterizedType node) {
      log("\t=> ParameterizedType");
      return true;
   }

   public boolean visit(ParenthesizedExpression node) {
      log("\t=> ParenthesizedExpression");
      return true;
   }

   public boolean visit(PatternInstanceofExpression node) {
      log("\t=> PatternInstanceofExpression");
      return true;
   }

   public boolean visit(PostfixExpression node) {
      log("\t=> PostfixExpression");
      return true;
   }

   public boolean visit(PrefixExpression node) {
      log("\t=> PrefixExpression");
      return true;
   }

   public boolean visit(ProvidesDirective node) {
      log("\t=> ProvidesDirective");
      return true;
   }

   public boolean visit(PrimitiveType node) {
      log("\t=> PrimitiveType");
      return true;
   }

   public boolean visit(QualifiedName node) {
      log("\t=> QualifiedName");
      return true;
   }

   public boolean visit(QualifiedType node) {
      log("\t=> QualifiedType");
      return true;
   }

   public boolean visit(ModuleQualifiedName node) {
      log("\t=> ModuleQualifiedName");
      return true;
   }

   public boolean visit(RequiresDirective node) {
      log("\t=> RequiresDirective");
      return true;
   }

   public boolean visit(RecordDeclaration node) {
      log("\t=> RecordDeclaration");
      return true;
   }

   public boolean visit(RecordPattern node) {
      log("\t=> RecordPattern");
      return true;
   }

   public boolean visit(ReturnStatement node) {
      log("\t=> ReturnStatement");
      return true;
   }

   // public boolean visit(SimpleName node) {
   // log("\t=> SimpleName");
   // return true;
   // }

   public boolean visit(SimpleType node) {
      log("\t=> SimpleType");
      return true;
   }

   public boolean visit(SingleMemberAnnotation node) {
      log("\t=> SingleMemberAnnotation");
      return true;
   }

   public boolean visit(SingleVariableDeclaration node) {
      log("\t=> SingleVariableDeclaration");
      return true;
   }

   public boolean visit(StringLiteral node) {
      log("\t=> StringLiteral");
      return true;
   }

   public boolean visit(SuperConstructorInvocation node) {
      log("\t=> SuperConstructorInvocation");
      return true;
   }

   public boolean visit(SuperFieldAccess node) {
      log("\t=> SuperFieldAccess");
      return true;
   }

   public boolean visit(SuperMethodInvocation node) {
      log("\t=> SuperMethodInvocation");
      return true;
   }

   public boolean visit(SuperMethodReference node) {
      log("\t=> SuperMethodReference");
      return true;
   }

   public boolean visit(SwitchCase node) {
      log("\t=> SwitchCase");
      return true;
   }

   public boolean visit(SwitchExpression node) {
      log("\t=> SwitchExpression");
      return true;
   }

   public boolean visit(SwitchStatement node) {
      log("\t=> SwitchStatement");
      return true;
   }

   public boolean visit(SynchronizedStatement node) {
      log("\t=> SynchronizedStatement");
      return true;
   }

   public boolean visit(TagElement node) {
      log("\t=> TagElement");
      return true;
   }

   public boolean visit(TagProperty node) {
      log("\t=> TagProperty");
      return true;
   }

   public boolean visit(TextBlock node) {
      log("\t=> TextBlock");
      return true;
   }

   public boolean visit(TextElement node) {
      log("\t=> TextElement");
      return true;
   }

   public boolean visit(ThisExpression node) {
      log("\t=> ThisExpression");
      return true;
   }

   public boolean visit(ThrowStatement node) {
      log("\t=> ThrowStatement");
      return true;
   }

   public boolean visit(TryStatement node) {
      log("\t=> TryStatement");
      return true;
   }

   public boolean visit(TypeDeclaration node) {
      log("\t=> TypeDeclaration");
      return true;
   }

   public boolean visit(TypeDeclarationStatement node) {
      log("\t=> TypeDeclarationStatement");
      return true;
   }

   public boolean visit(TypeLiteral node) {
      log("\t=> TypeLiteral");
      return true;
   }

   public boolean visit(TypeMethodReference node) {
      log("\t=> TypeMethodReference");
      return true;
   }

   public boolean visit(TypeParameter node) {
      log("\t=> TypeParameter");
      return true;
   }

   public boolean visit(TypePattern node) {
      log("\t=> TypePattern");
      return true;
   }

   public boolean visit(UnionType node) {
      log("\t=> UnionType");
      return true;
   }

   public boolean visit(UsesDirective node) {
      log("\t=> UsesDirective");
      return true;
   }

   public boolean visit(VariableDeclarationExpression node) {
      log("\t=> VariableDeclarationExpression");
      return true;
   }

   public boolean visit(VariableDeclarationStatement node) {
      log("\t=> VariableDeclarationStatement");
      return true;
   }

   public boolean visit(VariableDeclarationFragment node) {
      log("\t=> VariableDeclarationFragment");
      return true;
   }

   public boolean visit(WhileStatement node) {
      log("\t=> WhileStatement");
      return true;
   }

   public boolean visit(WildcardType node) {
      log("\t=> WildcardType");
      return true;
   }

   public boolean visit(YieldStatement node) {
      log("\t=> YieldStatement");
      return true;
   }
}