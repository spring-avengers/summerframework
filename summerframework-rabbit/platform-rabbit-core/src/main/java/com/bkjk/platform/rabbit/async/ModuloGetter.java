package com.bkjk.platform.rabbit.async;

import java.util.concurrent.atomic.AtomicInteger;

public class ModuloGetter {
    public static void main(String[] args) {
        ModuloGetter moduloGetter = newModuloGetter();
        for (int i = 0; i < 100; i++) {
            System.out.println(moduloGetter.getNext(4));
        }
        System.out.println("-----");
        for (int i = 0; i < 100; i++) {
            System.out.println(moduloGetter.getNext(5));
        }
        System.out.println("-----");
        for (int i = 0; i < 100; i++) {
            System.out.println(moduloGetter.getNext(2));
        }
    }

    public static final ModuloGetter newModuloGetter() {
        return new ModuloGetter();
    }

    private AtomicInteger nextIndex = new AtomicInteger();

    private ModuloGetter() {

    }

    public int getNext(int modulo) {
        while (true) {
            int current = nextIndex.get();
            int next = (current + 1) % modulo;
            if (nextIndex.compareAndSet(current, next) && current < modulo)
                return current;

        }
    }
}
