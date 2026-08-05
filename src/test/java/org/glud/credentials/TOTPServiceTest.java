package org.glud.credentials;

import org.glud.credentials.totp_seed.service.TOTPService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@TestPropertySource(properties = {
        "totp.secret=8xJkLp2QwErTyUiOpAsDfGhJkLzXcVbNmQwErTyU=",
        "totp.tenant=TESTAPP"
})
class TOTPServiceTest {

    private static final Pattern BASE32 = Pattern.compile("^[a-z2-7]+$");
    private static final String RFC6238_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Autowired
    TOTPService totpService;

    @Test
    void generatedSeed_isValidBase32() throws NoSuchAlgorithmException {
        String seed = totpService.generateSeed("20210000000", "GLUD");
        assertEquals(52, seed.length());
        assertTrue(BASE32.matcher(seed).matches(), "La seed debe ser Base32 (a-z2-7)");
    }

    @Test
    void generatedSeed_returnsSameSeedForSameParameters() throws NoSuchAlgorithmException {
        assertEquals("4ljjwnrzlorsnlhdmitrl4rubdftyhc64bt3qqsnhjbdbq2uqyhq", totpService.generateSeed("123456L", "TESTAPP"));
        assertEquals("56awc4clgvjbf7rnbeyfs3uyly6vyn4xiq6ptkyss335j2a62edq", totpService.generateSeed("BraveNewWorld", "TESTAPP"));
        assertEquals("tita2k7w3yuw4seqldobhf3ciefwitkzo6zic3uvcjzuc74ymoma", totpService.generateSeed("TheColorFromTheSky", "TESTAPP"));
        assertEquals("tita2k7w3yuw4seqldobhf3ciefwitkzo6zic3uvcjzuc74ymoma", totpService.generateSeed("TheColorFromTheSky", "TESTAPP"));
        assertEquals("3qhfryvxejq7yhmsgtz7okoylysulqkm77vx26dbuljsqamu66ma", totpService.generateSeed("Otelo", "TESTAPP"));
    }

    @Test
    void generatedSeed_changesWhenTenantChanges() throws NoSuchAlgorithmException {
        String seedGlud = totpService.generateSeed("20210000000", "GLUD");
        String seedOther = totpService.generateSeed("20210000000", "OTRO");
        assertEquals(52, seedGlud.length());
        assertEquals(52, seedOther.length());
        assertNotEquals(seedGlud, seedOther, "Distintos tenants deben producir seeds distintas");
    }

    @Test
    void generateCode_matchesRfc6238Vectors() throws NoSuchAlgorithmException, InvalidKeyException {
        assertEquals("287082", totpService.generateCode(RFC6238_SECRET, 1L));
        assertEquals("081804", totpService.generateCode(RFC6238_SECRET, 37037036L));
        assertEquals("050471", totpService.generateCode(RFC6238_SECRET, 37037037L));
        assertEquals("005924", totpService.generateCode(RFC6238_SECRET, 41152263L));
        assertEquals("279037", totpService.generateCode(RFC6238_SECRET, 66666666L));
        assertEquals("353130", totpService.generateCode(RFC6238_SECRET, 666666666L));
    }

    @Test
    void verify_acceptsCodeGeneratedForCurrentStep() throws NoSuchAlgorithmException, InvalidKeyException {
        String seed = totpService.generateSeed("20210000000", "GLUD");
        long timeIndex = Instant.now().getEpochSecond() / 30;

        assertTrue(totpService.verify(seed, totpService.generateCode(seed, timeIndex)),
                "El código del paso actual debe ser aceptado");
    }

    @Test
    void verify_rejectsWrongCode() throws NoSuchAlgorithmException, InvalidKeyException {
        String seed = totpService.generateSeed("20210000000", "GLUD");

        assertFalse(totpService.verify(seed, "000000"), "Un código incorrecto debe ser rechazado");
    }
}
