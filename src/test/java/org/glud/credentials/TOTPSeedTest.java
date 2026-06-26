package org.glud.credentials;

import org.glud.credentials.TOTPseed.Service.TOTPService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

import java.security.NoSuchAlgorithmException;

@SpringBootTest
@TestPropertySource(properties = {
        "totp.secret=8xJkLp2QwErTyUiOpAsDfGhJkLzXcVbNmQwErTyU=",
        "totp.tenant=TESTAPP"
})
public class TOTPSeedTest {

    @Autowired
    TOTPService totpService;

    @Test
    void generatedSeed_returns16charactersToken() throws NoSuchAlgorithmException {
        String seed = totpService.generateSeed("12345L");
        assertEquals(16, seed.length());
    }

    @Test
    void generatedSeed_returnSameSeedForIntroducedParameters() throws NoSuchAlgorithmException{
        assertEquals("4tKbNjlboyas42In", totpService.generateSeed("123456L"));
        assertEquals("74FhcEs1UhL+LQkw", totpService.generateSeed("BraveNewWorld"));
        assertEquals("miYNK/beKW5IkFjc", totpService.generateSeed("TheColorFromTheSky"));
        assertEquals("miYNK/beKW5IkFjc", totpService.generateSeed("TheColorFromTheSky"));
        assertEquals("3A5Y4rciYfwdkjTz", totpService.generateSeed("Otelo"));
    }



}
