
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenPasswordHash {
    @Test
    void gen() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("Publisher@2026");
        System.out.println("HASH = " + hash);
        System.out.println("MATCH = " + encoder.matches("Publisher@2026", hash));
        System.out.println("LENGTH = " + hash.length());
    }
}