/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package santaclaus;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;

/**
 *
 * @author rubenbustos
 */
public class SantaClaus implements Runnable {

    static final int REINDEER = 9;
    static final int ELVES = 9;
    static final int ELVES_TO_WAKE_SANTA = 3;
    static volatile int num_reindeer = 0;
    static volatile int num_elves = 0;
    static volatile int num_elves_helped = 0;
    static Semaphore reindeer = new Semaphore(1);
    static Semaphore elves = new Semaphore(3);
    static Semaphore santa = new Semaphore(0);
    static Semaphore reindeer_wait = new Semaphore(0);
    static Semaphore elves_mutex = new Semaphore(1);
    static Semaphore santa_mutex = new Semaphore(1);
    static Semaphore ask_help = new Semaphore(0);
    static Semaphore santa_help = new Semaphore(0);
    static Semaphore santa_load = new Semaphore(0);
    Random rnd = new Random();

    private final int id;

    public static void main(String[] args) throws InterruptedException {

        new SantaClaus(0).init();
    }

    private void init() throws InterruptedException {
        Thread[] threads = new Thread[REINDEER + ELVES + 1];

        threads[0] = new Thread(new SantaClaus(id));
        threads[0].start();

        for (int i = 1; i <= REINDEER; i++) {
            threads[i] = new Thread(new Reindeer(i));
            threads[i].start();
        }

        for (int i = REINDEER + 1; i <= ELVES + REINDEER; i++) {
            threads[i] = new Thread(new Elf(i));
            threads[i].start();
        }

        for (int i = 0; i < ELVES + REINDEER + 1; i++) {
            threads[i].join();
        }
    }

    public SantaClaus(int id) {
        this.id = id;
    }

    @Override
    public void run() {
        try {

            while (num_elves_helped < 6) {
                System.out.println("SANTA: let's rest");
                santa.acquire();

                if (num_reindeer == REINDEER) {
                    System.out.println("SANTA: All loaded");

                    santa_mutex.acquire();
                    num_reindeer = 0;
                    santa_mutex.release();

                    for (int i = 0; i < REINDEER - 1; i++) { // -1 because the last one does not acquire
                        reindeer_wait.release();
                    }
                    for(int i = 0; i < REINDEER; i++){
                        santa_load.release();
                    }
                }
                if (num_elves == ELVES_TO_WAKE_SANTA) {
                    santa_mutex.acquire();
                    System.out.println("SANTA: Helping these bois");
                    num_elves = 0;
                    num_elves_helped++;
                    System.out.printf("SANTA: Helped %d groups\n", num_elves_helped);
                    santa_mutex.release();

                    for (int i = 0; i < ELVES_TO_WAKE_SANTA - 1; i++) {
                        ask_help.release();
                    }
                    for (int i = 0; i < ELVES_TO_WAKE_SANTA; i++) {
                        santa_help.release();
                    }
                }
            }

        } catch (InterruptedException ex) {
            Logger.getLogger(SantaClaus.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    class Reindeer implements Runnable {

        private final int id;

        public Reindeer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(rnd.nextInt(500) + 500);

                System.out.printf("Reindeer %d arrived\n", id);
                reindeer.acquire();

                num_reindeer++;

                if (num_reindeer == REINDEER) {
                    System.out.printf("I'm the last! %d\n", id);

                    reindeer.release();
                    santa.release();
                } else {
                    reindeer.release();
                    reindeer_wait.acquire();
                }

                santa_load.acquire();
                
                System.out.printf("Reindeer %d ends\n", id);
            } catch (InterruptedException ex) {
                Logger.getLogger(SantaClaus.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    class Elf implements Runnable {

        private final int id;

        public Elf(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < 2; i++) {
                    Thread.sleep(rnd.nextInt(500) + 500);
                    
                    elves.acquire();

                    System.out.printf("Elf %d needs help\n", id);
                    elves_mutex.acquire();

                    num_elves++;

                    if (num_elves == ELVES_TO_WAKE_SANTA) {
                        System.out.println("Asking Santa some help");
                        elves_mutex.release();
                        santa.release();
                    } else {
                        elves_mutex.release();
                        ask_help.acquire();
                    }

                    santa_help.acquire();

                    elves.release();
                }

                System.out.printf("Elf %d ends\n", id);
            } catch (InterruptedException ex) {
                Logger.getLogger(SantaClaus.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
