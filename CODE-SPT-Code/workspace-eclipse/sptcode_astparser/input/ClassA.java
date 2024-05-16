package pkg1;

public class ClassA {
   public void foo(int position, String buffer) {
      String data = buffer;
      int index = position / 100;

      if (index < 0) {
         index = -1 * index;
         System.out.println(data.charAt(index));
      } else {
         index = index + 1;
         System.out.println(data.charAt(index));
      }
   }
}
