package hexlet.code.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Encoder {
    public String encodePassword(String password) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(password.getBytes());
            return new String(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
