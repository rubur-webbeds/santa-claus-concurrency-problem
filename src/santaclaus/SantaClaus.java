/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package santaclaus;

import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author rubenbustos
 */
public class SantaClaus implements Runnable {

    static final int REINDEER = 9;
    static final int ELVES = 9;
    static volatile int num_reindeer = 0;
    static volatile int num_elves = 0;
    static volatile int num_reindeer_loaded = 0;
    static volatile int num_elves_helped = 0;
    static Semaphore reindeer = new Semaphore(1);
    static Semaphore elves = new Semaphore(2);
    static Semaphore reindeer_wait = new Semaphore(0);
    static Semaphore elves_wait = new Semaphore(1);
    static Semaphore santa = new Semaphore(0);
    static Semaphore santa_mutex = new Semaphore(1);
    static Semaphore ask_help = new Semaphore(1);

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

            while (num_reindeer_loaded != 1 || num_elves_helped != 3) {
                System.out.println("SANTA: let's rest");
                santa.acquire();
                //santa_mutex.acquire();
                
                if (num_reindeer == REINDEER) {
                    System.out.println("SANTA: All loaded");

                    num_reindeer = 0;
                    num_reindeer_loaded++;

                    for (int i = 0; i < REINDEER - 1; i++) { // -1 because the last one does not acquire
                        reindeer_wait.release();
                    }
                } else {
                    System.out.println("SANTA: Helping these bois");

                    num_elves = 0;
                    num_elves_helped++;
                    System.err.printf("helped %d\n", num_elves_helped);

                    for (int i = 0; i < 3; i++) {
                        ask_help.release();
                    }
                }
                
                //santa_mutex.release();
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
                Thread.sleep(5000);
                
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
                for (int i = 0; i < 1; i++) {
                    //System.err.printf("%d: permits %d\n", id, elves.availablePermits());
                    elves.acquire();
                    
                    System.out.printf("Elf %d needs help\n", id);
                    elves_wait.acquire();
                    
                    num_elves++;
                    
                    if (num_elves == 3) {
                        santa.release();
                    }
                    
                    elves_wait.release();

                    ask_help.acquire();
                    
                    elves.release();
                }

                System.out.printf("Elf %d ends\n", id);
            } catch (InterruptedException ex) {
                Logger.getLogger(SantaClaus.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
