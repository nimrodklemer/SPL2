package bguspl.set.ex;

import java.io.Console;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable, PlayerContract {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    // Quoue for key actions.
    public Queue<Integer> chosenSlots;

    private Dealer dealer;

    public boolean keyContinue;

    // makes sure the player thread waits before we interrupt the dealer thread (in order to make sure player thread is waiting before dealer thread notifies them.)
    public Object waitBeforeInterruptPlayer;

    private Queue<Integer> queue;
    
    /**
     * The class constructor.
     *
     * @param env    - the game environment object.
     * @param table  - the table object.
     * @param dealer - the dealer object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        this.keyContinue = true;
        chosenSlots = new LinkedList<Integer>();
        waitBeforeInterruptPlayer = new Object();
        queue = new LinkedList<Integer>(3);
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop

            // if slot already in quoue remove it else add it
            int slot =queue.poll();
            if(chosenSlots.contains(slot)){
                //remove from the list.
                chosenSlots.remove(slot);
                //remove token from table.
                table.removeToken(id, slot);
            }
            else{
                if(chosenSlots.size() < 3){
                    //add to the list.
                    chosenSlots.add(slot);
                    //add token to table.
                    table.placeToken(id, slot);
                    
                    // when we picke 3 cards we will change keyContinue to false in order to block incming acion until the dealer test the set
                    if(chosenSlots.size() == 3){
                        keyContinue = false;
                    }
                }
                
            }

            //if third token placed
            // if yes - wait for point or penalty
            if(chosenSlots.size() == 3){
                synchronized(dealer.handleSetLock){
                    if(chosenSlots.size() == 3){
                        
                        int[] setAsArray = chosenSlots.stream().mapToInt(Integer::intValue).toArray();
                        //we submit the set we want to check to dealer
                        dealer.submitSet(id, setAsArray);

                        synchronized(waitBeforeInterruptPlayer){
                            //awaken the dealer 
                            dealer.interruptDealer(id);

                            try{
                                //waits for dealer to send notify, which would happen when they finish checking the set we submitted.
                                System.out.println("FAIL 01");
                                waitBeforeInterruptPlayer.wait();
                                
                            
                                System.out.println("FAIL 02");
                            } catch(InterruptedException ex){}
                        }   
                    }
                    
                }
                if(penalty){
                    for(int i=3; i>0; i++){
                        env.ui.setFreeze(id, i);
                        Thread.sleep(1000);//sec
                        
                    }
                    penalty = false;
                }
                else if(point){
                    for(int i=1; i>0; i++){
                        env.ui.setFreeze(id, i);
                        Thread.sleep(1000);//sec
                        
                    }
                    point = false;
                }
                env.ui.setFreeze(id, 0);
                keyContinue = true;
            }

            

        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
            while (!terminate) {
                // TODO implement player key press simulator

                
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement

        // change terminate to true
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        // if(player thread waiting do nothing)
         //   return;
        //so player won't be able to remove or add more slots after making a set.
        if(keyContinue){
            queue.offer(slot); 
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement

        env.ui.setScore(id, ++score);

        Long secs = env.config.pointFreezeMillis/1000;
        //display point freeze time
        env.ui.setFreeze(id, secs.intValue());

        //make player wait
        try{
            System.out.println("FAIL 09");
            Thread.sleep(env.config.pointFreezeMillis);
            System.out.println("FAIL 10");
        } catch(InterruptedException ignored2){}
        
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement

        Long secs = env.config.penaltyFreezeMillis/1000;
        //display penalty freeze time
        env.ui.setFreeze(id, secs.intValue());

        //make player wait
        try{
            System.out.println("FAIL 11");
            long time = System.currentTimeMillis();
            Thread.sleep(env.config.penaltyFreezeMillis);

            
            System.out.println("FAIL 12");
        } catch(InterruptedException ignored3){}
    }

    public int getScore() {
        return score;
    }
    public Thread getThread(){
        return playerThread;
    }

}
