package engineer.pol.polutilities.utils;

public class RandomUtils {

    public static int getRandomInt(int min, int max) {
        return (int) (Math.random() * (max - min + 1)) + min;
    }

}
