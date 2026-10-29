package net.sirplop.aetherworks.util;

import com.mojang.datafixers.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WeightedList<T> {
    public final List<Pair<T, Double>> internalList;
    private double totalWeight = 0;
    private final Random random;

    public WeightedList() {
        internalList = new ArrayList<>();
        random = new Random();
    }

    public void add(T val, double weight) {
        internalList.add(Pair.of(val, weight));
        totalWeight += weight;
    }
    public void remove(T val) {
        for (int i = 0; i < internalList.size(); i++)
        {
            if (internalList.get(i).getFirst().equals(val))
            {
                totalWeight -= internalList.get(i).getSecond();
                internalList.remove(i);
                return;
            }
        }
    }

    public T choose() {
        if (internalList.isEmpty())
            throw new IllegalStateException("WeightedList is empty.");
        if (internalList.size() == 1)
            return internalList.get(0).getFirst(); //only one option

        double selectedWeight = random.nextDouble(0, totalWeight);
        for (int i = 0; i < internalList.size() - 1; i++)
        {
            Pair<T, Double> pair = internalList.get(i);
            selectedWeight -= pair.getSecond();
            if (0 >= selectedWeight)
                return pair.getFirst();
        } //skip checking last one because we know it's the one.
        return internalList.get(internalList.size() - 1).getFirst();
    }
}
