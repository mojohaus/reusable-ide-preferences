boolean found = false;
String expected = "No dependency of type eclipse-formatter found, skipping";
new File(basedir,"build.log").eachLine { line ->
  if ( line.contains(expected) ) {
    found = true;
  }
}

if ( !found ) {
    throw new RuntimeException("Expected text not found : ${expected}")
}