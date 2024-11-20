package util;

public enum CheckParsing {
   PARSE_FAILURE("Check Parse Failure"), //
   UNPARSED("Check Unparsed"), //
   BAD_CLASS("Check Bad Class"), //
   BAD_METHODS("Check Multi Methods"), //
   Pass("Pass"), //
   Unknown("Unknown");

   private String description;

   CheckParsing(String description) {
      this.description = description;
   }

   public String getDescription() {
      return this.description;
   }
}
