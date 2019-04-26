import java.nio.file.Files

separator = File.separator
String filename = '../../../target/it/generate-single-version/target/wildscribe-generated/index.html';
filename = filename.replace("/", separator)
File indexFile = new File(basedir, filename);

System.err.println('Generated index file ' + indexFile.getAbsolutePath());
assert indexFile.exists();

String indexFileContent = new String(Files.readAllBytes(indexFile.toPath()));
assert indexFileContent.contains("href=\"css/bootstrap.min.css\"")

return true;