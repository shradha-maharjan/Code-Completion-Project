package datactrlflow;

import org.eclipse.jdt.core.dom.TypeDeclaration;

public class EnclosingClassInfo {
   private final TypeDeclaration enclosingClass;
   private final boolean isDirectClass;

   public EnclosingClassInfo(TypeDeclaration enclosingClass, boolean isDirectClass) {
       this.enclosingClass = enclosingClass;
       this.isDirectClass = isDirectClass;
   }

   public TypeDeclaration getEnclosingClass() {
       return enclosingClass;
   }

   public boolean isDirectClass() {
       return isDirectClass;
   }
}