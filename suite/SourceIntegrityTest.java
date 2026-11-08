/*
 * @test
 * @summary The Gallery source used by the suite is byte-for-byte identical to the supplied application
 * @run main SourceIntegrityTest
 */
import java.nio.file.*;
import java.security.*;
import java.util.*;

public class SourceIntegrityTest {
    public static void main(String[] args) throws Exception {
        Path source = Path.of(System.getProperty("test.src", "suite"), "lib", "Gallery.java");
        String actual = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(source)));
        String expected = "06c13fe21b278bcf8192b35f45041aef6698ed1d4fa64a0ab9a6c869cff1f3bb";
        if (!actual.equals(expected)) {
            throw new AssertionError("Gallery.java must remain unchanged: expected SHA-256=" + expected + "; actual=" + actual);
        }
        System.out.println("Original Gallery.java SHA-256 verified: " + actual);
    }
}
