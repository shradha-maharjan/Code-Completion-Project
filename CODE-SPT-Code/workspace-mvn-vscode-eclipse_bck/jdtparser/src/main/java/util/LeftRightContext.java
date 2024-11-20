package util;

public class LeftRightContext {
   String left, target, right;

   public LeftRightContext(String left, String target, String right) {
      this.left = left;
      this.target = target;
      this.right = right;
   }

   public String getLeft() {
      return left;
   }

   public String getTarget() {
      return target;
   }

   public String getRight() {
      return right;
   }
}
