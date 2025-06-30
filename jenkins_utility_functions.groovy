// Calculates the duration in seconds between two Date objects.
// 'end' defaults to the current date if not provided.
int getDurationSeconds(Date start, Date end = new Date()) {
    return ((int)(end.getTime()/1000 - start.getTime()/1000))
}

// Generates a random alphanumeric string of a specified length.
// Excludes 'l' and '0' to avoid common ambiguities.
String getRandomString(Integer len) {
    def pool = ['a'..'k','m'..'z',1..9].flatten()
    Random rand = new Random(System.currentTimeMillis())
    def passChars = (0..len).collect { pool[rand.nextInt(pool.size())] }
    def rdnStr = ""
    passChars.each { chr -> rdnStr = rdnStr + chr }
    return rdnStr
}

// Decodes a Base64 encoded string.
import java.util.Base64
String base64Decode(String string) {
    return new String(Base64.getDecoder().decode(string))
}

// Requires java.util.Base64 import.
import java.util.Base64
String base64Encode(String string) {
    return Base64.getEncoder().encodeToString(string.getBytes());
}

// Prints the full stack trace of an exception to a string and then echoes it.
import java.io.PrintWriter
import java.io.StringWriter
void printStackTrace(Exception e) {
    def sw = new StringWriter()
    def pw = new PrintWriter(sw)
    e.printStackTrace(pw)
    echo sw.toString()
}