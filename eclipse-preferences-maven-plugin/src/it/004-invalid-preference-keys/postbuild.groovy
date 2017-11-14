boolean found = false;
String expected = "Invalid property key 'invalid.pref.key'";
new File(basedir,"build.log").eachLine { line ->
  if ( line.contains(expected) ) {
    found = true;
  }
}

if ( !found ) {
    throw new RuntimeException("Expected text not found : ${expected}")
}