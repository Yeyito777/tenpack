package dev.yeyito.tenpackfoodui;

final class FoodBalanceState {
    int staple;
    int protein;
    int produce;
    int decayClock;
    int tickBucket;
    double lastX;
    double lastY;
    double lastZ;

    void reset(double x, double y, double z) {
        staple = 0;
        protein = 0;
        produce = 0;
        decayClock = 0;
        tickBucket = 0;
        lastX = x;
        lastY = y;
        lastZ = z;
    }

    void add(String itemId) {
        staple = cap(staple + FoodBalanceValues.STAPLE.getOrDefault(itemId, 0));
        protein = cap(protein + FoodBalanceValues.PROTEIN.getOrDefault(itemId, 0));
        produce = cap(produce + FoodBalanceValues.PRODUCE.getOrDefault(itemId, 0));
    }

    boolean knows(String itemId) {
        return FoodBalanceValues.STAPLE.containsKey(itemId)
                || FoodBalanceValues.PROTEIN.containsKey(itemId)
                || FoodBalanceValues.PRODUCE.containsKey(itemId)
                || FoodBalanceValues.QUALITY.containsKey(itemId);
    }

    boolean activeDecayTick() {
        decayClock++;
        if (decayClock < FoodBalanceValues.DECAY_SECONDS_ACTIVE) {
            return false;
        }
        decayClock = 0;
        int oldStaple = staple;
        int oldProtein = protein;
        int oldProduce = produce;
        if (staple > 0) {
            staple--;
        }
        if (protein > 0) {
            protein--;
        }
        if (produce > 0) {
            produce--;
        }
        return oldStaple != staple || oldProtein != protein || oldProduce != produce;
    }

    FoodBalancePayload payload() {
        return new FoodBalancePayload(staple, protein, produce);
    }

    private static int cap(int value) {
        return Math.max(0, Math.min(FoodBalanceValues.MAX_POINTS, value));
    }
}
