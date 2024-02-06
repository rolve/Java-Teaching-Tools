import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JahreszeitTest {

    // astronomische Jahreszeiten https://de.wikipedia.org/wiki/Jahreszeit

    @Test
    public void testSommerEinfach() {
        assertEquals("Sommer", Jahreszeit.getJahreszeit(7, 1));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(7, 17));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(7, 31));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(8, 1));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(8, 9));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(8, 31));
    }

    @Test
    public void testSeptember() {
        assertEquals("Sommer", Jahreszeit.getJahreszeit(9, 1));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(9, 20));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(9, 21));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(9, 22));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(9, 23));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(9, 30));
    }

    @Test
    public void testHerbstEinfach() {
        assertEquals("Herbst", Jahreszeit.getJahreszeit(10, 1));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(10, 14));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(10, 31));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(11, 1));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(11, 21));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(11, 30));
    }

    @Test
    public void testDezember() {
        assertEquals("Herbst", Jahreszeit.getJahreszeit(12, 1));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(12, 20));
        assertEquals("Herbst", Jahreszeit.getJahreszeit(12, 21));
        assertEquals("Winter", Jahreszeit.getJahreszeit(12, 22));
        assertEquals("Winter", Jahreszeit.getJahreszeit(12, 23));
        assertEquals("Winter", Jahreszeit.getJahreszeit(12, 31));
    }

    @Test
    public void testWinterEinfach() {
        assertEquals("Winter", Jahreszeit.getJahreszeit(1, 1));
        assertEquals("Winter", Jahreszeit.getJahreszeit(1, 12));
        assertEquals("Winter", Jahreszeit.getJahreszeit(1, 31));
        assertEquals("Winter", Jahreszeit.getJahreszeit(2, 1));
        assertEquals("Winter", Jahreszeit.getJahreszeit(2, 19));
        assertEquals("Winter", Jahreszeit.getJahreszeit(2, 28));
    }

    @Test
    public void testMaerz() {
        assertEquals("Winter", Jahreszeit.getJahreszeit(3, 1));
        assertEquals("Winter", Jahreszeit.getJahreszeit(3, 18));
        assertEquals("Winter", Jahreszeit.getJahreszeit(3, 19));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(3, 20));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(3, 21));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(3, 31));
    }

    @Test
    public void testFruehlingEinfach() {
        assertEquals("Frühling", Jahreszeit.getJahreszeit(4, 1));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(4, 15));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(4, 30));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(5, 1));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(5, 18));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(5, 31));
    }

    @Test
    public void testJuni() {
        assertEquals("Frühling", Jahreszeit.getJahreszeit(6, 1));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(6, 19));
        assertEquals("Frühling", Jahreszeit.getJahreszeit(6, 20));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(6, 21));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(6, 22));
        assertEquals("Sommer", Jahreszeit.getJahreszeit(6, 30));
    }
}